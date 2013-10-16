package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Page;
import com.mastfrog.acteur.util.Method;
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
class DeleteTimeResource extends Page {

    @Inject
    public DeleteTimeResource(ActeurFactory af) {
        add(af.matchPath(Timetracker.URL_PATTERN_TIME));
        add(af.matchMethods(Method.DELETE));
        add(af.banParameters("type"));
        add(Auth.class);
        add(AuthorizedChecker.class);
        add(CreateCollectionPolicy.DONT_CREATE.toActeur());
        add(TimeCollectionFinder.class);
        add(TotalGetter.class);
    }

    @Override
    protected String getDescription() {
        return "Delete records matching this query";
    }

    private static class TotalGetter extends Acteur {

        @Inject
        public TotalGetter(HttpEvent evt, DBCollection collection, BasicDBObject query) throws IOException {
            query.put(type, time);
            query.remove(detail);

            WriteResult res = collection.remove(query, WriteConcern.ACKNOWLEDGED);
            setState(new RespondWith(200, Timetracker.quickJson("updated", res.getN())));
        }
    }
}
