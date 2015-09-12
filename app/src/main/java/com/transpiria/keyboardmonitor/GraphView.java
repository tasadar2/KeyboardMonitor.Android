package com.transpiria.keyboardmonitor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

public class GraphView
        extends SurfaceView
        implements SurfaceHolder.Callback {

    private SurfaceHolder Holder;
    private int mHeight;
    private int mWidth;
    private int mHistorySeconds = 8;
    private int mUpdatesPerSecond = 20;
    private int mHistoryUpdates = mHistorySeconds * mUpdatesPerSecond;
    private int HistoryIndex;
    private int Value;
    private int[] ValueHistory;

    public int mColor;

    public int getColor() {
        return mColor;
    }

    public void setColor(int value) {
        mColor = value;
    }

    public GraphView(Context context) {
        super(context);

        SetupHolder();
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);

        SetupHolder();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.graphView);
        int s = a.getColor(R.styleable.graphView_graphColor, 0);
        this.setColor(s);
        a.recycle();

        this.postDelayed(Draw, 0);
    }

    public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
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

    public void SetValues(int value) {
        Value = value;
    }

    private Runnable Draw = new Runnable() {
        @Override
        public void run() {
            postDelayed(Draw, 50);

            Canvas canvas = Holder.lockCanvas();
            if (canvas != null) {
                try {
                    synchronized (Holder) {
                        Clear(canvas);
                        DrawBars(canvas);
                    }
                } finally {
                    Holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    };

    private void Clear(Canvas canvas) {
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
    }

    private void DrawBars(Canvas canvas) {
        if (ValueHistory != null) {
            try {
                HistoryIndex++;
                if (HistoryIndex >= ValueHistory.length) {
                    HistoryIndex = 0;
                }
                ValueHistory[HistoryIndex] = Value;

                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                paint.setColor(getColor());

                int[] history = ValueHistory;
                int head = HistoryIndex;
                int index = head;
                float lastValue = -1;
                float graphIndex = mWidth;
                float graphInterval = (float)mWidth / mHistoryUpdates;
                do {
                    float value = history[index];
                    if (lastValue != -1) {
                        canvas.drawLine(graphIndex + graphInterval, mHeight - lastValue / 60 * mHeight, graphIndex, mHeight - value / 60 * mHeight, paint);
                    }
                    lastValue = value;

                    graphIndex -= graphInterval;
                    index--;
                    if (index < 0) {
                        index = history.length - 1;
                    }
                } while (index != head);
            } catch (Exception ex) {
                ex.printStackTrace();
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

        int[] oldHistory = ValueHistory;
        ValueHistory = new int[mHistoryUpdates];

        if (oldHistory != null) {
            int oldHead = HistoryIndex;
            int oldIndex = oldHead;

            int newHead = ValueHistory.length - 1;
            int newIndex = newHead;

            do {
                ValueHistory[newIndex] = oldHistory[oldIndex];

                oldIndex--;
                if (oldIndex < 0) {
                    oldIndex = oldHistory.length - 1;
                }
                newIndex--;
            } while (oldIndex != oldHead && newIndex != newHead);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

}
