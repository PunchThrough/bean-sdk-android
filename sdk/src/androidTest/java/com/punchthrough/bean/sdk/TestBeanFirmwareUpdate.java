package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.FirmwareBundle;
import com.punchthrough.bean.sdk.upload.FirmwareImage;
import com.punchthrough.bean.sdk.util.BeanTestCase;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestBeanFirmwareUpdate extends BeanTestCase {

    public void setUp() {
        super.setUp();
        setUpTestBean();
    }

    public void tearDown() {
        super.tearDown();
        tearDownTestBean();
    }

    public void testFirmwareUpdate() throws Exception {

        final String FIRMWARE_IMAGES_LOCATION = "firmware_bundles/asymmetrical";

        List<FirmwareImage> fwImages = new ArrayList<>();
        for (String imageFileName : filesInAssetDir(getContext(), FIRMWARE_IMAGES_LOCATION)) {
            String imagePath = FilenameUtils.concat(FIRMWARE_IMAGES_LOCATION, imageFileName);
            InputStream imageStream  = getContext().getAssets().open(imagePath);
            FirmwareImage image = new FirmwareImage(IOUtils.toByteArray(imageStream), imageFileName);
            fwImages.add(image);
        }

        FirmwareBundle bundle = new FirmwareBundle(fwImages);

        Callback<UploadProgress> onProgress = new Callback<UploadProgress>() {
            @Override
            public void onResult(UploadProgress result) {
                System.out.println("[BEANTEST] On Result: " + result);
            }
        };

        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                System.out.println("[BEANTEST] - Complete!");
            }
        };

        CountDownLatch fwLatch = new CountDownLatch(1);
        testBean.programWithFirmware(bundle, onProgress, onComplete);
        fwLatch.await(120, TimeUnit.SECONDS);

    }
}
