package com.punchthrough.bean.sdk.util;

import android.content.Context;
import android.os.Looper;
import android.os.Parcel;
import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.BuildConfig;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.LedColor;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.message.SketchMetadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class BeanTestCase extends AndroidTestCase {

    public Bean testBean;

    // beanName and beanAddress are Gradle properties
    public final String beanName = BuildConfig.BEAN_NAME;
    public final String beanAddress = BuildConfig.BEAN_ADDRESS;

    private LooperRunner lr = new LooperRunner(BeanManager.getInstance().getHandler().getLooper());

    private final CountDownLatch connectLatch = new CountDownLatch(1);
    private final CountDownLatch disconnectLatch = new CountDownLatch(1);

    private class LooperRunner extends Thread {

        private Looper looper;

        public LooperRunner(Looper looper) {
            this.looper = looper;
        }

        public void run() {
            Looper.prepare();
            Looper.loop();
        }

    }

    protected void setUp() {
        lr.start();
    }

    protected void tearDown() {}

    protected void setUpTestBean() {
        try {
            testBean = discoverBean();
            synchronousConnect(testBean);
        } catch(Exception e) {
            fail("Error connecting to " + beanName + " bean in setup.");
        }
    }

    protected void tearDownTestBean() {
        try {
            super.tearDown();
            synchronousDisconnect(testBean);
        } catch(Exception e) {
            fail("Error disconnecting.  This may affect later tests.");
        }
    }

    private final BeanListener beanConnectionListener = new BeanListener() {
        @Override
        public void onConnected() {
            System.out.println("[BeanTest] Connect Event!");
            connectLatch.countDown();
        }

        @Override
        public void onConnectionFailed() {
            System.out.println("[BeanTest] On Connection Failed");
        }

        @Override
        public void onDisconnected() {
            System.out.println("[BeanTest] Disconnect Event");
            disconnectLatch.countDown();
        }

        @Override
        public void onSerialMessageReceived(byte[] data) {}

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {}

        @Override
        public void onError(BeanError error) {
            System.out.println("On Error: " + error.toString());
            fail(error.toString());
        }

        @Override
        public void onReadRemoteRssi(int rssi) {

        }
    };

    protected void ensureConnected(Bean bean, CountDownLatch connectLatch) throws InterruptedException {
        /**
         * This method assumes that `connectLatch` is getting counted down by
         * the .onConnected() callback in a BeanListener.
         */

        connectLatch.await(100, TimeUnit.SECONDS);
        assertThat(bean.isConnected()).isTrue();
        System.out.println("Connected to Bean: " + bean.describe());
    }

    protected void ensureDisconnected(Bean bean, CountDownLatch disconnectLatch) throws InterruptedException {
        /**
         * This method assumes that `disconnectLatch` is getting counted down by
         * the .onDisconnected() callback in a BeanListener.
         */

        disconnectLatch.await(60, TimeUnit.SECONDS);
        assertThat(bean.isConnected()).isFalse();
    }

    protected void synchronousConnect(Bean bean) throws InterruptedException {
        /**
         * Synchronously connect to a Bean
         */

        if (!bean.isConnected()) {
            bean.connect(getContext(), beanConnectionListener);
            ensureConnected(bean, connectLatch);
        }
    }

    protected void synchronousDisconnect(Bean bean) throws InterruptedException {
        /**
         * Synchronously disconnect from a Bean
         */

        if (bean.isConnected()) {
            bean.disconnect();
            ensureDisconnected(bean, disconnectLatch);
        }
    }

    protected List<Bean> discoverBeans(int num) throws InterruptedException {
        final CountDownLatch beanLatch = new CountDownLatch(num);
        final List<Bean> beans = new ArrayList<>();

        BeanDiscoveryListener listener = new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean bean, int rssi) {
                beans.add(bean);
                beanLatch.countDown();
            }

            @Override
            public void onDiscoveryComplete() {
                beanLatch.countDown();
            }
        };

        boolean startedOK = BeanManager.getInstance().startDiscovery(listener);
        assertThat(startedOK).isTrue();
        beanLatch.await(60, TimeUnit.SECONDS);
        assertThat(beans.size()).isEqualTo(num);
        return beans;
    }

    protected Bean discoverBean() throws Exception {
        /**
         * Discover a Bean that will be used for instrumentation testing
         *
         * The Bean selected will be selected based on the following criteria:
         *   - The Bean name matches the one set in gradle.properties
         *   - The closest Bean based on highest RSSI
         *
         */

        final CountDownLatch beanLatch = new CountDownLatch(1);
        final List<Bean> beans = new ArrayList<>();

        BeanDiscoveryListener listener = new BeanDiscoveryListener() {

            int highestRssi = -120;

            @Override
            public void onBeanDiscovered(Bean bean, int rssi) {
                String msg = String.format("Found Bean: %s (RSSI: %s) (%s)",
                        bean.getDevice().getName(),
                        rssi,
                        bean.getDevice().getAddress());
                System.out.println(msg);

                if (beanName.equals(bean.getDevice().getName())) {
                    beans.add(bean);
                    beanLatch.countDown();
                } else if (beanAddress.equals(bean.getDevice().getAddress())) {
                    beans.add(bean);
                    beanLatch.countDown();
                }
                else if (rssi > highestRssi) {
                    highestRssi = rssi;
                    beans.add(bean);

                    if (rssi >= -50) {
                        // This Bean is very close, lets quit early to speed up the test
                        beanLatch.countDown();
                    }
                }
            }

            @Override
            public void onDiscoveryComplete() {
                System.out.println("Discovery Complete");
                beanLatch.countDown();
            }
        };

        System.out.println(String.format("Scanning for Bean (%s) or nearest Bean.", beanName));
        boolean startedOK = BeanManager.getInstance().startDiscovery(listener);
        assertThat(startedOK).isTrue();
        beanLatch.await(30, TimeUnit.SECONDS);
        if (beans.isEmpty()) {
            throw new Exception("No beans found");
        }
        Bean bean = beans.get(beans.size() - 1);
        System.out.println("Closest Bean: " + bean.describe());
        BeanManager.getInstance().cancelDiscovery();
        return bean;
    }

    protected DeviceInfo getDeviceInformation(Bean bean) throws Exception {
        final CountDownLatch deviceInfoLatch = new CountDownLatch(1);
        final List<DeviceInfo> deviceInfos = new ArrayList<>();

        bean.readDeviceInfo(new Callback<DeviceInfo>() {
            @Override
            public void onResult(DeviceInfo deviceInfo) {
                assertThat(deviceInfo).isNotNull();
                deviceInfos.add(deviceInfo);
                deviceInfoLatch.countDown();
            }
        });

        deviceInfoLatch.await(20, TimeUnit.SECONDS);
        if (deviceInfos.isEmpty()) {
            throw new Exception("Couldn't get device info");
        }
        return deviceInfos.get(0);
    }

    protected SketchMetadata getSketchMetadata(Bean bean) throws Exception {
        final CountDownLatch metadataLatch = new CountDownLatch(1);
        final List<SketchMetadata> metadatas = new ArrayList<>();

        bean.readSketchMetadata(new Callback<SketchMetadata>() {
            @Override
            public void onResult(SketchMetadata metadata) {
                assertThat(metadata).isNotNull();
                metadatas.add(metadata);
                metadataLatch.countDown();
            }
        });

        metadataLatch.await(20, TimeUnit.SECONDS);
        if (metadatas.isEmpty()) {
            throw new Exception("Couldn't get Sketch Meta Data");
        }
        return metadatas.get(0);
    }

    protected static List<String> filesInAssetDir(Context context, String dirName) {

        String[] filePathArray = new String[0];
        try {
            filePathArray = context.getAssets().list(dirName);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return Arrays.asList(filePathArray);

    }

    protected void blinkBeanThrice(Bean bean) {
        LedColor blue = LedColor.create(0, 0, 255);
        LedColor off = LedColor.create(0, 0, 0);
        int sleep = 500;

        try {
            bean.setLed(blue);
            Thread.sleep(sleep);
            bean.setLed(off);
            Thread.sleep(sleep);

            bean.setLed(blue);
            Thread.sleep(sleep);
            bean.setLed(off);
            Thread.sleep(sleep);

            bean.setLed(blue);
            Thread.sleep(sleep);
            bean.setLed(off);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
