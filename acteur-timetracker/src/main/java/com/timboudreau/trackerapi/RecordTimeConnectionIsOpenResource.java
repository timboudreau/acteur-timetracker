package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.Application;
import com.mastfrog.acteur.Closables;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.errors.Err;
import com.mastfrog.acteur.headers.HeaderValueType;
import com.mastfrog.acteur.headers.Headers;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.Authenticated;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.util.time.TimeUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.LiveWriter;
import com.timboudreau.trackerapi.support.TTUser;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bson.types.ObjectId;
import static com.timboudreau.trackerapi.Properties.*;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import io.netty.util.AsciiString;
import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@Authenticated
@PathRegex("^users/(.*?)/sessions/(.*?)")
@Methods(PUT)
@Precursors({AuthorizedChecker.class, CreateCollectionPolicy.CreatePolicy.class, TimeCollectionFinder.class})
@Description("Record an ongoing time event which lasts as long as the connection to this"
        + " URL is held open")
final class RecordTimeConnectionIsOpenResource extends Acteur implements ChannelFutureListener {

    private final BasicDBObject toWrite = new BasicDBObject(type, time);
    private final long created = TimeUtil.toUnixTimestamp(ZonedDateTime.now());
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    public static final HeaderValueType<CharSequence> RS
            = Headers.header(AsciiString.of("X-Remote_Start"));
    public static final HeaderValueType<CharSequence> XTI
            = Headers.header(AsciiString.of("X-Tracker-ID"));
    public static final HeaderValueType<CharSequence> XLI
            = Headers.header(AsciiString.of("X-Local-ID"));
    

    @Inject
    RecordTimeConnectionIsOpenResource(@Named("periodicLiveWrites") final boolean pings, final HttpEvent evt, final Provider<DBCollection> coll, TTUser user, Application application, final Provider<LiveWriter> writer, Closables clos) {
        toWrite.append(by, user.idAsString())
                .append(start, created)
                .append(end, created)
                .append(running, true)
                .append(added, created)
                .append(version, 0);
        String err = buildQueryFromURLParameters(evt, toWrite);
        if (err != null) {
            setState(new RespondWith(Err.badRequest(err)));
            return;
        }
        add(Headers.CONTENT_LENGTH, 3600000L);
        add(Headers.header("X-Remote-Start"), created + "");
        add(Headers.X_ACCEL_BUFFERING, false);
        add(Headers.DATE, TimeUtil.fromUnixTimestamp(created));
        setChunked(false);
        setState(new RespondWith(HttpResponseStatus.ACCEPTED));
        setResponseBodyWriter(this);
        coll.get().insert(toWrite, WriteConcern.FSYNC_SAFE);
        ObjectId id = (ObjectId) toWrite.get(_id);
        add(XTI, id.toStringMongod());
        if (evt.urlParameter(Properties.localId) != null) {
            add(XLI, evt.urlParameter(localId));
        }

        final AtomicBoolean done = new AtomicBoolean();
        final Callable<?> c = application.getRequestScope()
                .wrap(new PeriodicDurationUpdater(toWrite, coll, done,
                                isRunning, created));

        if (pings) {
            writer.get().add(c);
        }
        clos.add(new AutoCloseable() {

            @Override
            public void close() throws Exception {
                isRunning.set(false);
                try {
                    c.call();
                } finally {
                    done.set(true);
                    if (pings) {
                        writer.get().remove(c);
                    }
                }
            }
        });
    }

    static boolean undot(String key, Object val, BasicDBObject ob) {
        if (key.length() == 0) {
            return false;
        }
        if (key.indexOf('.') < 0) {
            ob.put(key, val);
            return true;
        } else {
            int ix = key.indexOf('.');
            String rest = key.substring(ix + 1);
            String first = key.substring(0, ix);
            if (first.length() == 0 || first.charAt(0) == '$' || first.charAt(0) == '$') {
                return false;
            }
            BasicDBObject sub = new BasicDBObject();
            ob.put(first, sub);
            return undot(rest, val, sub);
        }
    }

    static String buildQueryFromURLParameters(final HttpEvent evt, BasicDBObject toWrite, String... ignore) {
        Arrays.sort(ignore);
        if (evt.urlParametersAsMap().size() > AddTimeResource.MAX_PROPERTIES) {
            return "Too many URL parameters - max is "
                    + AddTimeResource.MAX_PROPERTIES;
        }
        for (Map.Entry<String, String> e : evt.urlParametersAsMap().entrySet()) {
            switch (e.getKey()) {
                case start:
                case end:
                case version:
                case running:
                case added:
                case duration:
                case by:
                    if (Arrays.binarySearch(ignore, e.getKey()) < 0) {
                        return "Illegal query parameter '" + e.getKey() + "'";
                    }
                    break;
                default:
                    String v = e.getValue();
                    if (v.charAt(0) == '.' || v.charAt(v.length() - 1) == '.' || v.charAt(0) == '$') {
                        return "Parameter name may not start or end with . or start with $";
                    }
                    if (v.indexOf(',') > 0) {
                        String[] spl = v.split(",");
                        List<String> l = new LinkedList<>();
                        for (String s : spl) {
                            l.add(s.trim());
                        }
                        if (!undot(e.getKey(), l, toWrite)) {
                            return "Empty sub-property name or presence of . or $ in " + e.getKey();
                        }
                    } else if (!undot(e.getKey(), e.getValue(), toWrite)) {
                        return "Empty sub-property name or presence of . or $ in " + e.getKey();
                    }
            }
        }
        return null;
    }

    static class PeriodicDurationUpdater implements Callable<Void> {

        private final BasicDBObject toWrite;
        private final Provider<DBCollection> coll;
        private final AtomicBoolean done;
        private final AtomicBoolean running;
        private final long start;

        public PeriodicDurationUpdater(BasicDBObject toWrite, Provider<DBCollection> coll, AtomicBoolean done, AtomicBoolean running, long start) {
            this.toWrite = toWrite;
            this.coll = coll;
            this.done = done;
            this.running = running;
            this.start = start;
        }

        public Void call() throws Exception {
            if (!done.get()) {
                long end = TimeUtil.toUnixTimestamp(ZonedDateTime.now());
                toWrite.append("end", end).append(Properties.duration, end - start).append(Properties.running, running.get());
                coll.get().save(toWrite, WriteConcern.UNACKNOWLEDGED);
            }
            return null;
        }
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        future.channel().writeAndFlush(Unpooled.wrappedBuffer(("Started at " + toWrite.get(Properties.start)).getBytes()));
        // don't close!
    }
}
