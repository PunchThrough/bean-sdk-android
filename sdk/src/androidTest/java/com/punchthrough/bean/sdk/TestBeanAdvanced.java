package com.punchthrough.bean.sdk;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.util.TestingUtils;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * These tests are labeled "Advanced" because they have stricter setup/requirements to pass.
 * They are also all @Suppress'd by default so that the instrumentation tests can pass more
 * frequently. If you would like to run these tests you must manually/temporarily remove
 * the @Suppress annotation.
 *
 */
public class TestBeanAdvanced extends AndroidTestCase {

    @Suppress
    public void testConnectMultipleBeansWithSameListener() throws InterruptedException {
        /* This test requires at least 3 beans nearby to pass */

        final List<Bean> beans = TestingUtils.BeanUtils.getBeans(3);
        final Bean beanA = beans.get(0);
        final Bean beanB = beans.get(1);
        final Bean beanC = beans.get(2);
        final CountDownLatch connectionLatch = new CountDownLatch(2);
        final HashMap<String, Boolean> connectionState = new HashMap<>();
        connectionState.put("bean_a_connected", false);
        connectionState.put("bean_b_connected", false);

        BeanListener beanListener = new BeanListener() {

            @Override
            public void onConnected() {
                if (connectionState.get("bean_a_connected") == false) {
                    if (beanA.isConnected()) {
                        connectionState.put("bean_a_connected", true);
                        connectionLatch.countDown();
                    }
                }

                if (connectionState.get("bean_b_connected") == false) {
                    if (beanB.isConnected()) {
                        connectionState.put("bean_b_connected", true);
                        connectionLatch.countDown();
                    }
                }

                if (beanC.isConnected()) {
                    fail("Bean C not suppose to connect!");
                }
            }

            @Override
            public void onConnectionFailed() {
                fail("Connection failed!");
            }

            @Override
            public void onDisconnected() {
            }

            @Override
            public void onSerialMessageReceived(byte[] data) {
            }

            @Override
            public void onScratchValueChanged(ScratchBank bank, byte[] value) {
            }

            @Override
            public void onError(BeanError error) {
                fail(error.toString());
            }
        };

        beanA.connect(getContext(), beanListener);
        beanB.connect(getContext(), beanListener);
        connectionLatch.await(60, TimeUnit.SECONDS);
        // No need to assert anything, implicit success based on connection latch
    }

}
