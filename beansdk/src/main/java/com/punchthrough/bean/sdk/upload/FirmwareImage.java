package com.punchthrough.bean.sdk.upload;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;

import java.nio.ByteOrder;
import java.util.Arrays;

import auto.parcel.AutoParcel;

import static com.punchthrough.bean.sdk.internal.utility.Misc.twoBytesToInt;

@AutoParcel
public abstract class FirmwareImage implements Parcelable {

    // The byte order used by the CC2540
    public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    // Raw image data
    public abstract byte[] data();

    /**
     * The CRC16 of the image.
     * @return The CRC16 of the image
     */
    public int crc() {
        return uint16FromData(0);
    }

    /**
     * The CRC shadow of the image. This is always 0xFFFF.
     * @return The CRC shadow of the image
     */
    public int crcShadow() {
        return uint16FromData(2);
    }

    /**
     * The version of the image. This is used to determine if an image is newer than another image.
     * @return The version of the image
     */
    public int version() {
        return uint16FromData(4);
    }

    /**
     * The length of the image, in bytes.
     * @return The length of the image
     */
    public int length() {
        return uint16FromData(5);
    }

    /**
     * The user-defined unique ID for the image.
     * @return The image's unique ID
     */
    public byte[] uniqueID() {
        return uint8_4FromData(8);
    }

    /**
     * The reserved bytes of the image. These are not currently used and usually return 0xFFFF.
     * @return The image's reserved bytes
     */
    public byte[] reserved() {
        return uint8_4FromData(12);
    }

    /**
     * Parses a block of data into a new FirmwareImage.
     *
     * @param data  The bytes of image data
     * @return      The FirmwareImage object for those bytes of data
     */
    public static FirmwareImage create(byte[] data) throws ImageParsingException {
        FirmwareImage image = new AutoParcel_FirmwareImage(data);
        try {
            // Ensure there are enough bytes in the data[] by calling the last field getter
            image.reserved();
        } catch (Exception e) {
            throw new ImageParsingException(e.getLocalizedMessage());
        }
        return image;
    }

    /**
     * Parse a little-endian UInt16 from the data at the given offset.
     *
     * @param offset    The offset at which to parse data
     * @return          The Java int representation of the parsed bytes
     */
    private int uint16FromData(int offset) {
        return twoBytesToInt(Arrays.copyOfRange(data(), offset, offset + 2), BYTE_ORDER);
    }

    /**
     * Get a UInt8[4] from the data at the given offset.
     * @param offset    The offset at which to retrieve bytes
     * @return          The four-byte array starting at offset
     */
    private byte[] uint8_4FromData(int offset) {
        return Arrays.copyOfRange(data(), offset, offset + 4);
    }

}
