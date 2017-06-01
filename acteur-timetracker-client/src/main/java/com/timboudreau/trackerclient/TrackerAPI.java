package com.timboudreau.trackerclient;

import com.mastfrog.acteur.headers.Method;
import static com.mastfrog.acteur.headers.Method.DELETE;
import static com.mastfrog.acteur.headers.Method.GET;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.util.time.Interval;
import com.mastfrog.webapi.Interpreter;
import com.mastfrog.webapi.WebCall;
import com.mastfrog.webapi.WebCallBuilder;
import com.mastfrog.webapi.WebCallEnum;
import com.mastfrog.webapi.builtin.BodyFromString;
import com.mastfrog.webapi.builtin.BodyFromMap;
import com.mastfrog.webapi.builtin.IntervalParameters;
import com.mastfrog.webapi.builtin.ParameterFromClassNameAndToStringCamelCase;
import com.timboudreau.trackerclient.pojos.DisplayName;
import com.timboudreau.trackerclient.pojos.EventID;
import com.timboudreau.trackerclient.pojos.FieldID;
import com.timboudreau.trackerclient.pojos.OtherID;
import com.timboudreau.trackerclient.pojos.SeriesID;
import com.timboudreau.trackerclient.pojos.UserID;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Map;

/**
 * The timetracker client web api
 *
 * @author Tim Boudreau
 */
public enum TrackerAPI implements WebCallEnum {

    GET_API_VERSION(new WebCallBuilder().method(GET).path("hello")),
    DISTINCT_FIELDS(new WebCallBuilder().method(GET).path("users/{{userid}}/time/{{seriesid}}/distinct").authenticationRequired().addRequiredTypes(UserID.class, SeriesID.class).withDecorator(String.class, FieldNameFromString.class)),
    DELETE_TIME(new WebCallBuilder().method(DELETE).path("users/{{userid}}/time/{{seriesid}}").authenticationRequired().addRequiredTypes(UserID.class, SeriesID.class).withDecorator(EventQuery.class, EventQueryDecorator.class)),
    DELETE_FIELDS(new WebCallBuilder().method(DELETE).path("users/{{userid}}/update/{{seriesid}}/{{eventid}}").authenticationRequired().addRequiredTypes(UserID.class, SeriesID.class, EventID.class)),
    MODIFY_FIELDS(new WebCallBuilder().method(POST).withDecorator(Map.class, BodyFromMap.class).path("users/{{userid}}/update/{{seriesid}}/{{eventid}}").authenticationRequired().addRequiredTypes(UserID.class, SeriesID.class, EventID.class)),
    @SuppressWarnings("unchecked")
    GET_TIMES(new WebCallBuilder().path("users/{{userid}}/time/{{seriesid}}").authenticationRequired().withDecorator(EventQuery.class, EventQueryDecorator.class).addRequiredTypes(UserID.class, SeriesID.class)),
    @SuppressWarnings("unchecked")
    SIGNUP(new WebCallBuilder().addRequiredTypes(UserID.class, DisplayName.class).withDecorator(DisplayName.class, ParameterFromClassNameAndToStringCamelCase.class).withDecorator(String.class, BodyFromString.class).method(PUT).path("users/{{userid}}/signup")),
    LIST_USERS(GET, "all", false),
    LIST_SERIES(GET, "users/{{userid}}/list", true, UserID.class),
    AUTHORIZE_USER(PUT, "users/{{userid}}/authorize/{{otherid}}", true, UserID.class, OtherID.class),
    ADD_TIME(new WebCallBuilder().method(Method.PUT).path("users/{{userid}}/time/{{seriesid}}").withDecorator(Interval.class, IntervalParameters.class).authenticationRequired().addRequiredTypes(UserID.class, SeriesID.class, Interval.class)),
    UPDATE_TIMES(new WebCallBuilder().method(Method.PUT).path("users/{{userid}}/update/{{seriesid}}/{{fieldid}}").withDecorator(String.class, BodyFromString.class).addRequiredTypes(UserID.class, SeriesID.class, FieldID.class).authenticationRequired()),
    DELETE_FIELD(DELETE, "users/{{userid}}/update/{{seriesid}}/{{fieldid}}", true, UserID.class, SeriesID.class, FieldID.class),
    DEAUTHORIZE_USER(PUT, "users/{{userid}}/deauthorize/{{otherId}}", true, UserID.class, OtherID.class),
    ADJUST_TIMES(PUT, "users/{{userid}}/adjust/{{seriesid}}", true, UserID.class, SeriesID.class),
    TOTAL_TIMES(GET, "users/{{userid}}/total/{{seriesid}}", true, UserID.class, SeriesID.class),
    SET_PASSWORD(new WebCallBuilder().method(PUT).addRequiredTypes(UserID.class, String.class).authenticationRequired().path("users/{{userid}}/password").withDecorator(String.class, BodyFromString.class)),
    GET_SKEW(GET, "skew", false),
    LIVE_SESSION(new WebCallBuilder().method(PUT).authenticationRequired()
            .stayOpen().path("users/{{userid}}/sessions/{{seriesid}}").addRequiredTypes(UserID.class, SeriesID.class).interpreter(EmptyRI.class)),
    WHO_AM_I(GET, "whoami", true);

    private final WebCall call;

    @SuppressWarnings("LeakingThisInConstructor")
    TrackerAPI(WebCallBuilder builder) {
        call = builder.id(this).build();
    }

    TrackerAPI(Method method, String urlPattern, boolean requiresAuth, Class<?>... requiredTypes) {
        this(new WebCallBuilder().method(method).addRequiredTypes(requiredTypes).path(urlPattern).authenticationRequired(requiresAuth));
    }

    @Override
    public WebCall get() {
        return call;
    }
    
    static class EmptyRI extends Interpreter {
        @Override
        public <T> T interpret(HttpResponseStatus status, HttpHeaders headers, ByteBuf contents, Class<T> as) throws Exception {
            return null;
        }
    }
}
