package com.punchthrough.bean.sdk;

import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.upload.firmware.OADProfile;
import com.punchthrough.bean.sdk.internal.upload.firmware.OADState;
import com.punchthrough.bean.sdk.message.BeanError;
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

    private Bean bean;
    private OADProfile.OADApproval oadApproval;

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

    private FirmwareBundle getAsymmBundle() {
        String FW_IMAGES_DIR = "firmware_bundles/asymmetrical/bean";
        List<FirmwareImage> fwImages = new ArrayList<>();
        for (String imageFileName : filesInAssetDir(getContext(), FW_IMAGES_DIR)) {
            String imagePath = FilenameUtils.concat(FW_IMAGES_DIR, imageFileName);
            try {
                InputStream imageStream = getContext().getAssets().open(imagePath);
                FirmwareImage image = new FirmwareImage(IOUtils.toByteArray(imageStream), imageFileName);
                fwImages.add(image);
            } catch (IOException | ImageParsingException e) {
                fail(e.getMessage());
            }
        }

        return new FirmwareBundle(fwImages);
    }

    @Suppress
    public void testFirmwareUpdate() throws Exception {

        final CountDownLatch fwLatch = new CountDownLatch(1);

        oadApproval = bean.programWithFirmware(getAsymmBundle(), new OADProfile.OADListener() {
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
            public void stateChange(OADState state){};

        });

        // Wait 5 minutes for it to complete or fail
        fwLatch.await(300, TimeUnit.SECONDS);
    }
}
