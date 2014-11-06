package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.Acteur.RespondWith;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.errors.Err;
import static com.mastfrog.acteur.headers.Method.POST;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.InjectRequestBodyAs;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import static com.timboudreau.trackerapi.ModifyEventsFieldsByIdResource.PAT;
import static com.timboudreau.trackerapi.Properties.*;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.Map;
import org.bson.types.ObjectId;

/**
 *
 * @author tim
 */
@HttpCall
@BasicAuth
@Methods({POST})
@PathRegex(PAT)
@BannedUrlParameters({type, version})
@InjectRequestBodyAs(Map.class)
@Precursors({CreateCollectionPolicy.DontCreatePolicy.class, TimeCollectionFinder.class})
@Description("Modify or add data to a single event, including the modified fields in the body")
public final class ModifyEventsFieldsByIdResource extends Acteur {

    public static final String PAT = "^users/(.*?)/update/(.*?)/(.*?)$";

    @SuppressWarnings("unchecked")
    @Inject
    public ModifyEventsFieldsByIdResource(HttpEvent evt, DBCollection collection, BasicDBObject query, Map body) throws IOException {
        query.put(type, time);
        ObjectId oid;
        try {
            oid = new ObjectId(evt.getPath().getLastElement().toString());
        } catch (Exception e) {
            setState(new RespondWith(Err.badRequest("Not an ID: " + evt.getPath().getLastElement())));
            return;
        }
        query.put(Properties._id, oid);
        BasicDBObject mod = new BasicDBObject(body);
        // XXX check for start / end / dur and recompute whichever is missing
        for (String banned : new String[] { Properties._id, Properties.version, Properties.created, Properties.running, "createdBy"}) {
            mod.removeField(banned);
        }
        if (!checkNumber(mod, Properties.duration) || !checkNumber(mod, Properties.start) || !checkNumber(mod, Properties.end)) {
            return;
        }
        if (mod.containsField(Properties.duration) && !(mod.get(Properties.duration) instanceof Number)) {
            if (mod.get(Properties.duration) instanceof String) {
                mod.put(Properties.duration, Long.parseLong(mod.get(Properties.duration).toString()));
            } else {
                setState(new RespondWith(Err.badRequest("Duration must be a number but is " + mod.get(Properties.duration))));
                return;
            }
        }
        DBObject modification = new BasicDBObject("$set", mod).append("$inc", new BasicDBObject(Properties.version, 1));
        WriteResult res = collection.update(query, modification, false, true, WriteConcern.ACKNOWLEDGED);
        String resultJson = Timetracker.quickJson("updated", res.getN());
        setState(new RespondWith(res.getN() > 0 ? HttpResponseStatus.ACCEPTED
                : HttpResponseStatus.GONE, resultJson));
    }

    private boolean checkNumber(BasicDBObject mod, String key) {
        if (mod.containsField(key) && !(mod.get(key) instanceof Number)) {
            if (mod.get(key) instanceof String) {
                try {
                    mod.put(key, Long.parseLong(mod.get(key).toString()));
                } catch (NumberFormatException ex) {
                    setState(new RespondWith(Err.badRequest("Duration must be a string but is " + mod.get(key))));
                    return false;
                }
            } else {
                setState(new RespondWith(Err.badRequest("Duration must be a string but is " + mod.get(key))));
                return false;
            }
        }
        return true;
    }
}
