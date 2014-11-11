package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import static com.mastfrog.acteur.headers.Method.GET;
import com.mastfrog.acteur.preconditions.Authenticated;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequiredUrlParameters;
import com.mongodb.DBCollection;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;

/**
 * Get all distinct values of some field
 *
 * @author Tim Boudreau
 */
@HttpCall(order = Integer.MIN_VALUE)
@PathRegex("^users/(.*?)/time/(.*?)/distinct$")
@Methods(GET)
@RequiredUrlParameters("field")
@Authenticated
@Description("Get all unique values of a field, e.g. /users/joe/time/stuff/distinct?field=activity")
@Precursors({CreateCollectionPolicy.DontCreatePolicy.class, AuthorizedChecker.class, TimeCollectionFinder.class})
public class DistinctResource extends Acteur {

    @Inject
    DistinctResource(HttpEvent evt, DBCollection coll) {
        String field = evt.getParameter("field");
        setState(new RespondWith(200, coll.distinct(field)));
    }
}
