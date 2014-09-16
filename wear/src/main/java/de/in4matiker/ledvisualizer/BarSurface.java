package de.in4matiker.ledvisualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by zerghase on 10.08.14.
 */
public class BarSurface extends SurfaceView implements SurfaceHolder.Callback {
    private int value;
    private Paint paint;
    private Rect bar, background;

    public BarSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        bar = new Rect();
        background = new Rect();
        paint = new Paint();
        paint.setAntiAlias(false);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        background.set(0, 0, width, height);
        bar.set(0, height * value / 100, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void setValue(int value) {
        this.value = value;
        bar.top = background.bottom * value / 100;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        canvas.drawRect(background, paint);
        paint.setColor(getColor());
        canvas.drawRect(bar, paint);
    }

    public int getColor() {
        int r = 0, g = 0, b = 0, color;
        if (value < 33) {
            b = value * 3;
        } else if (value < 66) {
            g = (value - 33) * 3;
            b = 100 - (value - 33) * 3;
        } else {
            r = (value - 66) * 3;
            g = 100 - (value - 66) * 3;
        }
        color = clamp(r, 0, 100) << 16;
        color += clamp(g, 0, 100) << 8;
        color += clamp(b, 0, 100);
        return color;
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }
}
