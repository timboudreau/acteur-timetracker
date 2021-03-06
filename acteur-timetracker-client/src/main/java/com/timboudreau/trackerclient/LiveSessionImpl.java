package com.timboudreau.trackerclient;

import com.mastfrog.netty.http.client.State;
import static com.mastfrog.netty.http.client.StateType.Closed;
import static com.mastfrog.netty.http.client.StateType.Connected;
import static com.mastfrog.netty.http.client.StateType.Error;
import static com.mastfrog.netty.http.client.StateType.HeadersReceived;
import com.mastfrog.util.thread.Receiver;
import com.mastfrog.util.time.TimeUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponse;
import java.time.ZonedDateTime;

/**
 *
 * @author tim
 */
class LiveSessionImpl extends Receiver<State<?>> implements LiveSession {

    private volatile ZonedDateTime remoteStart;
    private volatile boolean closed;
    private volatile boolean started;
    private volatile Channel channel;
    private volatile String trackerId;
    private final LiveSessionListener listener;

    LiveSessionImpl(final LiveSessionListener listener) {
        this.listener = listener;
    }

    @Override
    public String toString() {
        return "LiveSessionImpl{" + "remoteStart=" + remoteStart + ", closed=" + closed + ", started=" + started + ", channel=" + channel + ", listener=" + listener + '}';
    }

    public String getRemoteID() {
        return trackerId;
    }

    @Override
    public void receive(State<?> state) {
        switch (state.stateType()) {
            case Closed:
                closed = true;
                channel = null;
                listener.onClose();
                break;
            case Connected:
                State.Connected cnn = (State.Connected) state;
                channel = cnn.get();
                break;
            case Error:
                State.Error err = (State.Error) state;
                listener.onError(err.get());
                break;
            case HeadersReceived:
                State.HeadersReceived hr = (State.HeadersReceived) state;
                HttpResponse resp = hr.get();
                listener.set(this);
                if (resp.status().code() > 399) {
                    listener.onFail(resp.status() + "");
                } else {
                    started = true;
                    if (resp != null) {
                        String dt = resp.headers().get("X-Remote-Start");
                        if (dt != null) {
                            remoteStart = TimeUtil.fromUnixTimestamp(Long.parseLong(dt));
                        }
                        trackerId = resp.headers().get("X-Tracker-ID");
                    }
                    listener.onRequestCompleted();
                }
        }
    }

    @Override
    public void end() {
        Channel ch = this.channel;
        if (ch != null && (ch.isActive() || ch.isOpen())) {
            ch.close();
            closed = true;
        }
    }

    @Override
    public ZonedDateTime getRemoteStartTime() {
        return remoteStart == null ? ZonedDateTime.now() : remoteStart;
    }

    @Override
    public boolean isRunning() {
        return channel != null && started && !closed;
    }

}
