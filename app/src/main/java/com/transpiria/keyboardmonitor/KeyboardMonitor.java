package com.transpiria.keyboardmonitor;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class KeyboardMonitor extends Activity
        implements
        com.transpiria.keyboardmonitor.StatisticsService.ISubscriptionsChanged {

    private Timer SlowUI;
    private StatisticsService Stats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.monitor);

        ProcessorBars processors = (ProcessorBars) findViewById(R.id.processors);
        processors.setZOrderOnTop(true);

        SlowUI = new Timer();

        Stats = new StatisticsService();
        Stats.SubscriptionsChangedEvent.AddObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Stats.Subscribe();
        ResumeUI();
    }

    private void ResumeUI() {
        SlowUI.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UpdateUISlow();
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Stats.UnSubscribe();
        SlowUI.purge();
    }

    public void UpdateUISlow() {
        try {
            Date now = new Date();
            TextView time = (TextView) findViewById(R.id.time);
            time.setText(new SimpleDateFormat("h':'mm':'ss", Locale.US).format(now));

            TextView date = (TextView) findViewById(R.id.date);
            date.setText(new SimpleDateFormat("E MMM dd", Locale.US).format(now));

            TextView down = (TextView) findViewById(R.id.down);
            down.setText("Down: ");

            TextView up = (TextView) findViewById(R.id.up);
            up.setText("  Up: ");

            ProcessorBars processors = (ProcessorBars) findViewById(R.id.processors);

            if (Stats.Current != null) {
//                TextView test = (TextView) findViewById(R.id.text1);
//                test.setText("Processor: " + String.valueOf(Stats.Current.Processor.Value) + "\r\n");
//                for (double value : Stats.Current.Processor.Values) {
//                    test.append("\t" + String.valueOf(value) + "\r\n");
//                }

                down.append(FormatBytesPerSecond(Stats.Current.BytesReceived.Value));
                up.append(FormatBytesPerSecond(Stats.Current.BytesSent.Value));

                processors.SetValues(Stats.Current.Processor.Values);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String FormatBytesPerSecond(double value) {
        if (value < 1152) {
            return String.valueOf(Math.ceil(value)) + " B";
        } else {
            DecimalFormat df = new DecimalFormat("##.00");
            value = value / 1024;
            if (value < 896) {
                return df.format((double) Math.round(value * 100) / 100) + " KB";
            } else {
                value = value / 1024;
                if (value < 896) {
                    return df.format((double) Math.round(value * 100) / 100) + " MB";
                } else {
                    value = value / 1024;
                    return df.format((double) Math.round(value * 100) / 100) + " GB";
                }
            }
        }
    }

    @Override
    public void SubscriptionsChanged() {
        if (!Stats.isSubscribed() && !Stats.Subscriptions.isEmpty()) {
            Stats.Subscribe(Stats.Subscriptions.get(0), false);
        }
    }
}


