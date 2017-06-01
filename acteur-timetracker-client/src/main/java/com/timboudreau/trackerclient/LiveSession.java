package com.timboudreau.trackerclient;

import java.time.ZonedDateTime;


/**
 * Represents a running session that counts time as long as the connection is
 * open.
 *
 * @author Tim Boudreau
 */
public interface LiveSession {

    void end();

    ZonedDateTime getRemoteStartTime();

    boolean isRunning();

    String getRemoteID();
}
