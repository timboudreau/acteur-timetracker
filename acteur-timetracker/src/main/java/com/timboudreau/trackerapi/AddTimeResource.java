package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.errors.Err;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.Authenticated;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequiredUrlParameters;
import com.mastfrog.util.time.Interval;
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
import static com.timboudreau.trackerapi.Properties.*;
import static com.timboudreau.trackerapi.RecordTimeConnectionIsOpenResource.XLI;
import static com.timboudreau.trackerapi.RecordTimeConnectionIsOpenResource.XTI;
import static com.timboudreau.trackerapi.RecordTimeConnectionIsOpenResource.buildQueryFromURLParameters;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import java.time.Duration;
import java.time.Instant;

/**
 * Adds a time entry
 *
 * @author Tim Boudreau
 */
@HttpCall
@Authenticated
@Methods({PUT})
@PathRegex(Timetracker.URL_PATTERN_TIME)
@RequiredUrlParameters({Properties.start, Properties.end})
@BannedUrlParameters({Properties.added, Properties.type})
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
                Instant startTime = Instant.ofEpochMilli(evt.longUrlParameter(start).get());
                Instant endTime = Instant.ofEpochMilli(evt.longUrlParameter(end).get());
                Instant now = Instant.now();
                Instant twentyYearsAgo = now.minus(Duration.ofDays(365 * 20));
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
                if (startTime.equals(endTime)) {
                    reply(BAD_REQUEST, "Zero length event");
                    return;
                }
                Interval interval = Interval.create(startTime, endTime);
                next(interval);
            } catch (NumberFormatException e) {
                reply(Err.badRequest("Start or end is not a number: '" + evt.urlParameter(start) + "' and '" + evt.urlParameter(end)));
            }
        }
    }

    @Inject
    @SuppressWarnings("unchecked")
    AddTimeResource(HttpEvent evt, DBCollection coll, TTUser user, Interval interval) throws IOException {
        // We have validated values
        long startVal = interval.getStartMillis();
        long endVal = interval.getEndMillis();
        assert endVal != startVal;
        // The entity we will write to the database
        BasicDBObject toWrite = new BasicDBObject(type, time)
                .append(start, startVal)
                .append(end, endVal)
                .append(duration, endVal - startVal)
                .append(added, Instant.now().toEpochMilli())
                .append(by, user.idAsString())
                .append(version, 0);
        
        System.out.println("WILL WRITE " + toWrite);

        // Add the other URL properties to the BasicDBObject, returning an
        // error message if something goes wrong
        String err = buildQueryFromURLParameters(evt, toWrite, Properties.start, Properties.end, Properties.duration);
        if (err != null) {
            setState(new RespondWith(Err.badRequest(err)));
            return;
        }
        // Write it to the database
        coll.insert(toWrite, WriteConcern.MAJORITY);
        // Add a few headers and return a result
        Map<String, Object> m = toWrite.toMap();
        ObjectId id = (ObjectId) m.get(_id);
        if (id != null) {
            add(XTI, id.toString());
            if (evt.urlParameter(Properties.localId) != null) {
                add(XLI, evt.urlParameter(Properties.localId));
            }
        }
        System.out.println("MAP VERSION: " + m);
        setState(new RespondWith(HttpResponseStatus.ACCEPTED, m));
    }
}
