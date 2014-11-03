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
 * Authorizes another user to see our data
 *
 * @author Tim Boudreau
 */
@HttpCall
@BasicAuth
@PathRegex("^users/.*?/authorize/.*?")
@Methods(PUT)
@Precursors({AuthorizedChecker.class})
@Description("Authorize another user to access my data")
public class AuthorizeOtherUserResource extends Acteur {

    // The other user is looked up by UserFromURL and injected - see bindings
    // in TimeTrackerModule
    @Inject
    AuthorizeOtherUserResource(TTUser user, HttpEvent evt, @Named(USER_COLLECTION) DBCollection coll, @Named(Timetracker.OTHER_USER) Provider<TTUser> otherUser) {
        TTUser other = otherUser.get();
        // Should not happen
        if (other == null) {
            setState(new RespondWith(Err.gone("No such user " + evt.getPath().getLastElement())));
            return;
        }
        // Create a query
        BasicDBObject query = new BasicDBObject(Properties._id, user.id());
        // And an update
        BasicDBObject update = new BasicDBObject("$addToSet", new BasicDBObject(Properties.authorizes,
                other.id()));
        WriteResult res = coll.update(query, update, false, false, WriteConcern.FSYNCED);
        setState(new RespondWith(HttpResponseStatus.ACCEPTED, Timetracker.quickJson("updated", res.getN())));
    }
}
