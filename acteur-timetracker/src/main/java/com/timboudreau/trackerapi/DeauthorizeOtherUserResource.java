package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.errors.Err;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.Authenticated;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import static com.timboudreau.trackerapi.Timetracker.USER_COLLECTION;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.TTUser;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Remove another user's authoization to see our data
 *
 * @author Tim Boudreau
 */
@HttpCall
@Authenticated
@PathRegex("^users/.*?/deauthorize/.*?")
@Methods(PUT)
@Precursors({AuthorizedChecker.class})
@Description("Remove another user's authorization to use my data")
public class DeauthorizeOtherUserResource extends Acteur {

    @Inject
    DeauthorizeOtherUserResource(TTUser user, HttpEvent evt, @Named(USER_COLLECTION) DBCollection coll, @Named(Timetracker.OTHER_USER) Provider<TTUser> otherUser) {
        TTUser other = otherUser.get();
        if (other == null) {
            setState(new RespondWith(Err.gone("No such user " + evt.getPath().getLastElement())));
            return;
        }
        BasicDBObject query = new BasicDBObject(Properties._id, user.id());
        BasicDBObject update = new BasicDBObject("$pull", new BasicDBObject(Properties.authorizes, other.id()));
        WriteResult res = coll.update(query, update, false, false, WriteConcern.FSYNCED);
        setState(new RespondWith(HttpResponseStatus.ACCEPTED, Timetracker.quickJson("updated", res.getN())));
    }
}
