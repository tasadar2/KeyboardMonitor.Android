package com.transpiria.keyboardmonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

public class ProcessorBars
        extends SurfaceView
        implements SurfaceHolder.Callback {

    private SurfaceHolder Holder;
    private int mHeight;
    private int mWidth;
    private List<Double> Values;

    public ProcessorBars(Context context) {
        super(context);

        SetupHolder();
    }

    public ProcessorBars(Context context, AttributeSet attrs) {
        super(context, attrs);

        SetupHolder();
    }

    public ProcessorBars(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        SetupHolder();
    }

    private void SetupHolder() {
        if (Holder == null) {
            Holder = getHolder();
            Holder.addCallback(this);
            Holder.setFormat(PixelFormat.TRANSPARENT);
        }
    }

    public void SetValues(List<Double> values) {
        Values = values;
        Draw();
    }

    private void Draw() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Canvas canvas = Holder.lockCanvas();
                try {
                    synchronized (Holder) {
                        Clear(canvas);
                        DrawBars(canvas);
                    }
                } finally {
                    Holder.unlockCanvasAndPost(canvas);
                }
            }
        }).start();
    }

    private void Clear(Canvas canvas) {
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
    }

    private void DrawBars(Canvas canvas) {
        if (Values != null) {
            float barSize = (float) mWidth / Values.size();

            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);

            for (int index = 0; index < Values.size(); index++) {
                canvas.drawRect(
                        barSize * index + 3,
                        Math.min(mHeight - ((float) (mHeight * (Values.get(index) / 100))), mHeight - 1),
                        barSize * (index + 1) - 6,
                        mHeight,
                        paint);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mWidth = width;
        mHeight = height;
        Draw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

}
