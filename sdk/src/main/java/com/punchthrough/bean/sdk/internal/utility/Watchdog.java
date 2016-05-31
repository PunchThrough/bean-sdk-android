package com.punchthrough.bean.sdk.internal.utility;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.punchthrough.bean.sdk.internal.upload.firmware.OADProfile;

public class Watchdog {

    private final String TAG = "Watchdog";
    private final int TICK_INTERVAL = 1000;
    private final long WATCHDOG_FINISH = 3600000;  // 1 hour in milliseconds

    private Handler handler;
    private CountDownTimer timer;
    private long lastPoke = 0;
    private boolean paused = false;

    public Watchdog(Handler handler) {
        this.handler = handler;
    }

    private long uptimeSeconds() {
        return SystemClock.uptimeMillis() / 1000;
    }

    private void recordPoke() {
        lastPoke = uptimeSeconds();
    }

    public void start(final int timeoutSeconds, final WatchdogListener listener) {
        Log.i(TAG, "Starting watchdog with timeout seconds: " + timeoutSeconds);

        handler.post(new Runnable() {
            @Override
            public void run() {
                timer = new CountDownTimer(WATCHDOG_FINISH, TICK_INTERVAL) {

                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (uptimeSeconds() - lastPoke > timeoutSeconds) {
                            if (paused) {
                                Log.w(TAG, "Watchdog expired, but the timer is currently paused!");
                            } else {
                                Log.e(TAG, "Watchdog expired!");
                                listener.expired();
                            }
                        }
                    }

                    @Override
                    public void onFinish() {
                        Log.i(TAG, "Watchdog finished");
                    }

                };
                timer.start();
            }
        });

        recordPoke();
    }

    public void pause() {
        Log.i(TAG, "Watchdog Paused");
        paused = true;
    }

    public void stop() {
        if (timer != null) {
            Log.i(TAG, "Watchdog has been stopped");
            timer.cancel();
            timer = null;
        }
    }

    public void poke() {
        recordPoke();

        // Un-pause the timer by poking it
        if (paused) {
            Log.i(TAG, "Watchdog resumed...");
            paused = false;
        }
    }

    /**
     * Interface to alert clients of watchdog events
     *
     */
    public interface WatchdogListener {

        /**
         * The watchdog has expired
         *
         * A watchdog expires when the client does not call .poke() for the number of
         * seconds defined by the argument in the .start(timeoutSeconds, listener) method.
         */
        public void expired();
    }

}
