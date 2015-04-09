package com.punchthrough.bean.sdk.upload;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType;
import com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareMetadata;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intArrayToByteArray;
import static org.assertj.core.api.Assertions.assertThat;

public class FirmwareImageTest extends AndroidTestCase {

    // Valid images have all necessary headers
    byte[] validImageA = intArrayToByteArray(new int[] {
            0x2B, 0x65, 0xFF, 0xFF, 0x64, 0x00, 0x00, 0x7C,
            0x41, 0x41, 0x41, 0x41, 0xFF, 0xFF, 0xFF, 0xFF
    });

    // Image B has 0x42 "B"s instead of 0x41 "A"s
    byte[] validImageB = intArrayToByteArray(new int[] {
            0x2B, 0x65, 0xFF, 0xFF, 0x64, 0x00, 0x00, 0x7C,
            0x42, 0x42, 0x42, 0x42, 0xFF, 0xFF, 0xFF, 0xFF
    });

    // This image has no valid type
    byte[] validImageNoType = intArrayToByteArray(new int[] {
            0x2B, 0x65, 0xFF, 0xFF, 0x64, 0x00, 0x00, 0x7C,
            0x41, 0x42, 0x43, 0x44, 0xFF, 0xFF, 0xFF, 0xFF
    });

    // Invalid images don't have a long enough header
    byte[] invalidImage = intArrayToByteArray(new int[] {
            0x01, 0x02, 0x03, 0x04
    });

    // This image is of an odd length
    byte[] oddLengthImage = intArrayToByteArray(new int[] {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
            0x21, 0x22, 0x23
    });

    public void testParsingValidImage() throws ImageParsingException {

        FirmwareImage image = FirmwareImage.create(validImageA);

        assertThat(image.crc()).isEqualTo(25899);
        assertThat(image.version()).isEqualTo(100);
        assertThat(image.length()).isEqualTo(31744);
        assertThat(image.uniqueID()).isEqualTo(
                intArrayToByteArray(new int[] {0x41, 0x41, 0x41, 0x41}));
        assertThat(image.type()).isEqualTo(FirmwareImageType.A);

    }

    public void testParsingTypeB() throws ImageParsingException {

        FirmwareImage image = FirmwareImage.create(validImageB);
        assertThat(image.type()).isEqualTo(FirmwareImageType.B);

    }

    public void testParsingTypeInvalid() throws ImageParsingException {

        FirmwareImage image = FirmwareImage.create(validImageNoType);
        assertThat(image.type()).isNull();

    }

    public void testParsingInvalidImage() {

        try {
            FirmwareImage.create(invalidImage);
            fail();

        } catch (ImageParsingException e) {
            assertThat(e).isNotNull();

        }

    }

    public void testGetMetadata() throws ImageParsingException {

        FirmwareImage image = FirmwareImage.create(validImageA);
        FirmwareMetadata metadata = image.metadata();
        assertThat(metadata.version()).isEqualTo(100);
        assertThat(metadata.length()).isEqualTo(31744);
        assertThat(metadata.uniqueID()).isEqualTo(new byte[] {0x41, 0x41, 0x41, 0x41});

    }

    public void testFirmwareBlocks() throws ImageParsingException {

        FirmwareImage image = FirmwareImage.create(oddLengthImage);
        assertThat(image.block(0)).isEqualTo(intArrayToByteArray(new int[]{
                0x00, 0x00,
                0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18
        }));
        assertThat(image.block(1)).isEqualTo(intArrayToByteArray(new int[]{
                0x01, 0x00,
                0x21, 0x22, 0x23, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        }));

    }

}
