package com.punchthrough.bean.sdk;

import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.LedColor;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.message.SketchMetadata;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.SketchHex;
import com.punchthrough.bean.sdk.util.BeanTestCase;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;
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

    private static final String TAG = "TestBeanAdvanced";

    private final byte START_FRAME = 0x77;

    private void triggerBeanScratchChange(Bean bean) {
        byte[] msg = {START_FRAME, 0x01};
        bean.sendSerialMessage(msg);
    }

    private void triggerBeanSerialMessage(Bean bean) {
        byte[] msg = {START_FRAME, 0x00};
        bean.sendSerialMessage(msg);
    }

    private void triggerReadRemoteRssi(Bean bean) {
        bean.readRemoteRssi();
    }

    @Suppress
    public void testBeanSketchUpload() throws Exception {
        final Bean bean = discoverBean();
        synchronousConnect(bean);
        String hwVersion = getDeviceInformation(bean).hardwareVersion();

        String hexPath = null;
        for (String filename : filesInAssetDir(getContext(), "bean_fw_advanced_callbacks")) {
            if (FilenameUtils.getExtension(filename).equals("hex")) {
                String[] pieces = FilenameUtils.getBaseName(filename).split("_");
                String hexHW = pieces[pieces.length - 1];
                if (hexHW.equals(hwVersion)) {
                    hexPath = FilenameUtils.concat("bean_fw_advanced_callbacks", filename);
                    break;
                }
            }
        }

        assertThat(hexPath).isNotNull();
        InputStream imageStream  = getContext().getAssets().open(hexPath);
        StringWriter writer = new StringWriter();
        IOUtils.copy(imageStream, writer);

        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        SketchHex sketchHex = SketchHex.create(timestamp, writer.toString());

        final CountDownLatch sketchLatch = new CountDownLatch(1);
        Callback<UploadProgress> onProgress = new Callback<UploadProgress>() {
            @Override
            public void onResult(UploadProgress result) {
                System.out.println("On Result: " + result);
            }
        };

        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                System.out.println("all done!");
                sketchLatch.countDown();
            }
        };

        bean.programWithSketch(sketchHex, onProgress, onComplete);
        sketchLatch.await(120, TimeUnit.SECONDS);

        SketchMetadata metadata = getSketchMetadata(bean);
        if (!metadata.hexName().equals(timestamp)) {
            fail(String.format("Unexpected Sketch name: %s != %s", metadata.hexName(), timestamp));
        }
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
         * Note: This test requires a Bean with a particular sketch loaded. The
         * Sketch needed can be found in sdk/src/androidTest/assets/bean_fw_advanced_callbacks.
         */

        final Bean bean = discoverBean();

        // TODO: The latch should have a value of 5 when all callbacks are operational
        final CountDownLatch testCompletionLatch = new CountDownLatch(4);

        BeanListener beanListener = new BeanListener() {

            private void disconnect() {
                if (testCompletionLatch.getCount() == 1) {
                    Log.i(TAG, "Disconnecting!");
                    bean.disconnect();
                }
            }

            @Override
            public void onConnected() {
                Log.i(TAG, "SUCCESS: Connected");
                testCompletionLatch.countDown();
                triggerBeanScratchChange(bean);
                triggerBeanSerialMessage(bean);
                triggerReadRemoteRssi(bean);
            }

            @Override
            public void onConnectionFailed() {
                Log.e(TAG, "FAILURE: Connection failed");
                fail("Connection failed!");
            }

            @Override
            public void onDisconnected() {
                Log.i(TAG, "SUCCESS: Disconnected");
                testCompletionLatch.countDown();
            }

            @Override
            public void onSerialMessageReceived(byte[] data) {
                Log.i(TAG, "SUCCESS: Serial Message Received!");

                // TODO: Broken, never called!
                testCompletionLatch.countDown();
                disconnect();
            }

            @Override
            public void onScratchValueChanged(ScratchBank bank, byte[] value) {
                Log.i(TAG, "SUCCESS: Scratch Value Changed!");
                testCompletionLatch.countDown();
                disconnect();
            }

            @Override
            public void onError(BeanError error) {
                Log.e(TAG, "FAILURE: Bean error " + error.toString());
                fail(error.toString());
            }

            @Override
            public void onReadRemoteRssi(final int rssi) {
                Log.i(TAG, "SUCCESS: Read remote RSSI");
                testCompletionLatch.countDown();
                disconnect();
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

            @Override
            public void onReadRemoteRssi(final int rssi) {
                System.out.println("onReadRemoteRssi: " + rssi);
            }
        };

        beanA.connect(getContext(), beanListener);
        beanB.connect(getContext(), beanListener);
        connectionLatch.await(60, TimeUnit.SECONDS);
        // No need to assert anything, implicit success based on connection latch
    }

    @Suppress
    public void testFastSerialMessages() throws Exception {
        int times = 100;
        final CountDownLatch testCompletionLatch = new CountDownLatch(times);
        Bean bean = discoverBean();
        synchronousConnect(bean);
        for (int i = 0; i < times; i ++) {
            bean.readLed(new Callback<LedColor>() {
                @Override
                public void onResult(LedColor result) {
                    testCompletionLatch.countDown();
                }
            });
        }
        testCompletionLatch.await(120, TimeUnit.SECONDS);
        synchronousDisconnect(bean);
    }

}