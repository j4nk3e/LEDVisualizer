package de.in4matiker.ledvisualizer;

import android.app.Activity;
import android.graphics.Color;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;

public class MobileLEDActivity extends Activity implements TextWatcher, SeekBar.OnSeekBarChangeListener, DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MobileLEDActivity.class.getSimpleName();
    private ProgressBar barHi;
    private ProgressBar barLo;
    private SeekBar frequencySplit;
    private SeekBar frequencyTop;
    private SeekBar frequencyLo;
    private SeekBar smoothBar;
    private EditText editText;
    private Visualizer visualizer;

    private TextView lowCutText;
    private TextView highCutText;
    private TextView splitText;
    private TextView smoothText;

    private DatagramSocket socket;
    private InetAddress address;

    private int bufferLength = 40;
    private double[] lowBuffer = new double[bufferLength];
    private double[] highBuffer = new double[bufferLength];
    private int bufferPosition = 0;
    private double highDivisor = 1;
    private double lowDivisor = 1;
    private byte[] packetData = new byte[6];
    private float blockSize;
    private int splitIndex;
    private int highCutoffIndex;
    private int lowCutoffIndex;
    private GoogleApiClient googleApiClient;
    private Collection<String> nodes;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        editText = (EditText) findViewById(R.id.address);
        lowCutText = (TextView) findViewById(R.id.lo_text);
        highCutText = (TextView) findViewById(R.id.hi_text);
        splitText = (TextView) findViewById(R.id.split_text);
        smoothText = (TextView) findViewById(R.id.smooth_text);
        smoothBar = (SeekBar) findViewById(R.id.smooth_bar);
        barHi = (ProgressBar) findViewById(R.id.progress_hi);
        barLo = (ProgressBar) findViewById(R.id.progress_lo);
        frequencySplit = (SeekBar) findViewById(R.id.frequency_split);
        frequencyLo = (SeekBar) findViewById(R.id.frequency_lo);
        frequencyTop = (SeekBar) findViewById(R.id.frequency_top);
        editText.addTextChangedListener(this);
        frequencySplit.setOnSeekBarChangeListener(this);
        frequencyLo.setOnSeekBarChangeListener(this);
        frequencyTop.setOnSeekBarChangeListener(this);
        smoothBar.setOnSeekBarChangeListener(this);
        visualizer = new Visualizer(0);
        visualizer.setScalingMode(Visualizer.SCALING_MODE_AS_PLAYED);
        visualizer.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS);
        visualizer.setCaptureSize(1024);
        blockSize = visualizer.getSamplingRate() / (float) visualizer.getCaptureSize() / 1000f;
        updateFrequency(frequencyLo, frequencyLo.getProgress());
        updateFrequency(frequencyTop, frequencyTop.getProgress());
        updateFrequency(frequencySplit, frequencySplit.getProgress());
        connect();
        startPolling();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        if (nodes != null && nodes.getNodes() != null) {
            for (Node node : nodes.getNodes()) {
                results.add(node.getId());
                Log.d(TAG, "Node found: " + node.getId());
            }
        }
        return results;
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (null != googleApiClient && googleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                Log.d(TAG, item.getUri().toString());
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                final int smoothing = dataMapItem.getDataMap().getInt("smoothing", 0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setSmoothing(smoothing);
                    }
                });
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        Wearable.DataApi.addListener(googleApiClient, this);
        connected = true;
    }

    private boolean connected;

    @Override
    public void onConnectionSuspended(int i) {
        connected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        connected = false;
    }

    private void initBuffers() {
        lowBuffer = new double[bufferLength];
        highBuffer = new double[bufferLength];
        bufferPosition = 0;
    }

    private void startPolling() {
        new Thread() {
            @Override
            public void run() {
                visualizer.setEnabled(true);
                byte[] fft = new byte[4096];
                while (visualizer.getEnabled()) {
                    visualizer.getFft(fft);

                    double lowMax = 0;
                    double highMax = 0;
                    for (int i = 2; i < fft.length - 2; i += 2) {
                        double rfk = fft[i];
                        double ifk = fft[i + 1];
                        double magnitude = Math.sqrt(rfk * rfk + ifk * ifk);
                        if (magnitude > 0) {
                            if (i * 2 < splitIndex && i * 2 > lowCutoffIndex) {
                                lowMax += Math.log10(magnitude);
                            } else if (i * 2 < highCutoffIndex) {
                                highMax += Math.log10(magnitude);
                            }
                        }
                    }
                    lowBuffer[bufferPosition] = lowMax;
                    highBuffer[bufferPosition] = highMax;
                    bufferPosition = (bufferPosition + 1) % bufferLength;
                    double lowAvg = avg(lowBuffer);
                    double highAvg = avg(highBuffer);
                    if (lowAvg < 0) {
                        lowAvg = 0;
                    }
                    if (highAvg < 0) {
                        highAvg = 0;
                    }
                    lowDivisor *= 0.9999;
                    highDivisor *= 0.9999;
                    if (lowAvg > lowDivisor) {
                        lowDivisor = lowAvg;
                    }
                    if (highAvg > highDivisor) {
                        highDivisor = highAvg;
                    }
                    lowAvg /= lowDivisor;
                    highAvg /= highDivisor;
                    setProgressBars(lowAvg, highAvg);
                    if (socket != null) {
                        if (lowAvg == 0 && highAvg == 0) {
                            if (pulseinc) {
                                pulse += 0.0001;
                                if (pulse > 0.3) {
                                    pulseinc = false;
                                }
                            } else {
                                pulse -= 0.0001;
                                if (pulse < 0) {
                                    pulseinc = true;
                                    pulse = 0;
                                }
                            }
                            splitColor(pulse, packetData, 3);
                            splitColor(pulse, packetData, 0);
                        } else {
                            pulse = 0;
                            splitColor(lowAvg, packetData, 3);
                            splitColor(highAvg, packetData, 0);
                        }
                        try {
                            socket.send(new DatagramPacket(packetData, 6, address, 12345));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }.start();
    }

    private boolean pulseinc = true;
    private float pulse = 0;

    byte[] data = new byte[2];
    Thread nodeThread;

    private void setProgressBars(final double lo, final double hi) {
        if (connected && nodes != null) {
            data[0] = (byte) (lo * 100);
            data[1] = (byte) (hi * 100);

            for (String node : nodes) {
                Wearable.MessageApi.sendMessage(googleApiClient, node, "/led/data", data);
            }
        } else if (connected) {
            if (nodeThread == null) {
                nodeThread = new Thread() {
                    @Override
                    public void run() {
                        nodes = getNodes();
                    }
                };
                nodeThread.start();
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                barLo.setProgress((int) (lo * 128));
                barHi.setProgress((int) (hi * 128));
            }
        });
    }

    private void connect() {
        try {
            address = InetAddress.getByName(editText.getText().toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    editText.setBackgroundColor(Color.DKGRAY);
                }
            });
        } catch (UnknownHostException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    editText.setBackgroundColor(Color.BLACK);
                }
            });
        }
        if (address != null) {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } else if (socket != null) {
            socket.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        visualizer.setEnabled(false);
        if (socket != null && address != null) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        socket.send(new DatagramPacket(new byte[6], 6, address, 12345));
                        socket.close();
                    } catch (IOException e) {
                    }
                }
            }.start();
        }
    }

    private static final double max = 127;

    private static void splitColor(double value, byte[] data, int start) {
        double r = 0;
        double g = 0;
        double b = 0;
        if (value < 0.33) {
            b = value * 3;
        } else if (value < 0.66) {
            g = (value - 0.33) * 3;
            b = 1 - (value - 0.33) * 3;
        } else {
            r = (value - 0.66) * 3;
            g = 1 - (value - 0.66) * 3;
        }
        data[start] = (byte) (clamp(r, 0, 1) * max);
        data[start + 1] = (byte) (clamp(g, 0, 1) * max);
        data[start + 2] = (byte) (clamp(b, 0, 1) * max);
    }

    private static final double clamp(double value, int min, int max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    private static double avg(double[] values) {
        double sum = 0;
        for (double i : values) {
            sum += i;
        }
        return sum / values.length;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void afterTextChanged(final Editable editable) {
        connect();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        smoothText.setText(String.valueOf(progress + 1));
    }

    private void setSmoothing(int smoothing) {
        bufferLength = smoothing + 1;
        smoothBar.setProgress(smoothing);
        smoothText.setText(String.valueOf(smoothing + 1));
        initBuffers();
    }

    private void updateFrequency(SeekBar seekBar, int progress) {
        int frequency = (int) (progress * blockSize);
        String freqText;
        if (frequency > 10000) {
            freqText = (frequency / 1000) + "kHz";
        } else {
            freqText = frequency + "Hz";
        }
        if (seekBar == frequencySplit) {
            splitIndex = progress;
            splitText.setText(freqText);
        } else if (seekBar == frequencyTop) {
            highCutoffIndex = progress;
            highCutText.setText(freqText);
        } else if (seekBar == frequencyLo) {
            lowCutoffIndex = progress;
            lowCutText.setText(freqText);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar == smoothBar) {
            setSmoothing(seekBar.getProgress());
            PutDataMapRequest dataMap = PutDataMapRequest.create("/led/smoothing");
            dataMap.getDataMap().putInt("smoothing", seekBar.getProgress());
            PutDataRequest request = dataMap.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, request);
        } else {
            updateFrequency(seekBar, seekBar.getProgress());
        }
    }
}
