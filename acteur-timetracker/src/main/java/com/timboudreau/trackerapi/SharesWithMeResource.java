package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.Concluders;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.mongo.CursorWriterActeur;
import com.mastfrog.acteur.headers.Headers;
import static com.mastfrog.acteur.headers.Method.GET;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import static com.timboudreau.trackerapi.Properties.authorizes;
import static com.timboudreau.trackerapi.Properties.displayName;
import static com.timboudreau.trackerapi.Properties.name;
import com.timboudreau.trackerapi.support.TTUser;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@PathRegex("^users/.*?/sharers/?$")
@Methods(GET)
@BasicAuth
@Description("Authenticate login and fetch user name")
@Concluders(CursorWriterActeur.class)
public class SharesWithMeResource extends Acteur {

    @Inject
    SharesWithMeResource(HttpEvent evt, TTUser user, @Named(Timetracker.USER_COLLECTION) DBCollection coll, ObjectMapper mapper) throws IOException {
        add(Headers.stringHeader("UserID"), user.id().toString());
        BasicDBObject projection = new BasicDBObject("_id", 1).append(name, 1).append(displayName, 1);
        DBCursor cursor = coll.find(new BasicDBObject(authorizes, user.id()), projection);
        if (cursor == null) {
            setState(new RespondWith(HttpResponseStatus.GONE, "No record of " + user.name()));
            return;
        }
        if (!cursor.hasNext()) {
            setState(new RespondWith(200, "[]\n"));
            cursor.close();
        } else {
            setState(new ConsumedLockedState(cursor));
        }
    }
}
