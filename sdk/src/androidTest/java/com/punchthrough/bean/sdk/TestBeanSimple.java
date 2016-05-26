package com.punchthrough.bean.sdk;

import android.util.Log;

import com.punchthrough.bean.sdk.message.BatteryLevel;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.message.ScratchData;
import com.punchthrough.bean.sdk.util.BeanTestCase;

import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Bean.
 *
 * Prerequisites:
 * - Bean within range
 * - Android device connected over USB
 */
public class TestBeanSimple extends BeanTestCase {

    private static final String TAG = "BeanManager";

    public void setUp() {
        super.setUp();
        setUpTestBean();
    }

    public void tearDown() {
        super.tearDown();
        tearDownTestBean();
    }

    private boolean validHardwareVersion(String version) {
        Log.i(TAG, "Validating hardware version: " + version);
        return (
            version.equals("E") ||
            version.startsWith("1") ||
            version.startsWith("2")
        );
    }

    private boolean validFirmwareVersion(String version) {
        Log.i(TAG, "Validating firmware version: " + version);
        return version.length() > 0;
    }

    private boolean validSoftwareVersion(String version) {
        Log.i(TAG, "Validating software version: " + version);
        return version.length() > 0;
    }

    public void testBeanDeviceInfo() throws Exception {
        /** Read device information from a bean
         *
         * Warning: This test requires a nearby Bean
         */
        DeviceInfo info = getDeviceInformation(testBean);

        if (!validHardwareVersion(info.hardwareVersion())) {
            fail("Unexpected HW version: " + info.hardwareVersion());
        }
        if (!validFirmwareVersion(info.firmwareVersion())) {
            fail("Unexpected FW version: " + info.firmwareVersion());
        }
        if (!validSoftwareVersion(info.softwareVersion())) {
            fail("Unexpected SW version: " + info.softwareVersion());
        }

        Log.i(TAG, "TEST PASSED: Read Device Info");
    }

    public void testReadFirmwareVersion() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(1);
        testBean.readFirmwareVersion(new Callback<String>() {
            @Override
            public void onResult(String result) {
                assertThat(validFirmwareVersion(result)).isTrue();
                l.countDown();
                Log.i(TAG, "TEST PASSED: Read Firmware Version");
            }
        });
        l.await(20, TimeUnit.SECONDS);
    }

    public void testReadHardwareVersion() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(1);
        testBean.readHardwareVersion(new Callback<String>() {
            @Override
            public void onResult(String result) {
                assertThat(validHardwareVersion(result)).isTrue();
                l.countDown();
                Log.i(TAG, "TEST PASSED: Read Hardware Version");
            }
        });
        l.await(20, TimeUnit.SECONDS);
    }

    public void testBeanReadWriteScratchBank() throws Exception {
        /** Test Scratch characteristic functionality
         *
         * Warning: This test requires a nearby Bean
         */

        // write to BANK_1 and BANK_5
        testBean.setScratchData(ScratchBank.BANK_1, new byte[]{11, 12, 13});
        testBean.setScratchData(ScratchBank.BANK_5, new byte[]{51, 52, 53});

        // read BANK_1
        final CountDownLatch scratch1Latch = new CountDownLatch(1);
        testBean.readScratchData(ScratchBank.BANK_1, new Callback<ScratchData>() {
            @Override
            public void onResult(ScratchData result) {
                assertThat(result.number()).isEqualTo(1);
                assertThat(result.data()[0]).isEqualTo((byte) 11);
                assertThat(result.data()[1]).isEqualTo((byte)12);
                assertThat(result.data()[2]).isEqualTo((byte) 13);
                scratch1Latch.countDown();
                Log.i(TAG, "TEST PASSED: Scratch Bank 1");
            }
        });

        scratch1Latch.await();

        // read BANK_5
        final CountDownLatch scratch5Latch = new CountDownLatch(1);
        testBean.readScratchData(ScratchBank.BANK_5, new Callback<ScratchData>() {
            @Override
            public void onResult(ScratchData result) {
                assertThat(result.number()).isEqualTo(5);
                assertThat(result.data()[0]).isEqualTo((byte) 51);
                assertThat(result.data()[1]).isEqualTo((byte) 52);
                assertThat(result.data()[2]).isEqualTo((byte) 53);
                scratch5Latch.countDown();
                Log.i(TAG, "TEST PASSED: Scratch Bank 5");
            }
        });

        scratch5Latch.await();
    }

    public void testBatteryProfile() throws Exception {

        final CountDownLatch tlatch = new CountDownLatch(1);
        testBean.readBatteryLevel(new Callback<BatteryLevel>() {
            @Override
            public void onResult(BatteryLevel result) {
                assertThat(result.getPercentage()).isGreaterThan(0);
                tlatch.countDown();
                Log.i(TAG, "TEST PASSED: Battery Profile");
            }
        });
        tlatch.await(20, TimeUnit.SECONDS);
    }
}
