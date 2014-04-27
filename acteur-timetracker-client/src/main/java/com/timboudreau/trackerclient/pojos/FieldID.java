package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Tim Boudreau
 */
public final class FieldID extends ID {

    @JsonCreator
    public FieldID(String name) {
        super(name);
    }
}
