package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mastfrog.util.Checks;

/**
 *
 * @author Tim Boudreau
 */
public class ID implements Comparable<Object> {

    public final String name;

    @JsonCreator
    public ID(String name) {
        Checks.notNull("name", name);
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean is(ID other) {
        return name.equals(other.name);
    }

    @JsonValue
    public String id() {
        return name;
    }

    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && ((ID) o).name.equals(name);
    }

    public int hashCode() {
        return (getClass().getName() + name).hashCode();
    }

    @Override
    public int compareTo(Object t) {
        if (t == null) {
            t = "null";
        }
        return toString().compareToIgnoreCase(t.toString());
    }
}
