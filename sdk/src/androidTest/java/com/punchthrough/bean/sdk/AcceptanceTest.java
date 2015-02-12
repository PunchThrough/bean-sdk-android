package com.punchthrough.bean.sdk;

import android.test.AndroidTestCase;
import android.util.Log;

import java.util.concurrent.Semaphore;

public class AcceptanceTest extends AndroidTestCase {

    public static final String TAG = "AcceptanceTest";

    private Semaphore testCompletionLock;

    public void testAllFeatures() {
        testCompletionLock = new Semaphore(0);

        Log.d(TAG, "Scanning for test Bean...");
        boolean discoveryStarted = BeanManager.getInstance()
                .startDiscovery(DISCOVERY_LISTENER, 5000);
        assertTrue(discoveryStarted);

        try {
            Log.d(TAG, "Waiting for tests to complete.");
            testCompletionLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Interrupted while waiting for tests to complete");
        }

        Log.d(TAG, "Tests complete.");
    }

    private final BeanDiscoveryListener DISCOVERY_LISTENER = new BeanDiscoveryListener() {
        @Override
        public void onBeanDiscovered(Bean bean) {
            Log.d(TAG, "Found Bean!");
            testCompletionLock.release();
        }

        @Override
        public void onDiscoveryComplete() {
            Log.d(TAG, "Discovery complete. No test Bean found.");
            fail();
        }
    };

}
