package com.timboudreau.trackerapi;

import com.timboudreau.trackerapi.support.Intervals;
import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.InjectUrlParametersAs;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.parameters.Param;
import com.mastfrog.parameters.Params;
import com.mastfrog.parameters.Types;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import java.io.IOException;
import org.joda.time.Interval;
import static com.timboudreau.trackerapi.Properties.*;
import static com.timboudreau.trackerapi.TotalTimeResource.PAT;
import com.timboudreau.trackerapi.support.AuthorizedChecker;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@BasicAuth
@PathRegex(PAT)
@BannedUrlParameters("type")
@Precursors({AuthorizedChecker.class, CreateCollectionPolicy.DontCreatePolicy.class, TimeCollectionFinder.class})
@Description("Tallys total times for events matching the query terms in the URL parameters")
@Params(value={
    @Param(value=Properties.detail, type = Types.BOOLEAN, defaultValue = "true", required = false)
    ,@Param(value=Properties.summary, type = Types.BOOLEAN, defaultValue = "true", required = false)
})
final class TotalTimeResource extends Acteur {

    public static final String PAT = "^users/(.*?)/total/(.*?)$";

    @Inject
    public TotalTimeResource(TotalTimeResourceParams params, DBCollection collection, BasicDBObject query) throws IOException {
        query.put(type, time);
        query.remove(detail);

        Intervals ivals = new Intervals();
        try (DBCursor cur = collection.find(query)) {
            while (cur.hasNext()) {
                DBObject ob = cur.next();
                Long startTime = (Long) ob.get(start);
                Long endTime = (Long) ob.get(end);
                ivals.add(new Interval(startTime, endTime), "" + ob.get(Properties._id));
            }
        }
        setState(new RespondWith(200, ivals.toJSON(params.getDetail(), params.getSummary())));
    }
}
