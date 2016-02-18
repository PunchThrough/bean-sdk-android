package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.util.BeanTestCase;
import com.punchthrough.bean.sdk.util.TestingUtils.BeanUtils;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Bean.
 *
 * Prerequisites:
 * - Bean within range named TESTBEAN
 * - Android device connected over USB
 */
public class TestBeanSimple extends BeanTestCase {


    private void ensureDisconnected(Bean bean) {
        /**
         * Synchronously disconnect from a Bean
         *
         * This method assumes that `disconnectLatch` is getting counted down by
         * the .onDisconnected() callback in a BeanListener.
         */
    }

    public void testBeanDeviceInfo() throws Exception {
        /** Read device information from a bean
         *
         * Warning: This test requires a nearby bean named "TESTBEAN"
         */

        Bean bean = BeanUtils.getBeanByName("TESTBEAN");
        final CountDownLatch connectLatch = new CountDownLatch(1);
        final CountDownLatch disconnectLatch = new CountDownLatch(1);

        BeanListener beanListener = new BeanListener() {
            @Override
            public void onConnected() {
                connectLatch.countDown();
            }

            @Override
            public void onConnectionFailed() {
                fail("Connection failed!");
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

        // Make sure connection successful
        bean.connect(getContext(), beanListener);
        connectLatch.await(20, TimeUnit.SECONDS);
        assertThat(bean.isConnected()).isTrue();

        // Get Device Information
        final CountDownLatch deviceInfoLatch = new CountDownLatch(1);
        bean.readDeviceInfo(new Callback<DeviceInfo>() {
            @Override
            public void onResult(DeviceInfo deviceInfo) {
                assertThat(deviceInfo).isNotNull();
                deviceInfoLatch.countDown();
            }
        });
        deviceInfoLatch.await();

        if (bean.isConnected()) {
            bean.disconnect();
        }
    }

//    public void testBeanReadWriteScratchBank() throws Exception {
//        /** Test Scratch characteristic functionality
//         *
//         * Warning: This test requires a nearby bean named "TESTBEAN"
//         */
//        Bean bean = BeanUtils.getBeanByName("TESTBEAN");
//        final CountDownLatch connectLatch = new CountDownLatch(1);
//        final CountDownLatch disconnectLatch = new CountDownLatch(1);
//        final HashMap testState = new HashMap();
//        testState.put("connected", false);
//
//        BeanListener beanListener = new BeanListener() {
//            @Override
//            public void onConnected() {
//                testState.put("connected", true);
//                connectLatch.countDown();
//
//            }
//
//            @Override
//            public void onConnectionFailed() {
//                fail("Connection failed!");
//            }
//
//            @Override
//            public void onDisconnected() {
//                System.out.println("Disconnected");
//            }
//
//            @Override
//            public void onSerialMessageReceived(byte[] data) {
//            }
//
//            @Override
//            public void onScratchValueChanged(ScratchBank bank, byte[] value) {
//            }
//
//            @Override
//            public void onError(BeanError error) {
//                fail(error.toString());
//            }
//        };
//
//        bean.connect(getContext(), beanListener);
//        connectLatch.await();
//        assertThat(testState.get("connected")).isEqualTo(true);
//
//        // write to BANK_1 and BANK_5
//
//        bean.setScratchData(ScratchBank.BANK_1, new byte[]{11, 12, 13});
//        bean.setScratchData(ScratchBank.BANK_5, new byte[]{51, 52, 53});
//
//        // read BANK_1
//
//        final CountDownLatch scratch1Latch = new CountDownLatch(1);
//        bean.readScratchData(ScratchBank.BANK_1, new Callback<ScratchData>() {
//            @Override
//            public void onResult(ScratchData result) {
//                testState.put("scratch1", result);
//                scratch1Latch.countDown();
//            }
//        });
//
//        scratch1Latch.await();
//
//        ScratchData scratch1 = (ScratchData)testState.get("scratch1");
//        assertThat(scratch1.number()).isEqualTo(1);
//        assertThat(scratch1.data()[0]).isEqualTo((byte)11);
//        assertThat(scratch1.data()[1]).isEqualTo((byte)12);
//        assertThat(scratch1.data()[2]).isEqualTo((byte)13);
//
//        // read BANK_5
//
//        final CountDownLatch scratch5Latch = new CountDownLatch(1);
//        bean.readScratchData(ScratchBank.BANK_5, new Callback<ScratchData>() {
//            @Override
//            public void onResult(ScratchData result) {
//                testState.put("scratch5", result);
//                scratch5Latch.countDown();
//            }
//        });
//
//        scratch5Latch.await();
//
//        ScratchData scratch5 = (ScratchData)testState.get("scratch5");
//        assertThat(scratch5.number()).isEqualTo(5);
//        assertThat(scratch5.data()[0]).isEqualTo((byte)51);
//        assertThat(scratch5.data()[1]).isEqualTo((byte)52);
//        assertThat(scratch5.data()[2]).isEqualTo((byte)53);
//
//        if (bean.isConnected()) {
//            bean.disconnect();
//        }
//    }

}
