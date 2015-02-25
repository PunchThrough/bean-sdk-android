package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;

import static com.punchthrough.bean.sdk.internal.utility.Misc.intArrayToByteArray;
import static org.assertj.core.api.Assertions.assertThat;

public class FirmwareImageTest extends AndroidTestCase {

    // Valid images have all necessary headers
    byte[] validImage = intArrayToByteArray(new int[] {
            0x2B, 0x65, 0xFF, 0xFF, 0x64, 0x00, 0x00, 0x7C,
            0x41, 0x41, 0x41, 0x41, 0xFF, 0xFF, 0xFF, 0xFF
    });

    // Invalid images don't have a long enough header
    byte[] invalidImage = intArrayToByteArray(new int[] {
            0x01, 0x02, 0x03, 0x04
    });

    public void TestParsingValidImage() throws ImageParsingException {

        FirmwareImage image = FirmwareImage.create(validImage);

        assertThat(image.crc()).isEqualTo(25899);
        assertThat(image.version()).isEqualTo(100);
        assertThat(image.length()).isEqualTo(31744);
        assertThat(image.uniqueID()).isEqualTo(
                intArrayToByteArray(new int[] {0x41, 0x41, 0x41, 0x41}));

    }

    public void TestParsingInvalidImage() {

        try {
            FirmwareImage image = FirmwareImage.create(invalidImage);
            fail();

        } catch (ImageParsingException e) {
            assertThat(e).isNotNull();

        }

    }

}
