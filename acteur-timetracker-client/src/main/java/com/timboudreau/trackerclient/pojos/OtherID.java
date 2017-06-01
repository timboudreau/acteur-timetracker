package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author Tim Boudreau
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, creatorVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public final class OtherID extends ID {

    public OtherID(String name) {
        super(name);
    }
    
    @JsonValue
    public String id() {
        return super.id();
    }
    
    @JsonCreator
    public static OtherID create(String name) {
        return new OtherID(name);
    }
}
