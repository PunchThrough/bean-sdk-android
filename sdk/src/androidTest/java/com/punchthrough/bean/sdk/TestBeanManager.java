package com.punchthrough.bean.sdk;

import android.util.Log;

import com.punchthrough.bean.sdk.util.BeanTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestBeanManager extends BeanTestCase {

    private static final String TAG = "TestBeanManager";

    CountDownLatch latch;

    BeanDiscoveryListener bdl = new BeanDiscoveryListener() {
        @Override
        public void onBeanDiscovered(Bean bean, int rssi) {
            Log.i(TAG, "Bean Discovered");
        }

        @Override
        public void onDiscoveryComplete() {
            latch.countDown();
        }
    };

    public void testScanning() throws InterruptedException {
        latch = new CountDownLatch(1);
        BeanManager.getInstance().setScanTimeout(5);
        BeanManager.getInstance().startDiscovery(bdl);
        BeanManager.getInstance().startDiscovery(bdl);
        latch.await(10, TimeUnit.SECONDS);
        if (latch.getCount() > 0) {
            BeanManager.getInstance().cancelDiscovery();
            fail("Scan didn't finish as expected");
        }
    }

    public void testCancelWithoutScan() {
        BeanManager.getInstance().cancelDiscovery();
    }

    public void testCancelRemoveTimeoutCallback() throws InterruptedException {
        latch = new CountDownLatch(1);
        BeanManager.getInstance().setScanTimeout(10);
        BeanManager.getInstance().startDiscovery(bdl);
        BeanManager.getInstance().cancelDiscovery();
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) {
            BeanManager.getInstance().cancelDiscovery();
            fail("Cancel didn't work");
        }
    }

}
