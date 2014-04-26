package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Page;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.util.PasswordHasher;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.timboudreau.trackerapi.support.Auth;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.TTUser;
import com.timboudreau.trackerapi.support.UserCollectionFinder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@PathRegex("^users/(.*?)/password$")
@Methods({PUT, POST})
@Precursors({Auth.class, AuthorizedChecker.class})
public class SetPasswordResource extends Acteur {

    @Inject
    SetPasswordResource(@Named("ttusers") DBCollection coll, HttpEvent evt, PasswordHasher hasher, TTUser user) throws IOException {
        String userName = evt.getPath().getElement(1).toString();
        String pw = evt.getContent().toString(Charset.forName("UTF-8"));
        if (pw.length() < SignUpResource.MIN_PASSWORD_LENGTH) {
            setState(new RespondWith(400, "Password too short"));
            return;
        }
        if (pw.length() >= SignUpResource.MAX_PASSWORD_LENGTH) {
            setState(new RespondWith(400, "Password too long"));
            return;
        }
        if (!userName.equals(user.name)) {
            setState(new RespondWith(HttpResponseStatus.FORBIDDEN, user.name
                    + " cannot set the password for " + userName));
            return;
        }

        System.out.println("Set password for " + userName + " to " + pw);

        String hashed = hasher.encryptPassword(pw.toString());

        DBObject query = coll.findOne(new BasicDBObject("name", userName));

        DBObject update = new BasicDBObject("$set", new BasicDBObject("pass", hashed)).append("$inc",
                new BasicDBObject("version", 1));

        WriteResult res = coll.update(query, update, false, false, WriteConcern.FSYNCED);

        setState(new RespondWith(200, Timetracker.quickJson("updated", res.getN())));
    }
}
