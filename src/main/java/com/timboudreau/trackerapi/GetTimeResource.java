package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.Closables;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.headers.Method;
import com.mastfrog.acteur.mongo.CursorWriter;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.timboudreau.trackerapi.support.Auth;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.timboudreau.trackerapi.Properties.*;
import com.timboudreau.trackerapi.support.AuthorizedChecker;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@PathRegex(Timetracker.URL_PATTERN_TIME)
@Methods({Method.GET, Method.HEAD})
@BannedUrlParameters(type)
@Description("Query recorded time events")
@Precursors({Auth.class, AuthorizedChecker.class, CreateCollectionPolicy.CreatePolicy.class, TimeCollectionFinder.class})
class GetTimeResource extends Acteur {

    @Inject
    public GetTimeResource(DBCollection collection, BasicDBObject query, HttpEvent evt, ObjectMapper mapper, Closables clos) {
        query.put(type, time);
        String fields = evt.getParameter("fields");
        DBObject projection = null;
        if (fields != null) {
            projection = new BasicDBObject();
            for (String field : fields.split(",")) {
                field = field.trim();
                if (!field.isEmpty()) {
                    projection.put(field, 1);
                }
            }
        }
        DBCursor cur = projection == null ? collection.find(query) : collection.find(query, projection);
        if (!cur.hasNext()) {
            ok("[]\n");
        } else {
            ok();
            if (evt.getMethod() != Method.HEAD && evt.getChannel().isOpen()) {
                setResponseWriter(new CursorWriter(cur, evt, clos));
            }
        }
    }
}
