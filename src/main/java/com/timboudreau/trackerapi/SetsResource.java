package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.Acteur.RespondWith;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.annotations.Precursors;
import com.mastfrog.acteur.headers.Method;
import static com.mastfrog.acteur.headers.Method.GET;
import static com.mastfrog.acteur.headers.Method.HEAD;
import com.mastfrog.acteur.preconditions.BannedUrlParameters;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mongodb.DB;
import static com.timboudreau.trackerapi.Properties.type;
import static com.timboudreau.trackerapi.SetsResource.PAT;
import com.timboudreau.trackerapi.support.Auth;
import com.timboudreau.trackerapi.support.AuthorizedChecker;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TTUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@PathRegex(PAT)
@Methods({GET, HEAD})
@BannedUrlParameters(type)
@Precursors({Auth.class, AuthorizedChecker.class, CreateCollectionPolicy.DontCreatePolicy.class})
@Description("List user's sets of data")
public class SetsResource extends Acteur {

    public static final String PAT = "^users/(.*?)/list$";

    @Inject
    SetsResource(HttpEvent evt, DB db, TTUser user, ObjectMapper mapper) throws IOException {
        List<String> l = new ArrayList<>();
        String pfix = new StringBuilder(evt.getPath().getElement(1).toString()).append('_').toString();
        for (String coll : db.getCollectionNames()) {
            if (coll.startsWith(pfix)) {
                l.add(coll.substring(pfix.length()));
            }
        }
        Collections.sort(l);
        if (evt.getMethod() == Method.HEAD) {
            setState(new RespondWith(200));
        } else {
            setState(new RespondWith(200, mapper.writeValueAsString(l)));
        }
    }
}
