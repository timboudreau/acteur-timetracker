package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Tim Boudreau
 */
public class EventID extends ID {

    @JsonCreator
    EventID(String name) {
        super(name);
    }
}
