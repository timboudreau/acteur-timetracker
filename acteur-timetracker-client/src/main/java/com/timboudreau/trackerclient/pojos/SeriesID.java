package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Tim Boudreau
 */
public final class SeriesID extends ID {

    @JsonCreator
    public SeriesID(String name) {
        super(name);
    }
}
