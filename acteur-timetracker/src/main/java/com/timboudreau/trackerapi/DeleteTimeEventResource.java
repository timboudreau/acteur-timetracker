package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.errors.Err;
import static com.mastfrog.acteur.headers.Method.DELETE;
import com.mastfrog.acteur.preconditions.Authenticated;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import java.io.IOException;
import static com.timboudreau.trackerapi.Properties.*;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import org.bson.types.ObjectId;

/**
 * Delete time events in the specified series matching the query
 *
 * @author Tim Boudreau
 */
@HttpCall
@Authenticated
@PathRegex(Timetracker.URL_PATTERN_TIME)
@Methods(DELETE)
@BannedUrlParameters("type")
@Precursors({AuthorizedChecker.class, CreateCollectionPolicy.DontCreatePolicy.class, TimeCollectionFinder.class})
@Description("Delete records matching this query")
class DeleteTimeEventResource extends Acteur {

    @Inject
    public DeleteTimeEventResource(HttpEvent evt, DBCollection collection, BasicDBObject query) throws IOException {
        if (query.isEmpty()) {
            setState(new RespondWith(Err.badRequest("Empty query matches all events in series - will not do that")));
            return;
        }
        String id = evt.getParameter(Properties._id);
        if (id instanceof String) {
            try {
                ObjectId oid = new ObjectId(id.toString());
                query.put(Properties._id, oid);
            } catch (IllegalArgumentException e) {
                setState(new RespondWith(Err.badRequest("Not a valid event id: " + id)));
            }
        }
        query.put(type, time);
        query.remove(detail);
        WriteResult res = collection.remove(query, WriteConcern.ACKNOWLEDGED);
        ok(Timetracker.quickJson("updated", res.getN()));
    }
}
