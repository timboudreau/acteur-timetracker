package com.timboudreau.trackerapi.support;

import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.State;

/**
 *
 * @author Tim Boudreau
 */
public enum CreateCollectionPolicy {

    CREATE, DONT_CREATE;

    public static class CreatePolicy extends Acteur {

        CreatePolicy() {
            next(CREATE);
        }
    }

    public static class DontCreatePolicy extends Acteur {

        DontCreatePolicy() {
            next(DONT_CREATE);
        }
    }
}
