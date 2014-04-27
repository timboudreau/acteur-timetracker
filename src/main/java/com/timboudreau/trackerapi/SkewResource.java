package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import static com.mastfrog.acteur.headers.Method.GET;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.ParametersMustBeNumbersIfPresent;
import com.mastfrog.acteur.preconditions.Path;
import static com.timboudreau.trackerapi.Properties.time;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@Path("skew")
@Methods(GET)
@ParametersMustBeNumbersIfPresent("time")
@Description("Determine Clock Skew and Request Round trip Time")
public class SkewResource extends Acteur {

    @Inject
    SkewResource(ActeurFactory af, HttpEvent evt, Provider<ObjectMapper> mapper) throws IOException {
        long now = System.currentTimeMillis();
        Optional<Long> l = evt.getLongParameter(time);
        Map<String, Object> m = new HashMap<>();
        m.put("received", now);
        if (l.isPresent()) {
            long remote = l.get();
            m.put("sent", remote);
            m.put("offset", now - remote);
        }
        ok(m);
    }
}
