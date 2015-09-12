package com.transpiria.keyboardmonitor;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;

public class KeyboardMonitor extends Activity
        implements
        StatisticsService.ISubscriptionsChanged,
        StatisticsService.IStateChanged {

    private StatisticsService Stats;
    private Handler MainHandler = new Handler();

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

        Stats = new StatisticsService();
        Stats.SubscriptionsChangedEvent.AddObserver(this);
        Stats.StateChangedEvent.AddObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Stats.SetActive();
        MainHandler.postDelayed(UpdateSlowUI, 0);
        MainHandler.postDelayed(UpdateRealtimeUI, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!isFinishing()) {
            MainHandler.postDelayed(Stats.CheckIdle, 1000);
        }

        Stats.SetInactive();
        MainHandler.removeCallbacks(UpdateSlowUI);
        MainHandler.removeCallbacks(UpdateRealtimeUI);
    }

    private Runnable UpdateSlowUI = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(UpdateSlowUIUIThread);
        }
    };

    private Runnable UpdateRealtimeUI = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(UpdateRealtimeUIUIThread);
        }
    };

    private Runnable UpdateSlowUIUIThread = new Runnable() {
        @Override
        public void run() {
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

                TextView cpu = (TextView) findViewById(R.id.cpu);
                cpu.setText("Cpu: ");
                ProcessorBars processors = (ProcessorBars) findViewById(R.id.processors);

                if (Stats.Current != null) {
//                TextView test = (TextView) findViewById(R.id.text1);
//                test.setText("Processor: " + String.valueOf(Stats.Current.Processor.Value) + "\r\n");
//                for (double value : Stats.Current.Processor.Values) {
//                    test.append("\t" + String.valueOf(value) + "\r\n");
//                }
                    DecimalFormat df = new DecimalFormat("#");

                    down.append(FormatBytesPerSecond(Stats.Current.BytesReceived.Value));
                    up.append(FormatBytesPerSecond(Stats.Current.BytesSent.Value));

                    cpu.append(df.format(Stats.Current.Processor.Value) + " %");
                    processors.SetValues(Stats.Current.Processor.Values);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            MainHandler.postDelayed(UpdateSlowUI, 1000);
        }
    };

    private Runnable UpdateRealtimeUIUIThread = new Runnable() {
        @Override
        public void run() {
            TextView fps = (TextView) findViewById(R.id.fps);
            fps.setText(String.valueOf(Stats.FramesPerSecond));

            MainHandler.postDelayed(UpdateRealtimeUI, 333);
        }
    };

    private String FormatBytesPerSecond(double value) {
        if (value < 1152) {
            DecimalFormat df = new DecimalFormat("#");
            return df.format(Math.ceil(value)) + " B";
        } else {
            DecimalFormat df = new DecimalFormat("#.00");
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

    @Override
    public void StateChanged(StatisticsService.State state) {
        //if im not on the selection screen, then start idle timer

        if (state == StatisticsService.State.Idle) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Window window = getWindow();
                        window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                        long l = SystemClock.uptimeMillis();
                        PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Endpoint Discovered");
                        wl.acquire();
                        wl.release();

                        Window window = getWindow();
                        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }
}


