package de.in4matiker.ledvisualizer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ControllerActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    private Controller barController;
    private Controller couchController;
    @Bind(R.id.r1)
    SeekBar r1;
    @Bind(R.id.r2)
    SeekBar r2;
    @Bind(R.id.r3)
    SeekBar r3;
    @Bind(R.id.r4)
    SeekBar r4;
    @Bind(R.id.g1)
    SeekBar g1;
    @Bind(R.id.g2)
    SeekBar g2;
    @Bind(R.id.g3)
    SeekBar g3;
    @Bind(R.id.g4)
    SeekBar g4;
    @Bind(R.id.b1)
    SeekBar b1;
    @Bind(R.id.b2)
    SeekBar b2;
    @Bind(R.id.b3)
    SeekBar b3;
    @Bind(R.id.b4)
    SeekBar b4;
    private SeekBar[] bars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller);
        ButterKnife.bind(this);
        try {
            MqttClientPersistence persistence = new MemoryPersistence();
            MqttAsyncClient client = new MqttAsyncClient("tcp://raspberrybar", "android", persistence);
            barController = new Controller("led/bar", client);
            couchController = new Controller("led/couch", client);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Channel[] channels = new Channel[]{
                            barController.getChannel(0),
                            barController.getChannel(1),
                            couchController.getChannel(0),
                            couchController.getChannel(1)
                    };
                    int channelSlider = 0;
                    for (Channel channel : channels) {
                        if (channel.getTopic().equals(topic)) {
                            String data = new String(message.getPayload());
                            String[] values = data.split(",");
                            float[] floats = new float[3];
                            for (int i = 0; i < 3; i++) {
                                floats[i] = Float.valueOf(values[i]);
                                bars[channelSlider * 3 + i].setProgress((int) (bars[channelSlider * 3 + i].getMax() * floats[i]));
                            }
                        }
                        channelSlider += 1;
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            client.connect("context", new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    barController.read();
                    couchController.read();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    finish();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            finish();
        }
        bars = new SeekBar[]{r1, g1, b1, r2, g2, b2, r3, g3, b3, r4, g4, b4};
        int i = 0;
        for (SeekBar bar : bars) {
            bar.setTag(i++);
            bar.setOnSeekBarChangeListener(this);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int tag = (int) seekBar.getTag();
        int offset = 6;
        Controller controller = couchController;
        if (tag < bars.length / 2) {
            offset = 0;
            controller = barController;
        }
        int index = 1;
        if ((tag % (bars.length / 2)) < 3) {
            index = 0;
        }
        controller.getChannel(index).setColor(
                bars[offset + index * 3].getProgress() / 255f,
                bars[offset + index * 3 + 1].getProgress() / 255f,
                bars[offset + index * 3 + 2].getProgress() / 255f
        );
        controller.update();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
