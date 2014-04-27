package com.timboudreau.trackerclient;

import org.joda.time.DateTime;

/**
 * Represents a running session that counts time as long as the connection is
 * open.
 *
 * @author Tim Boudreau
 */
public interface LiveSession {

    void end();

    DateTime getRemoteStartTime();

    boolean isRunning();

    String getRemoteID();
}
