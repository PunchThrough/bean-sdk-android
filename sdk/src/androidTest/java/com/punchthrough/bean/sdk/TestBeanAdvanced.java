package com.punchthrough.bean.sdk;

import android.test.suitebuilder.annotation.Suppress;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.util.BeanTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are labeled "Advanced" because they have stricter setup/requirements to pass.
 * They are also all @Suppress'd by default so that the instrumentation tests can pass more
 * frequently. If you would like to run these tests you must manually/temporarily remove
 * the @Suppress annotation.
 *
 */
public class TestBeanAdvanced extends BeanTestCase {

    private final byte START_FRAME = 0x77;

    private void triggerBeanScratchChange(Bean bean) {
        byte[] msg = {START_FRAME, 0x01};
        bean.sendSerialMessage(msg);
    }

    private void triggerBeanSerialMessage(Bean bean) {
        byte[] msg = {START_FRAME, 0x00};
        bean.sendSerialMessage(msg);
    }

    @Suppress
    public void testBeanListenerCallbacks() throws Exception {
        /**
         * This tests all of the "Happy Path" BeanListener callbacks
         *  - onConnected
         *  - onSerialMessageReceived (TODO: Broken, test when fixed)
         *  - onScratchValueChanged
         *  - onDisconnected
         *
         * This test does not test failure callbacks:
         *  - onConnectionFailed
         *  - onError
         *
         * Note: This test requires a Bean named TESTBEAN with a particular sketch loaded. The
         * Sketch needed can be found in sdk/src/androidTest/assets/bean_fw_advanced_callbacks.
         */

        final Bean bean = discoverBean("TESTBEAN");

        // TODO: The latch should have a value of 4 when all callbacks are operational
        final CountDownLatch testCompletionLatch = new CountDownLatch(3);

        BeanListener beanListener = new BeanListener() {
            @Override
            public void onConnected() {
                testCompletionLatch.countDown();
                triggerBeanScratchChange(bean);
                triggerBeanSerialMessage(bean);
            }

            @Override
            public void onConnectionFailed() {
                fail("Connection failed!");
            }

            @Override
            public void onDisconnected() {
                System.out.println("Disconnected");
                testCompletionLatch.countDown();
            }

            @Override
            public void onSerialMessageReceived(byte[] data) {
                // TODO: Broken, never called!
                testCompletionLatch.countDown();
                if (testCompletionLatch.getCount() == 1) {
                    System.out.println("Disconnecting");
                    bean.disconnect();
                }
            }

            @Override
            public void onScratchValueChanged(ScratchBank bank, byte[] value) {
                testCompletionLatch.countDown();
                if (testCompletionLatch.getCount() == 1) {
                    System.out.println("Disconnecting");
                    bean.disconnect();
                }
            }

            @Override
            public void onError(BeanError error) {
                fail(error.toString());
            }
        };

        bean.connect(getContext(), beanListener);
        testCompletionLatch.await(60, TimeUnit.SECONDS);
        if (testCompletionLatch.getCount() != 0) {
            fail("Not all callbacks fired");
        }
    }

    @Suppress
    public void testConnectMultipleBeansWithSameListener() throws InterruptedException {
        /* This test requires at least 3 beans nearby to pass */

        final List<Bean> beans = discoverBeans(3);
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
