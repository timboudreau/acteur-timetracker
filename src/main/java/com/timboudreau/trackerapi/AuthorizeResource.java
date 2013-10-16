package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Page;
import com.mastfrog.acteur.util.Method;
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
import org.bson.types.ObjectId;

/**
 *
 * @author Tim Boudreau
 */
public class AuthorizeResource extends Page {

    @Inject
    AuthorizeResource(ActeurFactory af) {
        add(af.matchPath("^users/.*?/authorize/.*?"));
        add(af.matchMethods(Method.PUT));
        add(Auth.class);
        add(AuthorizedChecker.class);
        add(UserCollectionFinder.class);
        add(Authorizer.class);
    }

    @Override
    protected String getDescription() {
        return "Authorize another user to access my data";
    }

    private static final class Authorizer extends Acteur {

        @Inject
        Authorizer(TTUser user, HttpEvent evt, DBCollection coll) {
            String otherUserNameOrID = evt.getPath().getElement(3).toString();
            BasicDBObject findOtherUserQuery = new BasicDBObject("name", otherUserNameOrID);
            DBObject otherUser = coll.findOne(findOtherUserQuery);
            if (otherUser == null) {
                findOtherUserQuery = new BasicDBObject("_id", new ObjectId(otherUserNameOrID));
                otherUser = coll.findOne(findOtherUserQuery);
            }
            if (otherUser == null) {
                setState(new RespondWith(HttpResponseStatus.GONE, "No such user " + otherUserNameOrID));
                return;
            }
            BasicDBObject query = new BasicDBObject("_id", user.id);
            BasicDBObject update = new BasicDBObject("$addToSet", new BasicDBObject(Properties.authorizes, otherUser.get("_id")));
            WriteResult res = coll.update (query, update, false, false, WriteConcern.FSYNCED);
            setState (new RespondWith(HttpResponseStatus.ACCEPTED, Timetracker.quickJson("updated", res.getN())));
        }
    }
}
