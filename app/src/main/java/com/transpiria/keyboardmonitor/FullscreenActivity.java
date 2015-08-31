package com.transpiria.keyboardmonitor;

import com.transpiria.keyboardmonitor.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.net.SocketFactory;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity
        implements
        com.transpiria.keyboardmonitor.Communicator.IEndpointDiscovered,
        com.transpiria.keyboardmonitor.Communicator.IMessageReceived {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private Communicator Communicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        Communicator = new Communicator();
        Communicator.EndpointDiscoveredEvent.AddObserver(this);
        Communicator.IMessageReceivedEvent.AddObserver(this);
        Communicator.Discover(27831);

//        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
//                            if (mControlsHeight == 0) {
//                                mControlsHeight = controlsView.getHeight();
//                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
//                            controlsView.animate()
//                                    .translationY(visible ? 0 : mControlsHeight)
//                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
//                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Subscribe();
    }

    @Override
    protected void onPause() {
        super.onPause();

        UnSubscribe();
    }

    private boolean Subscribed = false;
    private InetAddress SubscribedAddress;
    private int SubscribedPort;

    @Override
    public void EndpointDiscovered(InetAddress ipAddress, int port) {
        SubscribedAddress = ipAddress;
        SubscribedPort = port;
        Subscribe(true);
    }

    private void Subscribe() {
        Subscribe(false);
    }

    private void Subscribe(boolean force) {
        if (Communicator != null && SubscribedAddress != null && (!Subscribed || force)) {
            Subscribed = true;
            Communicator.Subscribe(SubscribedAddress, SubscribedPort);
        }
    }

    private void UnSubscribe() {
        if (Communicator != null && SubscribedAddress != null && Subscribed) {
            Subscribed = false;
            Communicator.Unsubscribe(SubscribedAddress, SubscribedPort);
        }
    }

    @Override
    public void MessageReceived(byte[] content) {
        try {
            final Info info = new Info();
            String jsonText = new String(content, "UTF-8");
            JSONObject json = new JSONObject(jsonText);

            info.Processor = GetCounter(json.getJSONObject("Processors"));
            info.BytesReceived = GetCounter(json.getJSONObject("BytesReceived"));
            info.BytesSent = GetCounter(json.getJSONObject("BytesSent"));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView test = (TextView) findViewById(R.id.text1);
                    test.setText("Processor: " + String.valueOf(info.Processor.Value) + "\r\n");
                    for (double value : info.Processor.Values) {
                        test.append("\t" + String.valueOf(value) + "\r\n");
                    }
                    test.append("\r\n");
                    test.append("BytesReceived: " + String.valueOf(info.BytesReceived.Value) + "\r\n");
                    test.append("\r\n");
                    test.append("BytesSent: " + String.valueOf(info.BytesSent.Value) + "\r\n");
                    test.append("\r\n");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Counter GetCounter(JSONObject json) {
        Counter counter = new Counter();
        try {
            counter.Name = json.getString("Name");
            counter.Value = json.getDouble("Value");
            JSONArray values = json.getJSONArray("Values");
            for (int index = 0; index < values.length(); index++) {
                counter.Values.add(values.getDouble(index));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return counter;
    }

    public class Info {
        public Counter Processor;
        public Counter BytesReceived;
        public Counter BytesSent;
    }

    public class Counter {
        public String Name;
        public double Value;
        public List<Double> Values = new ArrayList<>();
    }
}
