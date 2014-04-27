package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Tim Boudreau
 */
public final class UserID extends ID {

    @JsonCreator
    public UserID(String name) {
        super(name);
    }
    
}
