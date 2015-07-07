package com.punchthrough.bean.sdk;

import android.os.Looper;

/**
 * This package contains helper classes to facilitate testing
 */

public class TestingUtils {
    public static class LooperRunner implements Runnable {

        private Looper looper = BeanManager.getInstance().mHandler.getLooper();

        public void run() {
            this.looper.prepare();
            this.looper.loop();
        }

        public void quit() {
            this.looper.quitSafely();
        }
    }
}
