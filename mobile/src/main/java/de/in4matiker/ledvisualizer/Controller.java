package de.in4matiker.ledvisualizer;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Controller {
    private static final int CHANNEL_COUNT = 2;
    private static final int CHANNEL_LENGTH = 3;
    private final Channel[] channels;
    private final MqttAsyncClient client;

    public Controller(String topic, MqttAsyncClient client) {
        this.client = client;
        channels = new Channel[CHANNEL_COUNT];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new Channel(topic + "/" + (i + 1));
        }
    }

    public Channel getChannel(int index) {
        return channels[index];
    }

    public void read() {
        for (Channel channel : channels) {
            try {
                client.subscribe(channel.getTopic(), 2);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        for (Channel channel : channels) {
            try {
                String data = channel.getData();
                if (data.isEmpty()) {
                    continue;
                }
                client.publish(channel.getTopic(), data.getBytes(), 0, true);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
}
