package com.timboudreau.trackerclient;

import com.mastfrog.netty.http.client.HttpRequestBuilder;
import com.mastfrog.webapi.Decorator;
import com.mastfrog.webapi.WebCall;
import java.io.IOException;

/**
 *
 * @author tim
 */
public class EventQueryDecorator implements Decorator<EventQuery> {

    @Override
    public void decorate(WebCall call, HttpRequestBuilder builder, EventQuery obj, Class<EventQuery> type) throws IOException {
        obj.appendTo(builder);
    }
    
}
