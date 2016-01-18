package de.in4matiker.ledvisualizer;

/**
 * Created by zerghase on 16.12.15.
 */
public class ConnectionEvent {
    private String host;
    private boolean success;

    public ConnectionEvent(String host, boolean success) {
        this.host = host;
        this.success = success;
    }

    public String getHost() {
        return host;
    }

    public boolean isSuccess() {
        return success;
    }
}
