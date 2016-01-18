package de.in4matiker.ledvisualizer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;

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
        barController = new Controller("192.168.178.21");
        couchController = new Controller("192.168.178.58");
        bars = new SeekBar[]{r1, g1, b1, r2, g2, b2, r3, g3, b3, r4, g4, b4};
        int i = 0;
        for (SeekBar bar : bars) {
            bar.setTag(i++);
            bar.setOnSeekBarChangeListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barController.close();
        couchController.close();
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
                bars[offset + index * 3].getProgress() / 127f,
                bars[offset + index * 3 + 1].getProgress() / 127f,
                bars[offset + index * 3 + 2].getProgress() / 127f
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
