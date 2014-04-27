package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.errors.Err;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.MaximumPathLength;
import com.mastfrog.acteur.preconditions.MaximumRequestBodyLength;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.MinimumRequestBodyLength;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequiredUrlParameters;
import com.mastfrog.acteur.util.PasswordHasher;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import static com.timboudreau.trackerapi.Properties.*;
import static com.timboudreau.trackerapi.SignUpResource.MAX_USERNAME_LENGTH;
import static com.timboudreau.trackerapi.SignUpResource.MIN_USERNAME_LENGTH;
import static com.timboudreau.trackerapi.Timetracker.USER_COLLECTION;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@PathRegex("^users/([^/]{" + MIN_USERNAME_LENGTH + "," + MAX_USERNAME_LENGTH + "}?)/signup$")
@MaximumPathLength(3)
@Methods({PUT, POST})
@RequiredUrlParameters("displayName")
@Description("New user signup")
@MinimumRequestBodyLength(SignUpResource.MIN_PASSWORD_LENGTH)
@MaximumRequestBodyLength(SignUpResource.MAX_PASSWORD_LENGTH)
class SignUpResource extends Acteur {

    public static final int MAX_PASSWORD_LENGTH = 40;
    public static final int MIN_PASSWORD_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 40;
    public static final int MIN_USERNAME_LENGTH = 3;

    @Inject
    SignUpResource(@Named(USER_COLLECTION) DBCollection coll, HttpEvent evt, PasswordHasher crypto, ObjectMapper mapper) throws UnsupportedEncodingException, IOException {
        String userName = evt.getPath().getElement(1).toString();
        DBObject existing = coll.findOne(new BasicDBObject(name, userName), new BasicDBObject("_id", true));
        if (existing != null) {
            setState(new RespondWith(Err.conflict("A user named " + userName + " exists")));
            return;
        }
        DBObject nue = new BasicDBObject(name, new String[]{userName})
                .append(displayName, evt.getParameter(displayName))
                .append(created, DateTime.now().getMillis())
                .append(version, 0).append(authorizes, new ObjectId[0]);
        String password = evt.getContentAsJSON(String.class);
        String encrypted = crypto.encryptPassword(password);
        nue.put(pass, encrypted);
        nue.put(origPass, encrypted);
        coll.save(nue, WriteConcern.FSYNCED);
        setState(new RespondWith(HttpResponseStatus.CREATED, nue.toMap()));
    }
}
