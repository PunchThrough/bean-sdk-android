package com.punchthrough.bean.sdk.util;

import android.content.Context;
import android.os.Looper;
import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
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

    private final BeanListener beanConnectionListener = new BeanListener() {
        @Override
        public void onConnected() {
            connectLatch.countDown();
        }

        @Override
        public void onConnectionFailed() {
            fail("Connection failed");
        }

        @Override
        public void onDisconnected() {
            disconnectLatch.countDown();
        }

        @Override
        public void onSerialMessageReceived(byte[] data) {}

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {}

        @Override
        public void onError(BeanError error) {
            fail(error.toString());
        }
    };

    protected void ensureConnected(Bean bean, CountDownLatch connectLatch) throws InterruptedException {
        /**
         * This method assumes that `connectLatch` is getting counted down by
         * the .onConnected() callback in a BeanListener.
         */

        connectLatch.await(20, TimeUnit.SECONDS);
        assertThat(bean.isConnected()).isTrue();
    }

    protected void ensureDisconnected(Bean bean, CountDownLatch disconnectLatch) throws InterruptedException {
        /**
         * This method assumes that `disconnectLatch` is getting counted down by
         * the .onDisconnected() callback in a BeanListener.
         */

        disconnectLatch.await(20, TimeUnit.SECONDS);
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

    protected Bean discoverBean(String name) throws Exception {
        final CountDownLatch beanLatch = new CountDownLatch(1);
        final List<Bean> beans = new ArrayList<>();

        final String targetName = name;

        BeanDiscoveryListener listener = new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean bean, int rssi) {
                if (bean.getDevice().getName() != null && bean.getDevice().getName().equals(targetName)) {
                    System.out.println("[BeanUtils] Found Bean by name: " + targetName);
                    beans.add(bean);
                    beanLatch.countDown();
                }
            }

            @Override
            public void onDiscoveryComplete() {
                System.out.println("[BeanUtils] Discovery Complete!");
                beanLatch.countDown();
            }
        };

        boolean startedOK = BeanManager.getInstance().startDiscovery(listener);
        assertThat(startedOK).isTrue();
        beanLatch.await(60, TimeUnit.SECONDS);
        if (beans.isEmpty()) {
            throw new Exception("No bean named: " + name);
        }
        return beans.get(0);
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

    protected static List<String> filesInAssetDir(Context context, String dirName) throws IOException {

        String[] filePathArray = context.getAssets().list(dirName);
        return Arrays.asList(filePathArray);

    }

}
