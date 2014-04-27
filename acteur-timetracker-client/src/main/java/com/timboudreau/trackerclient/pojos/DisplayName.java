package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Tim Boudreau
 */
public final class DisplayName extends ID {

    @JsonCreator
    public DisplayName(@JsonProperty("name") String name) {
        super(name);
    }
}
