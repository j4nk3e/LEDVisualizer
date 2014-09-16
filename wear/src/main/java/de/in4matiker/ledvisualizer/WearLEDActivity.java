package de.in4matiker.ledvisualizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


public class WearLEDActivity extends Activity implements DataApi.DataListener, MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = WearLEDActivity.class.getSimpleName();
    private TextView smoothText;
    private SeekBar smoothBar;
    private BarSurface surfaceLeft, surfaceRight;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                smoothText = (TextView) stub.findViewById(R.id.smooth_text);
                smoothBar = (SeekBar) stub.findViewById(R.id.smooth_bar);
                smoothBar.setOnSeekBarChangeListener(WearLEDActivity.this);
                surfaceLeft = (BarSurface) stub.findViewById(R.id.surfaceLeft);
                surfaceRight = (BarSurface) stub.findViewById(R.id.surfaceRight);
            }
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, messageEvent.getPath());
        if (messageEvent.getPath().equals("/led/data")) {
            surfaceLeft.setValue(messageEvent.getData()[0]);
            surfaceRight.setValue(messageEvent.getData()[1]);
            surfaceLeft.postInvalidate();
            surfaceRight.postInvalidate();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        smoothText.setText(String.valueOf(progress + 1));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar == smoothBar) {
            setSmoothing(seekBar.getProgress());
            PutDataMapRequest dataMap = PutDataMapRequest.create("/led/config");
            dataMap.getDataMap().putInt("smoothing", seekBar.getProgress());
            PutDataRequest request = dataMap.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, request);
        }
    }

    private void setSmoothing(int smoothing) {
        smoothText.setText(String.valueOf(smoothing + 1));
        smoothBar.setProgress(smoothing);
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
            Wearable.MessageApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
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
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
