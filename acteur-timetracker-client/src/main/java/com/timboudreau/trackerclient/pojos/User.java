package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

/**
 *
 * @author Tim Boudreau
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class User {

    public final String[] names;
    public final String displayName;
    public final ZonedDateTime created;
    public final int version;
    public final OtherID[] authorizes;
    public final ZonedDateTime lastModified;
    public final UserID _id;

    @JsonCreator
    public User(@JsonProperty("name") String[] names,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("created") ZonedDateTime created,
            @JsonProperty("version") int version,
            @JsonProperty("authorizes") OtherID[] authorizes,
            @JsonProperty("lastModified") ZonedDateTime lastModified,
            @JsonProperty("_id") UserID _id) {
        this.names = names;
        this.displayName = displayName;
        this.created = created;
        this.version = version;
        this.authorizes = authorizes;
        this.lastModified = lastModified;
        this._id = _id;
    }

    @JsonIgnore
    public String getName() {
        return names[0];
    }
    
    @JsonIgnore
    public UserID getID() {
        return _id;
    }

    public boolean equals(Object o) {
        return o instanceof User && ((User) o)._id.equals(_id);
    }

    public int hashCode() {
        return _id.hashCode();
    }

    @Override
    public String toString() {
        return "User{" + "names=" + names + ", displayName=" + displayName + ", created=" + created + ", version=" + version + ", authorizes=" + authorizes + ", _id=" + _id + '}';
    }
}
