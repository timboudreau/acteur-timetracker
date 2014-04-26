package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.util.Providers;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.Closables;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.headers.Method;
import com.mastfrog.acteur.mongo.CursorWriter;
import com.mastfrog.acteur.mongo.CursorWriter.MapFilter;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import static com.timboudreau.trackerapi.Properties.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@Methods({Method.HEAD, Method.GET})
@PathRegex("^all$")
@Description("List all users")
class ListUsersResource extends Acteur {

    @Inject
    ListUsersResource(@Named("ttusers") DBCollection coll, HttpEvent evt, Closables clos) {
        DBCursor cursor = coll.find();
        setState(new RespondWith(200));
        if (evt.getMethod() == Method.GET) {
            setResponseWriter(new CursorWriter(cursor, clos, evt, Providers.<MapFilter>of(new MF())));
        }
    }

    private static class MF implements MapFilter {

        @Override
        public Map<String, Object> filter(Map<String, Object> m) {
            m.remove(pass);
            m.remove(origPass);
            return m;
        }
    }
}
