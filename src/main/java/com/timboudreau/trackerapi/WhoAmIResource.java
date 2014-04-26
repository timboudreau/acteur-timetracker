package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.headers.Headers;
import static com.mastfrog.acteur.headers.Method.GET;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.timboudreau.trackerapi.support.Auth;
import com.timboudreau.trackerapi.support.TTUser;
import com.timboudreau.trackerapi.support.UserCollectionFinder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@Description("Authenticate login and fetch user name")
@Methods(GET)
@PathRegex("^whoami/?$")
@Precursors({UserCollectionFinder.class, Auth.class})
public class WhoAmIResource extends Acteur {

    @Inject
    WhoAmIResource(TTUser user, DBCollection coll, ObjectMapper mapper) throws IOException {
        add(Headers.stringHeader("UserID"), user.id.toStringMongod());
        DBObject ob = coll.findOne(new BasicDBObject("_id", user.id));
        if (ob == null) {
            setState(new RespondWith(HttpResponseStatus.GONE, "No record of " + user.name));
            return;
        }
        Map<String, Object> m = new HashMap<>(ob.toMap());
        m.remove(Properties.pass);
        m.remove(Properties.origPass);
        setState(new RespondWith(200, mapper.writeValueAsString(m)));
    }
}
