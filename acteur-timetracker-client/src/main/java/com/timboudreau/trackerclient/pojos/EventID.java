package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author Tim Boudreau
 */
public class EventID extends ID {

    public EventID(String name) {
        super(name);
    }
    
//    @JsonValue
//    public String name() {
//        return super.name;
//    }
    
    @JsonCreator
    public static EventID create(String name) {
        return new EventID(name);
    }
}
