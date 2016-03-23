package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
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

    private Bean bean;

    public void setUp() {
        super.setUp();
        try {
            bean = discoverClosestBean();
            BeanManager.getInstance().cancelDiscovery();
            synchronousConnect(bean);
            bean.setAutoReconnect(true);
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

    public void testFirmwareUpdate() throws Exception {

        final Callback<UploadProgress> onProgress = new Callback<UploadProgress>() {
            @Override
            public void onResult(UploadProgress result) {
                System.out.println("[BEANTEST] On Result: " + result);
            }
        };

        final Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                System.out.println("[BEANTEST] - Complete!");
            }
        };


        bean.programWithFirmware(getAsymmBundle(), onProgress, onComplete);
        CountDownLatch fwLatch = new CountDownLatch(1);
        fwLatch.await(8000, TimeUnit.SECONDS);

    }

}
