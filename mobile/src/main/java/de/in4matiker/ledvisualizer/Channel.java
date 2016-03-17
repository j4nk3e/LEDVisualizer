package de.in4matiker.ledvisualizer;

/**
 * Created by zerghase on 16.12.15.
 */
public class Channel {
    private final String topic;
    private float r;
    private float g;
    private float b;
    private boolean unset;

    public Channel(String topic) {
        this.topic = topic;
        unset = true;
    }

    private static float clamp(float value) {
        if (value < 0) {
            return 0;
        } else if (value > 1) {
            return 1;
        }
        return value;
    }


    public void setColor(float r, float g, float b) {
        unset = false;
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
    }

    public String getData() {
        if (unset) {
            return "";
        }
        return r + "," + g + "," + b;
    }

    public String getTopic() {
        return topic;
    }
}
