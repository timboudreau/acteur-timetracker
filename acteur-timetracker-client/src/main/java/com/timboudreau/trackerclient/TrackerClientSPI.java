package com.timboudreau.trackerclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mastfrog.acteur.util.BasicCredentials;
import com.mastfrog.guicy.scope.ReentrantScope;
import com.mastfrog.netty.http.client.ResponseFuture;
import com.mastfrog.netty.http.client.State;
import com.mastfrog.util.thread.Receiver;
import com.mastfrog.webapi.Callback;
import com.mastfrog.webapi.Invoker;
import com.timboudreau.trackerclient.pojos.DisplayName;
import com.timboudreau.trackerclient.pojos.User;
import com.timboudreau.trackerclient.pojos.UserID;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;

/**
 * Entry point for the time tracker API - use this to create a login session
 * through which you can use the rest of the API.
 *
 * @author Tim Boudreau
 */
public class TrackerClientSPI {

    final ReentrantScope scope;
    private final ObjectMapper mapper;
    private final Invoker<?> invoker;

    @Inject
    public TrackerClientSPI(ReentrantScope scope, ObjectMapper mapper, Invoker invoker) {
        this.scope = scope;
        this.mapper = mapper;
        this.invoker = invoker;
    }

    public ResponseFuture call(final TrackerAPI apiCall, Receiver<State<?>> r, final Callback<?> resultHandler, final Object... with) throws Exception {
        return invoker.call(apiCall, r, resultHandler, with);
    }

    public ResponseFuture call(final TrackerAPI apiCall, final Callback<?> resultHandler, final Object... with) throws Exception {
        return invoker.call(apiCall, new LogReceiver(apiCall), resultHandler, with);
    }

    public ResponseFuture signup(final String userName, String displayName, final String password, final Callback<TrackerSession> callback) throws IOException, Exception {
        return invoker.call(TrackerAPI.SIGNUP, new LogReceiver(TrackerAPI.SIGNUP), new SessionCreator(callback, userName, password), new DisplayName(displayName), new UserID(userName), password);
    }

    public ResponseFuture signIn(final String userName, final String password, final Callback<TrackerSession> callback) throws Exception {
        return invoker.call(TrackerAPI.WHO_AM_I, new LogReceiver(TrackerAPI.WHO_AM_I), new SessionCreator(callback, userName, password), new BasicCredentials(userName, password));
    }

    public ResponseFuture checkVersion(Callback<String> cb) throws Exception {
        return invoker.call(TrackerAPI.GET_API_VERSION, cb);
    }

    class SessionCreator extends Callback<User> {

        private final Callback<TrackerSession> callback;
        private final String userName;
        private final String password;

        SessionCreator(Callback<TrackerSession> callback, String userName, String password) {
            super(User.class);
            this.callback = callback;
            this.userName = userName;
            this.password = password;
        }

        @Override
        public void success(User user) {
            TrackerSession result = new TrackerSession(TrackerClientSPI.this, userName, password, user, mapper);
            callback.success(result);
        }

        @Override
        public void fail(HttpResponseStatus status, ByteBuf bytes) {
            callback.fail(status, bytes);
        }

        @Override
        public void error(Throwable err) {
            callback.error(err);
        }

        @Override
        public void responseReceived(HttpResponseStatus status, HttpHeaders headers) {
            callback.responseReceived(status, headers);
        }
    }
}
