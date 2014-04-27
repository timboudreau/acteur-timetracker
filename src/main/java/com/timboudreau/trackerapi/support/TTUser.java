package com.timboudreau.trackerapi.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bson.types.ObjectId;

/**
 *
 * @author Tim Boudreau
 */
public final class TTUser {

    public final String name;
    public final ObjectId id;
    private final int version;
    private final List<ObjectId> authorizes;

    public TTUser(String name, ObjectId id, int version, List<ObjectId> authorizes) {
        this.name = name;
        this.id = id;
        this.version = version;
        this.authorizes = authorizes == null ? Collections.<ObjectId>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(authorizes));
    }

    boolean authorizes(TTUser other) {
        return other.equals(this) || authorizes(other.id);
    }

    boolean authorizes(ObjectId id) {
        return authorizes != null && authorizes.contains(id);
    }

    public int version() {
        return version;
    }

    public String idAsString() {
        return id.toString();
    }

    public boolean equals(Object o) {
        return o == this ? true : o instanceof TTUser
                ? ((TTUser) o).id.equals(id) : false;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        return name + " (" + id + ")";
    }
}
