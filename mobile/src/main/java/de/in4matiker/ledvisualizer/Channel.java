package de.in4matiker.ledvisualizer;

/**
 * Created by zerghase on 16.12.15.
 */
public class Channel {
    private static final double max = 127;
    private final int index;
    private float r = 1;
    private float g = 1;
    private float b = 1;

    public Channel(int index) {
        this.index = index;
    }

    private static double clamp(double value) {
        if (value < 0) {
            return 0;
        } else if (value > 1) {
            return 1;
        }
        return value;
    }


    public void setColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void setBytes(byte[] data) {
        data[index * 3] = (byte) (clamp(r) * max);
        data[index * 3 + 1] = (byte) (clamp(g) * max);
        data[index * 3 + 2] = (byte) (clamp(b) * max);
    }
}
