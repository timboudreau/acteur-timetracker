package com.timboudreau.trackerapi.support;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.errors.Err;
import static com.timboudreau.trackerapi.Timetracker.URL_USER;

/**
 *
 * @author Tim Boudreau
 */
public class AuthorizedChecker extends Acteur {

    @Inject
    AuthorizedChecker(TTUser authUser, @Named(URL_USER) TTUser otherUser) {
        boolean authorized = otherUser.authorizes(authUser);
        if (!authorized) {
            setState(new RespondWith(Err.forbidden(authUser.name()
                    + " not allowed access to data belonging to " + otherUser.name() + "\n")));
        } else {
            next();
        }
    }
}
