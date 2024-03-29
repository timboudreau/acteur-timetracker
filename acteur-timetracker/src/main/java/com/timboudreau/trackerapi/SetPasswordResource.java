package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.errors.Err;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.mongo.util.UpdateBuilder;
import com.mastfrog.acteur.preconditions.Authenticated;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.InjectRequestBodyAs;
import com.mastfrog.acteur.preconditions.MaximumRequestBodyLength;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.MinimumRequestBodyLength;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.util.PasswordHasher;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import static com.timboudreau.trackerapi.Properties.name;
import static com.timboudreau.trackerapi.Properties.pass;
import static com.timboudreau.trackerapi.Timetracker.USER_COLLECTION;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.TTUser;
import java.io.IOException;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@Authenticated
@PathRegex("^users/(.*?)/password$")
@Methods({PUT, POST})
@MinimumRequestBodyLength(SignUpResource.MIN_PASSWORD_LENGTH)
@MaximumRequestBodyLength(SignUpResource.MAX_PASSWORD_LENGTH)
@Precursors({AuthorizedChecker.class})
@InjectRequestBodyAs(String.class)
@Description("Set a user's password")
public class SetPasswordResource extends Acteur {

    @Inject
    SetPasswordResource(@Named(USER_COLLECTION) DBCollection coll, HttpEvent evt, PasswordHasher hasher, TTUser user, String pw) throws IOException {
        String userName = evt.path().getElement(1).toString();
        if (!userName.equals(user.name())) {
            setState(new RespondWith(Err.forbidden(user.name()
                    + " cannot set the password for " + userName)));
            return;
        }
        String hashed = hasher.encryptPassword(pw);
        DBObject query = coll.findOne(new BasicDBObject(name, userName));

        DBObject update = UpdateBuilder.$().increment(Properties.version).set(pass, hashed).build();

        WriteResult res = coll.update(query, update, false, false, WriteConcern.FSYNCED);
        ok(Timetracker.quickJson("updated", res.getN()));
    }
}
