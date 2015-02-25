package com.punchthrough.bean.sdk.upload;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType;

import static com.punchthrough.bean.sdk.internal.utility.Misc.intArrayToByteArray;
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

    // Invalid images don't have a long enough header
    byte[] invalidImage = intArrayToByteArray(new int[] {
            0x01, 0x02, 0x03, 0x04
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

    public void testParsingInvalidImage() {

        try {
            FirmwareImage image = FirmwareImage.create(invalidImage);
            fail();

        } catch (ImageParsingException e) {
            assertThat(e).isNotNull();

        }

    }

}
