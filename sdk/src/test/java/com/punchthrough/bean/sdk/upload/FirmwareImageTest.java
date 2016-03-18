package com.punchthrough.bean.sdk.upload;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareMetadata;

import org.junit.Test;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intArrayToByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class FirmwareImageTest {

    byte[] rawImageData_valid = intArrayToByteArray(new int[] {
            0x2B, 0x65,                // CRC
            0xFF, 0xFF,                // CRC Shadow
            0x64, 0x00,                // Version
            0x00, 0x7C,                // Length
            0x41, 0x41, 0x41, 0x41,    // AAAA
            0xFF, 0xFF, 0xFF, 0xFF     // Reserved
    });

    byte[] rawImageData_tooShort = intArrayToByteArray(new int[] {
            0x2B, 0x65, 0xFF, 0xFF,
    });

    byte[] rawImageData_invalid = intArrayToByteArray(new int[] {

            // Valid header - 16 bytes data
            0x2B, 0x65,                // CRC
            0xFF, 0xFF,                // CRC Shadow
            0x64, 0x00,                // Version
            0x00, 0x7C,                // Length
            0x41, 0x41, 0x41, 0x41,    // AAAA
            0xFF, 0xFF, 0xFF, 0xFF,     // Reserved

            // Valid block - 16 bytes of image data
            0xFF, 0xFF,
            0xFF, 0xFF,
            0xFF, 0xFF,
            0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,

            // Invalid block - only 10 bytes of image data
            0xFF, 0xFF,
            0xFF, 0xFF,
            0xFF, 0xFF,
            0xFF, 0xFF,
            0xFF, 0xFF

    });

    @Test
    public void testParsingValidImage() throws ImageParsingException {
        FirmwareImage image = new FirmwareImage(rawImageData_valid, "");
        assertThat(image.crc()).isEqualTo(25899);
        assertThat(image.version()).isEqualTo(100);
        assertThat(image.length()).isEqualTo(31744);
        assertThat(image.uniqueID()).isEqualTo(intArrayToByteArray(new int[] {0x41, 0x41, 0x41, 0x41}));
    }

    @Test
    public void testInvalidImageTooShort() throws ImageParsingException {
        try {
            new FirmwareImage(rawImageData_tooShort, "");
            fail("Shouldn't have worked");
        } catch (ImageParsingException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    public void testGetMetadata() throws ImageParsingException {
        FirmwareImage image = new FirmwareImage(rawImageData_valid, "");
        FirmwareMetadata metadata = image.metadata();
        assertThat(metadata.version()).isEqualTo(100);
        assertThat(metadata.length()).isEqualTo(31744);
        assertThat(metadata.uniqueID()).isEqualTo(new byte[] {0x41, 0x41, 0x41, 0x41});

    }

    @Test
    public void testFirmwareBlocks() throws ImageParsingException {

        FirmwareImage image = new FirmwareImage(rawImageData_invalid, "");

        assertThat(image.block(0)).isEqualTo(intArrayToByteArray(new int[]{

                // Block index (0x0000 little endian)
                0x00, 0x00,

                // Header data
                0x2B, 0x65, 0xFF, 0xFF,
                0x64, 0x00, 0x00, 0x7C,
                0x41, 0x41, 0x41, 0x41,
                0xFF, 0xFF, 0xFF, 0xFF
        }));

        assertThat(image.block(1)).isEqualTo(intArrayToByteArray(new int[]{

                // Block index (0x0001 little endian)
                0x01, 0x00,

                // Block 1 data
                0xFF, 0xFF, 0xFF, 0xFF,
                0xFF, 0xFF, 0xFF, 0xFF,
                0xFF, 0xFF, 0xFF, 0xFF,
                0xFF, 0xFF, 0xFF, 0xFF
        }));

        assertThat(image.block(2)).isEqualTo(intArrayToByteArray(new int[]{

                // Block index (0x0002 little endian)
                0x02, 0x00,

                // 10 Bytes of valid block data
                0xFF, 0xFF,
                0xFF, 0xFF,
                0xFF, 0xFF,
                0xFF, 0xFF,
                0xFF, 0xFF,

                // 6 bytes of zeros because the image is "invalid"
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        }));

        // There is no block 3, this should fail
        try {
            image.block(3);
            fail("Shouldn't have worked");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }
}
