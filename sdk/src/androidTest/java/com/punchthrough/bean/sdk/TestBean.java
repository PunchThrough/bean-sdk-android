package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.TestingUtils.LooperRunner;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.message.ScratchData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Bean.
 * <p/>
 * Prerequisites:
 * - Bean within range
 * - Android device connected over USB
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

    private List<Bean> getBeans(int num) throws InterruptedException {
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

    private Bean getBeanByName(String name) throws InterruptedException {
        final CountDownLatch beanLatch = new CountDownLatch(1);
        final List<Bean> beans = new ArrayList<>();

        final String targetName = name;

        BeanDiscoveryListener listener = new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean bean, int rssi) {
                if (bean.getDevice().getName().equals(targetName)) {
                    System.out.println("Found Bean!!!!!!!");
                    beans.add(bean);
                    beanLatch.countDown();
                }
            }

            @Override
            public void onDiscoveryComplete() {
                System.out.println("Nothing");
                beanLatch.countDown();
            }
        };

        boolean startedOK = BeanManager.getInstance().startDiscovery(listener);
        assertThat(startedOK).isTrue();
        beanLatch.await(60, TimeUnit.SECONDS);
        if (beans.isEmpty()) {
            fail("Couldn't find bean by name: " + name);
        }
        return beans.get(0);
    }

    public void testBeanDeviceInfo() throws InterruptedException {
        /** Read device information from a bean
         *
         * Warning: This test requires a nearby bean named "TESTBEAN"
         */

        Bean bean = this.getBeanByName("TESTBEAN");
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

    public void testBeanReadWriteScratchBank() throws InterruptedException {
        /** Test Scratch characteristic functionality
         *
         * Warning: This test requires a nearby bean named "TESTBEAN"
         */
        Bean bean = this.getBeanByName("TESTBEAN");
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

        bean.connect(getContext(), beanListener);
        connectionLatch.await();
        assertThat(testState.get("connected")).isEqualTo(true);

        // write to BANK_1 and BANK_5

        bean.setScratchData(ScratchBank.BANK_1, new byte[]{11, 12, 13});
        bean.setScratchData(ScratchBank.BANK_5, new byte[]{51, 52, 53});

        // read BANK_1

        final CountDownLatch scratch1Latch = new CountDownLatch(1);
        bean.readScratchData(ScratchBank.BANK_1, new Callback<ScratchData>() {
            @Override
            public void onResult(ScratchData result) {
                testState.put("scratch1", result);
                scratch1Latch.countDown();
            }
        });

        scratch1Latch.await();

        ScratchData scratch1 = (ScratchData)testState.get("scratch1");
        assertThat(scratch1.number()).isEqualTo(1);
        assertThat(scratch1.data()[0]).isEqualTo((byte)11);
        assertThat(scratch1.data()[1]).isEqualTo((byte)12);
        assertThat(scratch1.data()[2]).isEqualTo((byte)13);

        // read BANK_5

        final CountDownLatch scratch5Latch = new CountDownLatch(1);
        bean.readScratchData(ScratchBank.BANK_5, new Callback<ScratchData>() {
            @Override
            public void onResult(ScratchData result) {
                testState.put("scratch5", result);
                scratch5Latch.countDown();
            }
        });

        scratch5Latch.await();

        ScratchData scratch5 = (ScratchData)testState.get("scratch5");
        assertThat(scratch5.number()).isEqualTo(5);
        assertThat(scratch5.data()[0]).isEqualTo((byte)51);
        assertThat(scratch5.data()[1]).isEqualTo((byte)52);
        assertThat(scratch5.data()[2]).isEqualTo((byte)53);

        if (bean.isConnected()) {
            bean.disconnect();
        }
    }

    @Suppress
    public void testConnectMultipleBeansWithSameListener() throws InterruptedException {
        /* This test requires at least 3 beans nearby to pass */

        final List<Bean> beans = this.getBeans(3);
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
