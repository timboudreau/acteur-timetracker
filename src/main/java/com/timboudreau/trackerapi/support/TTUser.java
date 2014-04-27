package com.timboudreau.trackerapi.support;

import com.mastfrog.acteur.auth.SimpleUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bson.types.ObjectId;

/**
 *
 * @author Tim Boudreau
 */
public final class TTUser extends SimpleUser.Base<ObjectId> {

    private final List<ObjectId> authorizes;

    public TTUser(String name, ObjectId id, int version, List<ObjectId> authorizes) {
        super(name, version, id);
        this.authorizes = authorizes == null ? Collections.<ObjectId>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(authorizes));
    }

    boolean authorizes(TTUser other) {
        return other.equals(this) || authorizes(other.id());
    }

    boolean authorizes(ObjectId id) {
        return authorizes != null && authorizes.contains(id);
    }
}
