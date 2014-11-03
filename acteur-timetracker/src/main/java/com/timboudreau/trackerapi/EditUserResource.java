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
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequireAtLeastOneUrlParameterFrom;
import com.mastfrog.acteur.util.PasswordHasher;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import static com.timboudreau.trackerapi.Properties.displayName;
import static com.timboudreau.trackerapi.Properties.lastModified;
import static com.timboudreau.trackerapi.Properties.name;
import static com.timboudreau.trackerapi.SignUpResource.MAX_USERNAME_LENGTH;
import static com.timboudreau.trackerapi.SignUpResource.MIN_USERNAME_LENGTH;
import static com.timboudreau.trackerapi.Timetracker.USER_COLLECTION;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.TTUser;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.net.URLDecoder;
import org.joda.time.DateTimeUtils;

/**
 * Change a user's display name
 *
 * @author Tim Boudreau
 */
@HttpCall
@BasicAuth
@PathRegex("^users/([^/]*?)/?$")
@Methods({PUT, POST})
@BannedUrlParameters({Properties._id, Properties.name, Properties.pass, Properties.origPass, Properties.authorizes})
@RequireAtLeastOneUrlParameterFrom(displayName)
@Precursors(AuthorizedChecker.class)
@Description("Edit a user's properties")
public class EditUserResource extends Acteur {

    @Inject
    EditUserResource(@Named(USER_COLLECTION) DBCollection coll, HttpEvent evt, PasswordHasher hasher, TTUser user/*, OAuthPlugins pgns*/) throws IOException {
        String userName = URLDecoder.decode(evt.getPath().getElement(1).toString(), "UTF-8");
        String dn = evt.getParameter(Properties.displayName);

        if (!userName.equals(user.name())) {
            setState(new RespondWith(Err.forbidden(user.name()
                    + " cannot set the password for " + userName)));
            return;
        }
        if (dn.length() < MIN_USERNAME_LENGTH) {
            setState(new RespondWith(Err.badRequest("Display name '" + dn
                    + "' too short - min is "
                    + MIN_USERNAME_LENGTH)));
            return;
        }
        if (dn.length() > MAX_USERNAME_LENGTH) {
            setState(new RespondWith(Err.badRequest("Display name too long - max is "
                    + MAX_USERNAME_LENGTH)));
            return;
        }

        DBObject query = coll.findOne(new BasicDBObject(name, userName));

        DBObject update = new BasicDBObject("$set", new BasicDBObject(displayName, dn)
                .append(lastModified, DateTimeUtils.currentTimeMillis())).append("$inc",
                        new BasicDBObject(Properties.version, 1));

        WriteResult res = coll.update(query, update, false, false, WriteConcern.FSYNCED);
//        if (res.getN() == 1) {
//            pgns.createDisplayNameCookie(evt, response(), dn);
//        }

        setState(new RespondWith(HttpResponseStatus.ACCEPTED, Timetracker.quickJson("updated", res.getN())));
    }
}
