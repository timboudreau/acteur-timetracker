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
 * Adds a time entry
 *
 * @author Tim Boudreau
 */
@HttpCall
@BasicAuth
@Methods({PUT})
@PathRegex(Timetracker.URL_PATTERN_TIME)
@RequiredUrlParameters({"start", "end"})
@BannedUrlParameters({"added", "type"})
@Precursors({CheckParameters.class, CreateCollectionPolicy.CreatePolicy.class,
    AuthorizedChecker.class, TimeCollectionFinder.class})
@Description("Add A Time Event")
final class AddTimeResource extends Acteur {

    static final int MAX_PROPERTIES = 10;

    // This runs first
    static class CheckParameters extends Acteur {

        @Inject
        CheckParameters(HttpEvent evt) {
            try {
                // Get the start and end parameters and check them for validity
                DateTime startTime = new DateTime(evt.getLongParameter(start).get());
                DateTime endTime = new DateTime(evt.getLongParameter(end).get());
                DateTime now = DateTime.now();
                DateTime twentyYearsAgo = now.minus(Duration.standardDays(365 * 20));
                // We're not building an api for world history here
                if (twentyYearsAgo.isAfter(startTime)) {
                    setState(new RespondWith(Err.badRequest(
                            "Too long ago - minimum is " + twentyYearsAgo)));
                    return;
                }
                if (endTime.isBefore(startTime)) {
                    setState(new RespondWith(Err.badRequest("Start is equal to or after end '"
                            + startTime + "' and '" + endTime + "'")));
                    return;
                }
                // Create an Interval which will be injected into the AddTimeResource
                // constructor
                Interval interval = new Interval(startTime, endTime);
                setState(new ConsumedLockedState(interval));
            } catch (NumberFormatException e) {
                setState(new RespondWith(Err.badRequest("Start or end is not a number: '" + evt.getParameter(start) + "' and '" + evt.getParameter(end))));
            }
        }
    }

    @Inject
    AddTimeResource(HttpEvent evt, DBCollection coll, TTUser user, Interval interval) throws IOException {
        // We have validated values
        long startVal = interval.getStartMillis();
        long endVal = interval.getEndMillis();
        // The entity we will write to the database
        BasicDBObject toWrite = new BasicDBObject(type, time)
                .append(start, startVal)
                .append(end, endVal)
                .append(duration, endVal - startVal)
                .append(added, DateTime.now().getMillis())
                .append(by, user.idAsString())
                .append(version, 0);

        // Add the other URL properties to the BasicDBObject, returning an
        // error message if something goes wrong
        String err = buildQueryFromURLParameters(evt, toWrite, Properties.start, Properties.end, Properties.duration);
        if (err != null) {
            setState(new RespondWith(Err.badRequest(err)));
            return;
        }
        // Write it to the database
        coll.insert(toWrite, WriteConcern.SAFE);
        // Add a few headers and return a result
        Map<String, Object> m = toWrite.toMap();
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
