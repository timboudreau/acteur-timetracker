package com.timboudreau.trackerapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.headers.Headers;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.MaximumPathLength;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequiredUrlParameters;
import com.mastfrog.acteur.util.PasswordHasher;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import static com.timboudreau.trackerapi.Properties.*;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@PathRegex("^users/(.*?)/signup$")
@MaximumPathLength(3)
@Methods({PUT, POST})
@RequiredUrlParameters("displayName")
@Description("New user signup")
class SignUpResource extends Acteur {

    public static final int MAX_PASSWORD_LENGTH = 40;
    public static final int MIN_PASSWORD_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 40;
    public static final int MIN_USERNAME_LENGTH = 3;
    private final DBObject nue;
    private final HttpEvent evt;
    private final ObjectMapper mapper;

    @Inject
    SignUpResource(@Named("ttusers") DBCollection coll, HttpEvent evt, PasswordHasher crypto, ObjectMapper mapper) throws UnsupportedEncodingException, IOException {
        this.mapper = mapper;
        this.evt = evt;
        add(Headers.CONTENT_TYPE, MediaType.JSON_UTF_8);

        String userName = URLDecoder.decode(evt.getPath().getElement(1).toString(),
                "UTF-8");

        nue = new BasicDBObject(name, new String[]{userName})
                .append(displayName, evt.getParameter(displayName))
                .append(created, DateTime.now().getMillis())
                .append(version, 0).append("authorizes", new ObjectId[0]);

        if (!(evt.getRequest() instanceof FullHttpRequest)) {
            if (userName.length() > MAX_USERNAME_LENGTH) {
                setState(new RespondWith(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "Not a " + FullHttpRequest.class.getName()));
                return;
            }
            setState(new RespondWith(HttpResponseStatus.REQUEST_URI_TOO_LONG,
                    "Max username length " + MAX_USERNAME_LENGTH));
            return;
        }
        if (userName.length() < MIN_USERNAME_LENGTH) {
            setState(new RespondWith(HttpResponseStatus.REQUEST_URI_TOO_LONG,
                    "Max username length " + MAX_USERNAME_LENGTH));
            return;
        }
        DBObject existing = coll.findOne(new BasicDBObject("name", userName));
        if (existing != null) {
            setState(new RespondWith(HttpResponseStatus.CONFLICT,
                    "A user named '" + userName + "' already exists"));
            return;
        }
        String password = evt.getContentAsJSON(String.class);
        if (password.length() > MAX_PASSWORD_LENGTH) {
            setState(new RespondWith(HttpResponseStatus.BAD_REQUEST,
                    "Password to long (" + password.length()
                    + ")  max " + MAX_PASSWORD_LENGTH));
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            setState(new RespondWith(HttpResponseStatus.BAD_REQUEST,
                    "Password to short (" + password.length()
                    + ")  min " + MIN_PASSWORD_LENGTH));
            return;
        }
        String encrypted = crypto.encryptPassword(password);
        nue.put(pass, encrypted);
        nue.put(origPass, encrypted);
        coll.save(nue, WriteConcern.FSYNCED);
        setState(new RespondWith(HttpResponseStatus.CREATED, nue.toMap()));
    }
}
