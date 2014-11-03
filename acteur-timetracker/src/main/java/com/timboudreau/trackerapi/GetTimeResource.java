package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.headers.Method;
import com.mastfrog.acteur.mongo.CursorWriter;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import static com.timboudreau.trackerapi.Properties.*;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@BasicAuth
@PathRegex(Timetracker.URL_PATTERN_TIME)
@Methods({Method.GET, Method.HEAD})
@BannedUrlParameters(type)
@Description("Query recorded time events, specifying the user and sequence ID")
@Precursors({AuthorizedChecker.class, CreateCollectionPolicy.CreatePolicy.class, TimeCollectionFinder.class})
class GetTimeResource extends Acteur {

    @Inject
    public GetTimeResource(DBCollection collection, BasicDBObject query, HttpEvent evt, CursorWriter.Factory factory) {
        // Get the list of fieldds, if any, that the caller has restricted the
        // results to - no need to pull anything over from the database we don't
        // actually need
        String fields = evt.getParameter(Properties.fields);
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
        // Do the query and get the cursor;  add it to the closables to ensure
        DBCursor cur = projection == null ? collection.find(query) : collection.find(query, projection);
        if (!cur.hasNext()) {
            ok("[]\n");
        } else {
            // Tell the framework we're ready to stream the response
            ok();
            // Set the response writer to be a CursorWriter, which will write out
            // the results one row at a time
            if (evt.getMethod() != Method.HEAD && evt.getChannel().isOpen()) {
                // Create a ResponseWriter which will write and flush one row
                // at atime
                CursorWriter writer = factory.create(cur);
                setResponseWriter(writer);
            }
        }
    }
}
