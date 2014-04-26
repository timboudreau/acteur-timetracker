package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import static com.timboudreau.trackerapi.Properties.name;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.TTUser;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.bson.types.ObjectId;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@BasicAuth
@PathRegex("^users/.*?/authorize/.*?")
@Methods(PUT)
@Precursors({AuthorizedChecker.class})
@Description("Authorize another user to access my data")
public class AuthorizeResource extends Acteur {

    @Inject
    AuthorizeResource(TTUser user, HttpEvent evt, @Named("ttusers") DBCollection coll) {
        String otherUserNameOrID = evt.getPath().getElement(3).toString();
        BasicDBObject findOtherUserQuery = new BasicDBObject(name, otherUserNameOrID);
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
        BasicDBObject update = new BasicDBObject("$addToSet", new BasicDBObject(Properties.authorizes,
                otherUser.get("_id")));
        WriteResult res = coll.update(query, update, false, false, WriteConcern.FSYNCED);
        setState(new RespondWith(HttpResponseStatus.ACCEPTED, Timetracker.quickJson("updated", res.getN())));
    }
}
