package com.punchthrough.bean.sdk;

import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.upload.firmware.OADProfile;
import com.punchthrough.bean.sdk.internal.upload.firmware.OADState;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.FirmwareBundle;
import com.punchthrough.bean.sdk.upload.FirmwareImage;
import com.punchthrough.bean.sdk.util.BeanTestCase;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestBeanFirmwareUpdate extends BeanTestCase {

    private final String TAG = "TestBeanFirmwareUpdate";
    private final int FW_TEST_MAX_DURATION = 5;  // Minutes

    private Bean bean;
    private OADProfile.OADApproval oadApproval;
    private static String hwVersion;

    public void setUp() {
        super.setUp();
        try {
            bean = discoverBean();
            synchronousConnect(bean);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not connect to close Bean");
        }
    }

    public void tearDown() {
        super.tearDown();
        try {
            synchronousDisconnect(bean);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Could not disconnect to Bean");
        }
    }

    private String bundlePathForHardwareRevision(String hardwareRevision) throws Exception {

        // Match on Bean Versions
        if (hardwareRevision.startsWith("1") || hardwareRevision.startsWith("E")) {
            return "firmware_bundles/asymmetrical/bean";
        }

        // Match on Bean+ Versions
        if (hardwareRevision.startsWith("2")) {
            return "firmware_bundles/asymmetrical/beanplus";
        }

        throw new Exception("Invalid hardware version: " + hardwareRevision);
    }

    public FirmwareBundle getFirmwareBundle(String hardwareRevision) throws Exception {

        Log.i(TAG, "Finding firmware bundle for hardware version: " + hardwareRevision);

        String bundlePath = bundlePathForHardwareRevision(hardwareRevision);
        List<FirmwareImage> fwImages = new ArrayList<>();
        for (String imageFileName : filesInAssetDir(getContext(), bundlePath)) {
            String imagePath = FilenameUtils.concat(bundlePath, imageFileName);
            try {
                InputStream imageStream = getContext().getAssets().open(imagePath);
                FirmwareImage image = new FirmwareImage(IOUtils.toByteArray(imageStream), imageFileName);
                fwImages.add(image);
            } catch (IOException | ImageParsingException e) {
                throw new Exception(e.getMessage());
            }
        }

        FirmwareBundle bundle = new FirmwareBundle(fwImages);
        Log.i(TAG, "Found firmware bundle: " + bundle.version());
        return bundle;
    }

    @Suppress
    public void testFirmwareUpdate() throws Exception {

        final CountDownLatch fwLatch = new CountDownLatch(1);
        final CountDownLatch hwVersionLatch = new CountDownLatch(1);

        bean.readHardwareVersion(new Callback<String>() {

            @Override
            public void onResult(String hardwareVersion) {
                hwVersion = hardwareVersion;
                hwVersionLatch.countDown();
            }
        });

        hwVersionLatch.await(10, TimeUnit.SECONDS);
        if (hwVersion == null) {
            fail("Couldn't get HW version");
        }

        oadApproval = bean.programWithFirmware(getFirmwareBundle(hwVersion), new OADProfile.OADListener() {

            @Override
            public void complete() {
                Log.i(TAG, "OAD Process Complete!");
                fwLatch.countDown();
            }

            @Override
            public void error(BeanError error) {
                Log.e(TAG, "OAD Error: " + error.toString());
                fail();
            }

            @Override
            public void progress(UploadProgress uploadProgress) {
                if (uploadProgress.blocksSent() % 50 == 0) {
                    Log.i(TAG, "OAD Progress: " + uploadProgress.completionBlocks());
                }
            }

            @Override
            public void updateRequired(boolean required) {
                if (required) {
                    oadApproval.allow();
                } else {
                    fwLatch.countDown();
                }
            }

            @Override
            public void stateChange(OADState state) {}

        });

        // Wait 5 minutes for it to complete or fail
        fwLatch.await(FW_TEST_MAX_DURATION * 60, TimeUnit.SECONDS);
        if (fwLatch.getCount() > 0) {
            fail("Firmware Update Test took too long!");
        } else {
            Log.i(TAG, "Firmware Update Test completed successfully!");
        }

    }
}
