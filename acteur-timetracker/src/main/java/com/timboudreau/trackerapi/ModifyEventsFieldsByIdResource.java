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
@Precursors({CreateCollectionPolicy.DontCreatePolicy.class, TimeCollectionFinder.class})
@Description("Modify or add data to an event or set of events, or delete fields.  "
        + "For deletions, the parameters should be simply field_name=1 for "
        + "eaach field to be removed")
public final class ModifyEventsFieldsByIdResource extends Acteur {

    public static final String PAT = "^users/(.*?)/update/(.*?)/(.*?)$";

    @Inject
    public ModifyEventsFieldsByIdResource(HttpEvent evt, DBCollection collection, BasicDBObject query) throws IOException {
        query.put(type, time);
        ObjectId oid;
        try {
            oid = new ObjectId(evt.getPath().getLastElement().toString());
        } catch (Exception e) {
            setState(new RespondWith(Err.badRequest("Not an ID: " + evt.getPath().getLastElement())));
            return;
        }
        query.put("_id", oid);
        BasicDBObject mod;
        try {
            mod = new BasicDBObject(evt.getContentAsJSON(Map.class));
        } catch (JsonMappingException e) {
            setState(new RespondWith(Err.badRequest("Bad JSON: " + e.getMessage())));
            return;
        }
        System.out.println("BODY IS " + evt.getContentAsJSON(String.class));
        if (!checkNumber(mod, "dur") || !checkNumber(mod, "start") || !checkNumber(mod, "end")) {
            return;
        }
        if (mod.containsField("dur") && !(mod.get("dur") instanceof Number)) {
            if (mod.get("dur") instanceof String) {
                mod.put("dur", Long.parseLong(mod.get("dur").toString()));
            } else {
                setState(new RespondWith(Err.badRequest("Duration must be a string but is " + mod.get("dur"))));
                return;
            }
        }
        System.out.println("Loaded body " + mod);
        System.out.println("QUERY IS " + query);
        DBObject modification = new BasicDBObject("$set", mod).append("$inc", new BasicDBObject("version", 1));
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
