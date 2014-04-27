package com.timboudreau.trackerclient;

/**
 *
 * @author Tim Boudreau
 */
public abstract class LiveSessionListener {

    public void onError(Throwable thrown) {
        thrown.printStackTrace();
    }
    
    public void onClose() {
        System.err.println("LSL on close");
    }

    public void onRequestCompleted() {
        System.err.println("LSL on request completed");
    }
    
    public void onFail(String reason) {
        
    }
    
    public void set(LiveSession session) {
        
    }
}
