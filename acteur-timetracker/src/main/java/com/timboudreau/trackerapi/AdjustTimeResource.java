package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.errors.Err;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.mongo.util.UpdateBuilder;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.InjectParametersAsInterface;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.ParametersMustBeNumbersIfPresent;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequireAtLeastOneUrlParameterFrom;
import com.mastfrog.acteur.preconditions.UrlParametersMayNotBeCombined;
import com.mastfrog.acteur.preconditions.UrlParametersMayNotBeCombinedSets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.timboudreau.trackerapi.AdjustTimeResource.AdjustParameters;
import static com.timboudreau.trackerapi.AdjustTimeResource.URL_PATTERN_ADJUST;
import static com.timboudreau.trackerapi.Properties.*;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;
import java.io.IOException;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@BasicAuth
@PathRegex(URL_PATTERN_ADJUST)
@Methods({PUT, POST})
@BannedUrlParameters("type")
@UrlParametersMayNotBeCombinedSets({
    @UrlParametersMayNotBeCombined({"newStart", "shift", "moveTo"}),
    @UrlParametersMayNotBeCombined({"newEnd", "shift", "moveTo"})
})
@RequireAtLeastOneUrlParameterFrom({"shift", "moveTo", "length", "newStart", "newEnd"})
@ParametersMustBeNumbersIfPresent(value = {"newStart", "newEnd", "shift", "moveTo", "length"}, allowDecimal = false, allowNegative = true)
@Description("Adjust records by shifting, moving or changing start, end or length")
@InjectParametersAsInterface(AdjustParameters.class)
@Precursors({AuthorizedChecker.class, CreateCollectionPolicy.DontCreatePolicy.class,
    TimeCollectionFinder.class})
class AdjustTimeResource extends Acteur {

    public static final String URL_PATTERN_ADJUST = "^users/(.*?)/adjust/(.*?)$";

    @Inject
    public AdjustTimeResource(HttpEvent evt, DBCollection collection, BasicDBObject query, AdjustParameters params) throws IOException {
        query.put(type, time);
        query.remove(detail);

        UpdateBuilder update = UpdateBuilder.$().increment(version);
        Long shift = params.shift();
        if (shift != null) {
            update.increment(start, shift).increment(end, shift);
            WriteResult res = collection.update(query, update.build(), false, true, WriteConcern.ACKNOWLEDGED);
            setState(new RespondWith(res.getN() > 0 ? 200 : 400, Timetracker.quickJson("updated", res.getN())));
        } else {
            update.increment(version);
            DBObject ob = collection.findOne(query);
            if (ob == null) {
                setState(new RespondWith(Err.gone("No matching object")));
                return;
            }
            long newStart = params.newStart() == null ? (Long) ob.get(start) : params.newStart();
            long newEnd = params.newEnd() == null ? (Long) ob.get(end) : params.newEnd();
            if (params.moveTo() == null && newEnd < newStart) {
                setState(new RespondWith(Err.gone("Start " + newStart
                        + " will be after end " + newEnd)));
                return;
            }
            if (params.newStart() != null || params.newEnd() != null) {
                if (params.newStart() != null) {
                    update.set(start, newStart);
                }
                if (params.newEnd() != null) {
                    update.set(end, newEnd);
                }
                update.set(duration, newEnd - newStart);
            }
            if (params.length() != null) {
                if (params.length() < 0) {
                    setState(new RespondWith(Err.badRequest("Negative length")));
                    return;
                }
                if (params.newEnd() != null) {
                    update.set(start, newEnd - params.length());
                } else {
                    update.set(end, newStart + params.length());
                }
                update.set(duration, params.length());
            }
            if (params.moveTo() != null) {
                long dur = params.length() == null ? newEnd - newStart : params.length();
                update.set(start, params.moveTo());
                update.set(duration, dur);
                update.set(end, params.moveTo() + dur);
            }
            WriteResult res = collection.update(query, update.build(), false, false, WriteConcern.ACKNOWLEDGED);
            setState(new RespondWith(res.getN() > 0 ? 200 : 400, Timetracker.quickJson("updated", res.getN())));
        }
    }

    public interface AdjustParameters {

        public Long newStart();

        public Long moveTo();

        public Long newEnd();

        public Long shift();

        public Long length();
    }
}
