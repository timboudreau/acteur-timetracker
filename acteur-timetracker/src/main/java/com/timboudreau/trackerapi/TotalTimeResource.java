package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.preconditions.Authenticated;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.util.time.Interval;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import static com.timboudreau.trackerapi.Properties.*;
import static com.timboudreau.trackerapi.TotalTimeResource.PAT;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.Intervals;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import java.io.IOException;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@Authenticated
@PathRegex(PAT)
@BannedUrlParameters("type")
@Precursors({AuthorizedChecker.class, CreateCollectionPolicy.DontCreatePolicy.class, TimeCollectionFinder.class})
@Description("Tallys total times for events matching the query terms in the URL parameters")
final class TotalTimeResource extends Acteur {

    public static final String PAT = "^users/(.*?)/total/(.*?)$";

    @Inject
    public TotalTimeResource(HttpEvent evt, DBCollection collection, BasicDBObject query) throws IOException {
        query.put(type, time);
        query.remove(detail);

        boolean detail = true;
        if (evt.urlParameter(Properties.detail) != null && "false".equals(evt.urlParameter(Properties.detail))) {
            detail = false;
        }
        boolean summary = true;
        if (evt.urlParameter(Properties.summary) != null && "false".equals(evt.urlParameter(Properties.summary))) {
            summary = false;
        }

        Intervals ivals = new Intervals();
        try (DBCursor cur = collection.find(query)) {
            while (cur.hasNext()) {
                DBObject ob = cur.next();
                Long startTime = (Long) ob.get(start);
                Long endTime = (Long) ob.get(end);
                ivals.add(Interval.create(startTime, endTime), "" + ob.get(Properties._id));
            }
        }
        ok(ivals.toJSON(detail, summary));
    }
}
