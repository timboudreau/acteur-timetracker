package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.errors.Err;
import com.mastfrog.acteur.headers.Headers;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequiredUrlParameters;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.timboudreau.trackerapi.AddTimeResource.CheckParameters;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TTUser;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.Map;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import static com.timboudreau.trackerapi.Properties.*;
import static com.timboudreau.trackerapi.RecordTimeConnectionIsOpenResource.buildQueryFromURLParameters;
import com.timboudreau.trackerapi.support.AuthorizedChecker;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@BasicAuth
@Methods({PUT, POST})
@PathRegex(Timetracker.URL_PATTERN_TIME)
@RequiredUrlParameters({"start", "end"})
@BannedUrlParameters({"added", "type"})
@Precursors({CheckParameters.class, CreateCollectionPolicy.CreatePolicy.class, 
    AuthorizedChecker.class, TimeCollectionFinder.class})
@Description("Add A Time Event")
final class AddTimeResource extends Acteur {

    static final int MAX_PROPERTIES = 10;

    static class CheckParameters extends Acteur {

        @Inject
        CheckParameters(HttpEvent evt) {
            try {
                DateTime startTime = new DateTime(evt.getLongParameter(start).get());
                DateTime endTime = new DateTime(evt.getLongParameter(end).get());
                DateTime now = DateTime.now();
                DateTime twentyYearsAgo = now.minus(Duration.standardDays(365 * 20));
                if (twentyYearsAgo.isAfter(startTime)) {
                    setState(new RespondWith(Err.badRequest(
                            "Too long ago - minimum is " + twentyYearsAgo)));
                    return;
                }
                Interval interval = new Interval(startTime, endTime);
                setState(new ConsumedLockedState(interval));
            } catch (NumberFormatException e) {
                setState(new RespondWith(Err.badRequest("Start or end is not a number: '" + evt.getParameter(start) + "' and '" + evt.getParameter(end))));
            }
        }
    }

    @Inject
    AddTimeResource(HttpEvent evt, DBCollection coll, TTUser user, Interval interval) throws IOException {
        long startVal = interval.getStartMillis();
        long endVal = interval.getEndMillis();
        if (endVal - startVal <= 0) {
            setState(new RespondWith(Err.badRequest("Start is equal to or after end '"
                    + interval.getStart() + "' and '" + interval.getEnd() + "'")));
            return;
        }
        BasicDBObject toWrite = new BasicDBObject(type, time)
                .append(start, startVal)
                .append(end, endVal)
                .append(duration, endVal - startVal)
                .append(added, DateTime.now().getMillis())
                .append(by, user.idAsString())
                .append(version, 0);

        String err = buildQueryFromURLParameters(evt, toWrite, Properties.start, Properties.end, Properties.duration);
        if (err != null) {
            setState(new RespondWith(Err.badRequest(err)));
            return;
        }
        coll.insert(toWrite, WriteConcern.SAFE);
        Map<String,Object> m = toWrite.toMap();
        ObjectId id = (ObjectId) m.get(_id);
        if (id != null) {
            add(Headers.stringHeader("X-Tracker-ID"), id.toString());
            if (evt.getParameter("localId") != null) {
                add(Headers.stringHeader("X-Local-ID"), evt.getParameter("localId"));
            }
        }
        setState(new RespondWith(HttpResponseStatus.ACCEPTED, m));
    }
}
