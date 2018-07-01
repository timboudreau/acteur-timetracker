package com.timboudreau.trackerclient;

import com.mastfrog.netty.http.client.State;
import com.mastfrog.util.thread.Receiver;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Logs wire traffic
 *
 * @author Tim Boudreau
 */
class LogReceiver extends Receiver<State<?>> {

    private final TrackerAPI call;

    public LogReceiver(TrackerAPI call) {
        this.call = call;
    }

    @Override
    public void receive(State<?> state) {
        if (state.get() instanceof FullHttpResponse) {
            FullHttpResponse r = (FullHttpResponse) state.get();
            if (r.status().code() > 399) {
                ByteBuf b = r.content();
                if (b.isReadable() && b.readableBytes() > 0) {
                    byte[] bb = new byte[b.readableBytes()];
                    String s = new String(bb);
                    System.out.println(s);
                    b.resetReaderIndex();
                }
            }
        }
    }
}
