package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import static com.mastfrog.acteur.headers.Method.GET;
import com.mastfrog.acteur.preconditions.BasicAuth;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequiredUrlParameters;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TimeCollectionFinder;

/**
 *
 * @author tim
 */
@HttpCall
@PathRegex("^users/(.*?)/time/(.*?)/distinct$")
@Methods(GET)
@RequiredUrlParameters("field")
@BasicAuth
@Precursors({CreateCollectionPolicy.DontCreatePolicy.class, AuthorizedChecker.class, TimeCollectionFinder.class})
public class DistinctResource extends Acteur {

    @Inject
    DistinctResource(HttpEvent evt, DBCollection coll) {
        String field = evt.getParameter("field");
        BasicDBObject cmd = new BasicDBObject("distinct", coll.getName()).append("key", field);
        CommandResult res = coll.getDB().command(cmd);
        setState(new RespondWith(200, res.get("values")));
    }
}
