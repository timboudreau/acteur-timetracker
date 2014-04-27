package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author tim
 */
public class Acknowledgement {

    public final short updated;
    @JsonCreator
    public Acknowledgement(@JsonProperty("updated") short updated) {
        this.updated = updated;
    }
    
    public int updated() {
        return updated;
    }
}
