package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import static com.mastfrog.acteur.headers.Method.GET;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.ParametersMustBeNumbersIfPresent;
import com.mastfrog.acteur.preconditions.Path;
import static com.timboudreau.trackerapi.Properties.time;
import com.timboudreau.trackerapi.support.TTUser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall(scopeTypes = {TTUser.class})
@Path("skew")
@Methods(GET)
@ParametersMustBeNumbersIfPresent("time")
@Description("Determine Clock Skew and Request Round trip Time")
public class SkewResource extends Acteur {

    @Inject
    SkewResource(HttpEvent evt) throws IOException {
        long now = System.currentTimeMillis();
        Optional<Long> l
                = evt.uriQueryParameter(time, Long.class);
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
