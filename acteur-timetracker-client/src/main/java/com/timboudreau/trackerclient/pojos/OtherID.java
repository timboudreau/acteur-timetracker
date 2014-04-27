package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Tim Boudreau
 */
public final class OtherID extends ID {

    @JsonCreator
    public OtherID(String name) {
        super(name);
    }
}
