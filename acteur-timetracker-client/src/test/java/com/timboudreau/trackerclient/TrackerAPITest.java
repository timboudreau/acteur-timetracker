package com.timboudreau.trackerclient;

import com.mastfrog.acteur.mongo.MongoHarness;
import com.mastfrog.acteur.mongo.MongoModule;
import com.mastfrog.acteur.util.ServerControl;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.netty.http.client.ResponseFuture;
import com.mastfrog.util.Streams;
import com.mastfrog.webapi.Callback;
import com.mastfrog.webapi.builtin.Parameters;
import com.mongodb.DB;
import com.timboudreau.trackerapi.Timetracker;
import com.timboudreau.trackerclient.impl.TrackerModule;
import com.timboudreau.trackerclient.pojos.Acknowledgement;
import com.timboudreau.trackerclient.pojos.Event;
import com.timboudreau.trackerclient.pojos.FieldID;
import com.timboudreau.trackerclient.pojos.SeriesID;
import com.timboudreau.trackerclient.pojos.Totals;
import com.timboudreau.trackerclient.pojos.User;
import com.timboudreau.trackerclient.pojos.UserID;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openide.util.Exceptions;

/**
 *
 * @author tim
 */
@RunWith(GuiceRunner.class)
@TestWith({MongoHarness.Module.class, MongoModule.class})
public class TrackerAPITest {

    Dependencies deps;
    TrackerClientSPI spi;
    TrackerSession sess;

    private ServerControl ctrl;
    private static int currPort = 57000;
    private MongoHarness harn;
    private void initServer(MongoHarness harn, DB db) throws IOException, InterruptedException {
        this.harn = harn;
        int mongoPort = harn.port();
        int port = currPort++;
        System.out.println("Start server on " + port + " with mongodb on ");
        System.out.flush();
        ServerControl ctrl = Timetracker.start("--mongoPort", mongoPort + "", "--port", "" + port);
        System.err.flush();
        deps = Dependencies.builder().add(
                new TrackerModule("http://localhost:" + port)).build();
        spi = deps.getInstance(TrackerClientSPI.class);
    }

    @After
    public void shutdown() throws InterruptedException {
        if (ctrl != null) {
            ctrl.shutdown(true);
        }
        if (deps != null) {
            deps.shutdown();
        }
        harn.stop();
        Thread.sleep(5000);
    }

