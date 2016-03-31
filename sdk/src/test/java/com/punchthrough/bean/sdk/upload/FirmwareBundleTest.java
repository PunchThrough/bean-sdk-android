package com.punchthrough.bean.sdk.upload;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.exception.OADException;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intArrayToByteArray;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

public class FirmwareBundleTest {

    byte[] rawImageData = intArrayToByteArray(new int[] {
            0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF
    });

    List<FirmwareImage> images = new ArrayList<>();
    FirmwareImage imageA;
    FirmwareImage imageB;
    FirmwareBundle bundle;

    @Before
    public void setUp() throws ImageParsingException {
        imageA = new FirmwareImage(rawImageData, "123450000_a_testNameA.bin");
        imageB = new FirmwareImage(rawImageData, "123450000_a_testNameB.bin");
    }

    @Test
    public void testFirmwareBundleVersion() throws ImageParsingException {
        images.add(imageA);
        bundle = new FirmwareBundle(images);
        assertThat(bundle.version()).isEqualTo(123450000);
    }

    @Test
    public void testFirmwareBundleGetNextImageOneImage() throws ImageParsingException, OADException {
        // Create a bundle with only 1 image
        images.add(imageA);
        bundle = new FirmwareBundle(images);

        assertThat(bundle.getNextImage()).isEqualTo(imageA);
        try {
            bundle.getNextImage();
            fail("Expected OAD Exception");
        } catch (OADException e) {
            // good
        }

    }

    @Test
    public void testFirmwareBundleGetNextImageManyImages() throws ImageParsingException, OADException {
        // Create a bundle with more than 1 image
        images.add(imageA);
        images.add(imageB);
        bundle = new FirmwareBundle(images);

        // Images shoute rotate each call
        assertThat(bundle.getNextImage()).isEqualTo(imageA);
        assertThat(bundle.getNextImage()).isEqualTo(imageB);
        try {
            bundle.getNextImage();
            fail("Expected OAD Exception");
        } catch (OADException e) {
            // good
        }
    }

}
