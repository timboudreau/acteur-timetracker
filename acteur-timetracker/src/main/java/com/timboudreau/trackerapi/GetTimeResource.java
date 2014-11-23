package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.headers.Method;
import com.mastfrog.acteur.mongo.CursorWriter;
import com.mastfrog.acteur.preconditions.Authenticated;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.parameters.Param;
import com.mastfrog.parameters.Params;
import com.mastfrog.util.Strings;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import static com.timboudreau.trackerapi.Properties.*;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import io.netty.channel.Channel;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall(scopeTypes = {CreateCollectionPolicy.class, DBCollection.class})
@Authenticated
@PathRegex(Timetracker.URL_PATTERN_TIME)
@Methods({Method.GET, Method.HEAD})
@BannedUrlParameters(type)
@Description("Query recorded time events, specifying the user and sequence ID")
@Precursors({AuthorizedChecker.class, CreateCollectionPolicy.CreatePolicy.class, TimeCollectionFinder.class})
@Params({
    @Param(value = Properties.fields, required = false, example = "thing1,thing2")})
class GetTimeResource extends Acteur {

    @Inject
    public GetTimeResource(GetTimeResourceParams params, Method method, Channel channel, DBCollection collection, BasicDBObject query, CursorWriter.Factory factory) {
        // Get the list of fieldds, if any, that the caller has restricted the
        // results to - no need to pull anything over from the database we don't
        // actually need

        // Do the query and get the cursor;  add it to the closables to ensure
        DBCursor cur;
        if (params.getFields().isPresent()) {
            DBObject projection = new BasicDBObject();
            for (String field : Strings.split(params.getFields().get())) {
                projection.put(field, 1);
            }
            cur = collection.find(query, projection);
        } else {
            cur = collection.find(query);
        }

        if (!cur.hasNext()) {
            cur.close();
            ok("[]\n");
        } else {
            // Tell the framework we're ready to stream the response
            ok();
            // Set the response writer to be a CursorWriter, which will write out
            // the results one row at a time
            if (method != Method.HEAD && channel.isOpen()) {
                // Create a ResponseWriter which will write and flush one row
                // at atime
                CursorWriter writer = factory.create(cur);
                setResponseWriter(writer);
            }
        }
    }
}
