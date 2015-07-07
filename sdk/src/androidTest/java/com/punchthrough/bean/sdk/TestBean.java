package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.TestingUtils.LooperRunner;
import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Bean.
 * Note: This requires an actual bean nearby to pass!
 */
public class TestBean extends AndroidTestCase {

    private LooperRunner lr = new LooperRunner();
    private Thread lrThread = new Thread(lr);

    protected void setUp() {
        lrThread.start();
    }

    protected void tearDown() throws InterruptedException {
        /*
        * "Speaking very generally, never quit() your looper threads. That method exists
        * mostly for historical and testing reasons. In Real Life™, I recommend that you
        * continue to reuse the same looper thread(s) for the life of the process rather
        * than creating/quitting them."
        * */
//        lr.quit();
//        lrThread.join();
    }

    private Bean getBean() throws InterruptedException {
        final CountDownLatch beanLatch = new CountDownLatch(1);
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
        beanLatch.await();
        assertThat(beans.size()).isGreaterThan(0);
        return beans.get(0);
    }

    public void testBeanDeviceInfo() throws InterruptedException {
        Bean bean = this.getBean();
        final CountDownLatch connectionLatch = new CountDownLatch(1);
        final HashMap testState = new HashMap();
        testState.put("connected", false);

        BeanListener beanListener = new BeanListener() {
            @Override
            public void onConnected() {
                testState.put("connected", true);
                connectionLatch.countDown();
            }

            @Override
            public void onConnectionFailed() {
                connectionLatch.countDown();
            }

            @Override
            public void onDisconnected() {
                connectionLatch.countDown();
            }

            @Override
            public void onSerialMessageReceived(byte[] data) {
                connectionLatch.countDown();
            }

            @Override
            public void onScratchValueChanged(ScratchBank bank, byte[] value) {
                connectionLatch.countDown();
            }

            @Override
            public void onError(BeanError error) {
                connectionLatch.countDown();
            }
        };

        bean.connect(getContext(), beanListener);
        connectionLatch.await();
        assertThat(testState.get("connected")).isEqualTo(true);

        final CountDownLatch deviceInfoLatch = new CountDownLatch(1);
        testState.put("device_info", null);
        bean.readDeviceInfo(new Callback<DeviceInfo>() {
            @Override
            public void onResult(DeviceInfo deviceInfo) {
                testState.put("device_info", deviceInfo);
                deviceInfoLatch.countDown();
            }
        });
        deviceInfoLatch.await();
        assertThat(testState.get("device_info")).isNotNull();

        if (bean.isConnected()) {
            bean.disconnect();
        }
    }
}
