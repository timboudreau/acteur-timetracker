package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import static com.mastfrog.acteur.headers.Method.DELETE;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.timboudreau.trackerapi.support.Auth;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import java.io.IOException;
import static com.timboudreau.trackerapi.Properties.*;
import com.timboudreau.trackerapi.support.AuthorizedChecker;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@PathRegex(Timetracker.URL_PATTERN_TIME)
@Methods(DELETE)
@BannedUrlParameters("type")
@Precursors({Auth.class, AuthorizedChecker.class, CreateCollectionPolicy.DontCreatePolicy.class, TimeCollectionFinder.class})
@Description("Delete records matching this query")
class DeleteTimeResource extends Acteur {

    @Inject
    public DeleteTimeResource(HttpEvent evt, DBCollection collection, BasicDBObject query) throws IOException {
        query.put(type, time);
        query.remove(detail);

        WriteResult res = collection.remove(query, WriteConcern.ACKNOWLEDGED);
        setState(new RespondWith(200, Timetracker.quickJson("updated", res.getN())));
    }
}
