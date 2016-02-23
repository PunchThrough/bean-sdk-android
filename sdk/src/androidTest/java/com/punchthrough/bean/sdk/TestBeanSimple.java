package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.message.ScratchData;
import com.punchthrough.bean.sdk.util.BeanTestCase;

import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Bean.
 *
 * Prerequisites:
 * - Bean within range named TESTBEAN
 * - Android device connected over USB
 */
public class TestBeanSimple extends BeanTestCase {

    public void testBeanDeviceInfo() throws Exception {
        /** Read device information from a bean
         *
         * Warning: This test requires a nearby bean named "TESTBEAN"
         */

        Bean bean = discoverBean("TESTBEAN");
        synchronousConnect(bean);

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

        // Always disconnect at end of test so that other tests will pass
        synchronousDisconnect(bean);
    }

    public void testBeanReadWriteScratchBank() throws Exception {
        /** Test Scratch characteristic functionality
         *
         * Warning: This test requires a nearby bean named "TESTBEAN"
         */
        Bean bean = discoverBean("TESTBEAN");
        synchronousConnect(bean);

        // write to BANK_1 and BANK_5
        bean.setScratchData(ScratchBank.BANK_1, new byte[]{11, 12, 13});
        bean.setScratchData(ScratchBank.BANK_5, new byte[]{51, 52, 53});

        // read BANK_1
        final CountDownLatch scratch1Latch = new CountDownLatch(1);
        bean.readScratchData(ScratchBank.BANK_1, new Callback<ScratchData>() {
            @Override
            public void onResult(ScratchData result) {
                assertThat(result.number()).isEqualTo(1);
                assertThat(result.data()[0]).isEqualTo((byte) 11);
                assertThat(result.data()[1]).isEqualTo((byte)12);
                assertThat(result.data()[2]).isEqualTo((byte) 13);
                scratch1Latch.countDown();
            }
        });

        scratch1Latch.await();

        // read BANK_5
        final CountDownLatch scratch5Latch = new CountDownLatch(1);
        bean.readScratchData(ScratchBank.BANK_5, new Callback<ScratchData>() {
            @Override
            public void onResult(ScratchData result) {
                assertThat(result.number()).isEqualTo(5);
                assertThat(result.data()[0]).isEqualTo((byte) 51);
                assertThat(result.data()[1]).isEqualTo((byte) 52);
                assertThat(result.data()[2]).isEqualTo((byte) 53);
                scratch5Latch.countDown();
            }
        });

        scratch5Latch.await();

        // Always disconnect at end of test so that other tests will pass
        synchronousDisconnect(bean);
    }

}
