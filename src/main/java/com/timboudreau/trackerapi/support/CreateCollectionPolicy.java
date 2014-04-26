package com.timboudreau.trackerapi.support;

import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.State;

/**
 *
 * @author Tim Boudreau
 */
public enum CreateCollectionPolicy {
    CREATE, DONT_CREATE;

    public Acteur toActeur() {
        return new Acteur() {
            @Override
            public State getState() {
                return new Acteur.ConsumedLockedState(CreateCollectionPolicy.this);
            }
        };
    }

    public static class CreatePolicy extends Acteur {
        CreatePolicy() {
            setState(new Acteur.ConsumedLockedState(CREATE));
        }
    }
    
    public static class DontCreatePolicy extends Acteur {
        DontCreatePolicy() {
            setState(new ConsumedLockedState(DONT_CREATE));
        }
    }
}
