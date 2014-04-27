package com.timboudreau.trackerclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastfrog.acteur.util.BasicCredentials;
import com.mastfrog.netty.http.client.ResponseFuture;
import com.mastfrog.webapi.Callback;
import com.mastfrog.webapi.builtin.Parameters;
import com.timboudreau.trackerclient.pojos.Acknowledgement;
import com.timboudreau.trackerclient.pojos.Event;
import com.timboudreau.trackerclient.pojos.FieldID;
import com.timboudreau.trackerclient.pojos.SeriesID;
import com.timboudreau.trackerclient.pojos.Totals;
import com.timboudreau.trackerclient.pojos.User;
import com.timboudreau.trackerclient.pojos.UserID;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

/**
 * Represents a login session for a user with the time tracker api
 *
 * @author Tim Boudreau
 */
public class TrackerSession {

    private final TrackerClientSPI spi;
    private final String userName;
    private final String password;
    private final User user;
    private final ObjectMapper mapper;

    TrackerSession(TrackerClientSPI spi, String userName, String password, User user, ObjectMapper mapper) {
        this.spi = spi;
        this.userName = userName;
        this.password = password;
        this.user = user;
        this.mapper = mapper;
    }

    public User getUser() {
        return user;
    }

    public UserID getUserID() {
        return new UserID(userName);
    }

    public BasicCredentials getCredentials() {
        return new BasicCredentials(userName, password);
    }

    public <T extends Callback<Acknowledgement>> ResponseFuture setPassword(String password, T h) throws Exception {
        return spi.call(TrackerAPI.SET_PASSWORD, h, getUserID(), getCredentials(), password);
    }

    public <T extends Callback<Acknowledgement>> ResponseFuture delete(SeriesID ser, EventQuery query, T h) throws Exception {
        return spi.call(TrackerAPI.DELETE_TIME, h, ser, query, getUserID(), getCredentials());
    }

    public <T extends Callback<Acknowledgement>> ResponseFuture deleteField(SeriesID ser, FieldID field, EventQuery query, T h) throws Exception {
        return spi.call(TrackerAPI.DELETE_FIELD, h, query, ser, field, getUserID(), getCredentials());
    }

    public <T extends Callback<Acknowledgement>> ResponseFuture updateTimes(SeriesID ser, FieldID field, EventQuery query, Object properties, T h) throws Exception {
        String props = mapper.writeValueAsString(properties);
        return spi.call(TrackerAPI.UPDATE_TIMES, h, query, ser, field, getUserID(), getCredentials(), props);
    }

    public <T extends Callback<Totals>> ResponseFuture getTotals(SeriesID ser, EventQuery query, T handler) throws Exception {
        return spi.call(TrackerAPI.TOTAL_TIMES, handler, query, ser, getUserID(), getCredentials());
    }

    public <T extends Callback<Event[]>> ResponseFuture getEvents(SeriesID ser, EventQuery query, T handler) throws Exception {
        return spi.call(TrackerAPI.GET_TIMES, handler, query, ser, getUserID(), getCredentials());
    }

    public <T extends Callback<Event>> ResponseFuture addEvent(Duration duration, DateTime end, T handler, SeriesID ser, Parameters params) throws Exception {
        return addEvent(new Interval(duration, end), handler, ser, params);
    }

    public <T extends Callback<Event>> ResponseFuture addEvent(DateTime start, Duration duration, T handler, SeriesID ser, Parameters params) throws Exception {
        return addEvent(new Interval(start, duration), handler, ser, params);
    }

    public <T extends Callback<Event>> ResponseFuture addEvent(Interval interval, T handler, SeriesID ser, Parameters params) throws Exception {
        return spi.call(TrackerAPI.ADD_TIME, handler, getUserID(), getCredentials(), ser, interval, params);
    }

    public <T extends Callback<SeriesID[]>> ResponseFuture getSeries(T handler) throws Exception {
        return spi.call(TrackerAPI.LIST_SERIES, handler, getUserID(), getCredentials());
    }

    public <T extends LiveSessionListener> ResponseFuture liveSession(final Parameters params, final SeriesID ser, final T listener) throws Exception {
        LiveSessionImpl ls = new LiveSessionImpl(listener);
        final ResponseFuture f = spi.call(TrackerAPI.LIVE_SESSION, ls, new DummyCallback(), params, ser, getUserID(), getCredentials());
        return f;
    }

    private static class DummyCallback extends Callback<Void> {

        DummyCallback() {
            super(Void.class);
        }

        @Override
        public void success(Void object) {
            System.out.println("dc success");
        }

        @Override
        public void fail(HttpResponseStatus status, ByteBuf bytes) {
            System.out.println("dc fail " + status + " - " + bytes.readableBytes() + " bytes");
            byte[] b = new byte[bytes.readableBytes()];
            if (b.length > 0) {
                bytes.readBytes(b);
                System.out.println(new String(b));
            }
        }
    }
}