    @Test
    public void testBasic(MongoHarness harn, DB db) throws Exception {
        initServer(harn, db);
        final SeriesID series = new SeriesID("stuff");

        String un = "jim" + Long.toString(System.currentTimeMillis(), 36);

        H<TrackerSession> hu = new H(TrackerSession.class);
        ResponseFuture fut = hu.future = spi.signup(un, "Moby Nightmare", "mobybaby", hu);
        fut.await();
        sess = hu.await();
        assertNotNull(sess);
//        Thread.sleep(3000);
//        assertEquals(CREATED, hu.status);

        final long now = System.currentTimeMillis();
        Duration length = Duration.standardSeconds(60);
        final long then = now + length.getMillis();

        final long before = now - 1;
        final long after = then + 1;

        H<SeriesID[]> addS = new H<>(SeriesID[].class);
        fut = addS.future = sess.getSeries(addS);
        fut.await();
        SeriesID[] allSeries = addS.await();
        assertNotNull(allSeries);
        assertEquals(0, allSeries.length);

        H<Event> addH = new H<>(Event.class);
        Event addedEvent;
        fut = addH.future = sess.addEvent(new Interval(now, then), addH,
                new SeriesID("stuff"), Parameters.create("foo", "bar"));
        addedEvent = addH.await();
        assertNotNull(addedEvent);

        Event[] queryResults;
        H<Event[]> evtH;

        assertEquals(now, addedEvent.getStart().getMillis());
        assertEquals(now + length.getMillis(), addedEvent.getEnd().getMillis());
        assertEquals("bar", addedEvent.metadata.get("foo"));

        addS = new H<>(SeriesID[].class);
        fut = addS.future = sess.getSeries(addS);
        allSeries = addS.await();
        assertEquals(1, allSeries.length);
        assertEquals(series, allSeries[0]);

        // These queries should all get our added event as the single result
        evtH = getEvents(series, EventQuery.create().startsBeforeOrAt(new DateTime(now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().startsAt(new DateTime(now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().startsAtOrAfter(new DateTime(now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().endsAt(new DateTime(then)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().endsAtOrAfter(new DateTime(then)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().endsBeforeOrAt(new DateTime(then)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().lasts(new Duration(then - now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().lastsAtLeast(new Duration(then - now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().lastsLessThan(new Duration((then + 1) - now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        evtH = getEvents(series, EventQuery.create().add("foo", "bar"), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());

        // Test empty results
        evtH = getEvents(series, EventQuery.create().add("foo", "baz"), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtH = getEvents(series, EventQuery.create().startsAt(new DateTime(now + 1)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtH = getEvents(series, EventQuery.create().startsAtOrAfter(new DateTime(now + 1)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtH = getEvents(series, EventQuery.create().endsAt(new DateTime(then - 1)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtH = getEvents(series, EventQuery.create().endsAtOrAfter(new DateTime(then + 1)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtH = getEvents(series, EventQuery.create().endsBeforeOrAt(new DateTime(then - 1)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtH = getEvents(series, EventQuery.create().lasts(new Duration(then - now - 1)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtH = getEvents(series, EventQuery.create().lastsAtLeast(new Duration((then + 1) - now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtH = getEvents(series, EventQuery.create().lastsLessThan(new Duration(then - (now + 1))), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        H<Totals> totalH = new H<>(Totals.class);
        fut = totalH.future = sess.getTotals(series, EventQuery.create().add("foo", "bar"), totalH);
        Totals tot = totalH.await();
        assertNotNull(tot);
        assertEquals(length, tot.total);
        assertNotNull(tot.intervals);
        assertNotNull(tot.period);
        assertEquals(length, tot.period.duration);
        assertEquals(1, tot.intervals.length);
        assertEquals(now, tot.period.interval.getStartMillis());
        assertEquals(then, tot.period.interval.getEndMillis());
        assertEquals(length, tot.intervals[0].duration);

        H<Acknowledgement> evtA = new H<>(Acknowledgement.class);
        fut = evtA.future = sess.updateTimes(series, new FieldID("food"), EventQuery.create().startsBeforeOrAt(new DateTime(now)), "pizza", evtA);
        Acknowledgement ack = evtA.await();
        assertNotNull(ack);
        assertEquals(1, ack.updated());

        evtH = getEvents(series, EventQuery.create().startsAt(new DateTime(now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());
        assertEquals("pizza", queryResults[0].getProperty("food"));

        evtA = new H<>(Acknowledgement.class);
        fut = evtA.future = sess.deleteField(series, new FieldID("food"), EventQuery.create().startsBeforeOrAt(new DateTime(now)), evtA);
        ack = evtA.await();
        assertNotNull(ack);
        assertEquals(1, ack.updated());

        evtH = getEvents(series, EventQuery.create().startsAt(new DateTime(now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(1, queryResults.length);
        assertEquals(now, queryResults[0].interval.getStartMillis());
        assertNull(queryResults[0].getProperty("food"));

        evtA = new H<>(Acknowledgement.class);
        fut = evtA.future = sess.delete(series, EventQuery.create().startsAt(new DateTime(now)), evtA);
        ack = evtA.await();
        assertNotNull(ack);
        assertEquals(1, ack.updated());

        evtH = getEvents(series, EventQuery.create().startsAt(new DateTime(now)), new H<>(Event[].class));
        queryResults = evtH.await();
        assertNotNull(queryResults);
        assertEquals(0, queryResults.length);

        evtA = new H<>(Acknowledgement.class);
        fut = evtA.future = sess.setPassword("monkey", evtA);
        ack = evtA.await();
        assertNotNull(ack);
        assertEquals(1, ack.updated());

        H<TrackerSession> huser = new H<>(TrackerSession.class);
        fut = huser.future = spi.signIn(un, "monkey", huser);
        TrackerSession sess2 = huser.await();
        assertNotNull(sess2);

        User user = sess2.getUser();
        assertNotNull(user);
        assertEquals(un, user.getName());
        testRecording();
    }

    H<Event[]> getEvents(SeriesID series, EventQuery query, H<Event[]> h) throws Exception {
        h.future = sess.getEvents(series, query, h);
        return h;
    }

    private  void testRecording() throws IOException, Exception {
        H<TrackerSession> hu = new H<>(TrackerSession.class);
        SeriesID series = new SeriesID("live");
        UserID userName = new UserID("wunky" + Long.toString(System.currentTimeMillis(), 36));
        ResponseFuture fut = hu.future = spi.signup(userName.toString(), "Funky Wunky", "password", hu);
        hu.await();
        sess = hu.obj;
        assertNotNull(sess);

        LSL liveListener = new LSL();
        fut = sess.liveSession(Parameters.create("foo", "bar").add("baz", "quux"), series, liveListener);
        LiveSession ls = liveListener.await();
        assertNotNull(ls);

        System.out.println("LS: " + ls);

        final long WAIT_FOR = 2000;

        DateTime start = liveListener.obj.getRemoteStartTime();
        System.err.println("Remote start time" + start);
        Thread.sleep(WAIT_FOR);
        ls.end();
        liveListener.waitForCancel();
        Thread.sleep(450);
        assertTrue(liveListener.cancelled);
        
        // XXX fixme - some intermittent timing-based failures here

//        H<Event[]> evtH = getEvents(series, EventQuery.create().startsAt(start), new H<>(Event[].class));
//        Event[] queryResults = evtH.await();
//        Thread.sleep(250);
//        assertNotNull(queryResults);
//        assertEquals(1, queryResults.length);
//
//        Event e = queryResults[0];
//        assertEquals("bar", e.getProperty("foo"));
//        assertEquals("quux", e.getProperty("baz"));
//        assertFalse(e.running);
//        assertTrue(e.duration.getMillis() > WAIT_FOR - 500);
    }

    static class H<T> extends Callback<T> {

        T obj;
        volatile boolean error;
        volatile int count = 0;

        ResponseFuture future;

        HttpHeaders resp;
        HttpResponseStatus status;

        Exception prevCall;
        Exception construction = new Exception();
        private final CountDownLatch latch = new CountDownLatch(1);

        H(Class<T> type) {
            super(type);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("H for " + type().getName() + " err" + error + "\n");
            return sb.toString();
        }

        @Override
        public void fail(HttpResponseStatus status, ByteBuf bytes) {
            System.out.println("FAIL " + status);
            status = status;
            error = true;
            if (bytes != null && bytes.isReadable() && bytes.readableBytes() > 0) {
                try {
                    System.err.println(Streams.readString(new ByteBufInputStream(bytes)));
                } catch (IOException | IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            latch.countDown();
        }

        public T await() throws InterruptedException {
            future.await(10, TimeUnit.SECONDS);
            latch.await(10, TimeUnit.SECONDS);
            return obj;
        }

        @Override
        public void success(T object) {
            obj = object;
            count++;
            if (count > 1) {
                System.err.println("CONSTRUCTED AT " + construction.getStackTrace()[1]);
                System.err.println("ALREADY CALLED AT");
                prevCall.printStackTrace();
                throw new Error("CALLED MORE THAN ONCE");
            }
            prevCall = new Exception();
            latch.countDown();
        }

        Throwable err;

        @Override
        public void error(Throwable err) {
            this.err = err;
            err.printStackTrace();
        }

        @Override
        public void responseReceived(HttpResponseStatus status, HttpHeaders headers) {
            System.out.println("RESPONSE REC " + status);
            resp = headers;
            status = status;
        }
    }

    static class LSL extends LiveSessionListener {

        LiveSession obj;
        private final CountDownLatch latch = new CountDownLatch(1);
        volatile boolean error;
        int count = 0;
        private volatile boolean cancelled;

        @Override
        public void onError(Throwable thrown) {
            thrown.printStackTrace();
            error = true;
        }
        
        void waitForCancel() {
            if (cancelled) {
                return;
            }
            synchronized(this) {
                try {
                    wait(1000);
                } catch (InterruptedException ex) {
                }
            }
        }

        @Override
        public void onClose() {
            cancelled = true;
            synchronized(this) {
                notifyAll();
            }
        }

        @Override
        public void onRequestCompleted() {
            count++;
            if (count > 1) {
                throw new Error("CALLED MORE THAN ONCE");
            }
        }

        @Override
        public void set(LiveSession session) {
            System.out.println("LiveSession set: " + session);
            this.obj = session;
            latch.countDown();
        }

        public LiveSession await() throws InterruptedException {
            latch.await(3, TimeUnit.SECONDS);
            return obj;
        }
    }
}
