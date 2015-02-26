package com.punchthrough.bean.sdk.upload;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareChunk;
import com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType;
import com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareMetadata;
import com.punchthrough.bean.sdk.internal.utility.Chunkable;
import com.punchthrough.bean.sdk.internal.utility.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import auto.parcel.AutoParcel;

import static com.punchthrough.bean.sdk.internal.utility.Misc.chunksFromChunkable;
import static com.punchthrough.bean.sdk.internal.utility.Misc.twoBytesToInt;

@AutoParcel
public abstract class FirmwareImage implements Parcelable, Chunkable {

    /**
     * The chunk size of firmware packets being sent
     */
    private static final int FW_CHUNK_SIZE = 16;

    /**
     * The raw firmware image data.
     * @return The raw image data
     */
    public abstract byte[] data();

    @Override
    public byte[] getChunkableData() {
        return data();
    }

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
        return uint16FromData(6);
    }

    /**
     * The user-defined unique ID for the image. Currently determines image type.
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
     * <p>
     * The {@link com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType}
     * of this image.
     * </p>
     *
     * <p>
     * Determined by {@link FirmwareImage#uniqueID()},
     * which is "AAAA" or "BBBB" for A and B images
     * respectively. Images A and B are identical but occupy different areas of CC storage.
     * </p>
     *
     * @return  {@link com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType#A},
     *          {@link com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType#B}, or
     *          null if {@link FirmwareImage#uniqueID()} is not "AAAA" or "BBBB"
     */
    public FirmwareImageType type() {
        String parsedID = new String(uniqueID());

        if (parsedID.equals(Constants.IMAGE_A_ID)) {
            return FirmwareImageType.A;

        } else if (parsedID.equals(Constants.IMAGE_B_ID)) {
            return FirmwareImageType.B;

        } else {
            return null;

        }
    }

    public FirmwareMetadata metadata() {
        return FirmwareMetadata.create(version(), length(), uniqueID());
    }

    /**
     * Get all {@link com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareChunk}s for this
     * firmware image.
     * @return All chunks for this image
     */
    public List<FirmwareChunk> chunks() {

        List<FirmwareChunk> chunks = new ArrayList<>();

        List<byte[]> rawChunks = chunksFromChunkable(this, FW_CHUNK_SIZE);
        int i = 0;
        for (byte[] rawChunk : rawChunks) {
            FirmwareChunk chunk = FirmwareChunk.create(i, rawChunk);
            chunks.add(chunk);
        }

        return chunks;

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
        return twoBytesToInt(
                Arrays.copyOfRange(data(), offset, offset + 2),
                Constants.CC2540_BYTE_ORDER);
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
