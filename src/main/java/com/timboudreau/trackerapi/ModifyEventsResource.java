package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.Acteur.RespondWith;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Page;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.headers.Method;
import static com.mastfrog.acteur.headers.Method.DELETE;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.InjectRequestBodyAs;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.timboudreau.trackerapi.ModifyEventsResource.Body;
import static com.timboudreau.trackerapi.ModifyEventsResource.PAT;
import com.timboudreau.trackerapi.support.Auth;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.Map;
import static com.timboudreau.trackerapi.Properties.*;
import java.util.List;

/**
 *
 * @author tim
 */
@HttpCall
@Methods({PUT, POST, DELETE})
@PathRegex(PAT)
@BannedUrlParameters(type)
@Precursors({Auth.class, CreateCollectionPolicy.DontCreatePolicy.class, TimeCollectionFinder.class})
public final class ModifyEventsResource extends Page {

    public static final String PAT = "^users/(.*?)/update/(.*?)/(.*?)$";

    @Inject
    public ModifyEventsResource(HttpEvent evt, ActeurFactory af) {
        add(Auth.class);
        add(CreateCollectionPolicy.DONT_CREATE.toActeur());
        add(TimeCollectionFinder.class);
        add(Modifier.class);
    }

    @Override
    protected String getDescription() {
        return "Modify or add data to an event or set of events";
    }

    private static class Modifier extends Acteur {

        @Inject
        public Modifier(HttpEvent evt, DBCollection collection, BasicDBObject query) throws IOException {
            try {
                query.put(type, time);
                Body something = null;
                boolean isDelete = evt.getMethod() == DELETE;
                if (!isDelete) {
                    something = evt.getContentAsJSON(Body.class);
                } else {
                    something = new Body(1);
                }
                System.out.println("GOT MODIFY REQUEST " + query.toMap());
                DBObject modification = new BasicDBObject(isDelete ? "$unset"
                        : "$set", new BasicDBObject(evt.getPath().getLastElement().toString(),
                                something.object)).append("$inc", new BasicDBObject("version", 1));
                WriteResult res = collection.update(query, modification, false, true, WriteConcern.ACKNOWLEDGED);
                String resultJson = Timetracker.quickJson("updated", res.getN());
                setState(new RespondWith(res.getN() > 0 ? HttpResponseStatus.ACCEPTED
                        : HttpResponseStatus.GONE, resultJson));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class Body {

        // This class exists because Jackson will not instantiate String or number directly,
        // but will instantiate an object that takes a String or number as
        // an argument
        private final Object object;

        Body(int number) {
            this.object = number;
        }

        Body(String value) {
            this.object = value;
        }

        Body(Map map) {
            this.object = map;
        }

        Body(List list) {
            this.object = list;
        }

        public Object get() {
            return object;
        }

        @Override
        public String toString() {
            return object.toString();
        }
    }
}
