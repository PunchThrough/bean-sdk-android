package com.punchthrough.bean.sdk.upload;

import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.utility.Chunk;
import com.punchthrough.bean.sdk.internal.utility.Constants;

import java.nio.ByteBuffer;
import java.util.Arrays;


import static com.punchthrough.bean.sdk.internal.utility.Convert.intToTwoBytes;
import static com.punchthrough.bean.sdk.internal.utility.Convert.twoBytesToInt;

/**
 * Holds data for a single firmware image
 */
public class FirmwareImage implements Chunk.Chunkable {

    private static final int FW_BLOCK_SIZE = 16;

    private byte[] rawData;
    private String filename;


    public FirmwareImage(byte[] rawData, String filename) throws ImageParsingException {
        if (rawData.length < 16) {
            throw new ImageParsingException("Images need to be at least 16 bytes long");
        }
        this.rawData = rawData;
        this.filename = filename;
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
                Constants.CC2540_BYTE_ORDER
        );
    }

    /**
     * Get a UInt8[4] from the data at the given offset.
     * @param offset    The offset at which to retrieve bytes
     * @return          The four-byte array starting at offset
     */
    private byte[] uint8_4FromData(int offset) {
        return Arrays.copyOfRange(data(), offset, offset + 4);
    }

    public byte[] data() {
        return rawData;
    }

    public String name() {
        return  filename.replace(".bin", "");
    }

    public int sizeBytes() {
        return rawData.length;
    }

    public long version() {
        return Long.parseLong(filename.split("_")[0]);
    }

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
    public int intVersion() {
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

    public byte[] metadata() {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.put(intToTwoBytes(intVersion(), Constants.CC2540_BYTE_ORDER));
        buffer.put(intToTwoBytes(length(), Constants.CC2540_BYTE_ORDER));
        buffer.put(uniqueID());
        buffer.put(reserved());
        return buffer.array();
    }

    /**
     * @return The number of blocks in this firmware image
     */
    public int blockCount() {
        return (int) Math.ceil((double) data().length / FW_BLOCK_SIZE);
    }

    /**
     * Get a firmware block for this image. Blocks are made up of a UINT16 block index followed
     * by a 16-byte data block (total of 18 bytes).
     *
     * @param index The index of the block to be returned
     * @return The block at the given index
     */
    public byte[] block(int index) {

        if (index >= blockCount()) {
            throw new ArrayIndexOutOfBoundsException("Invalid block index " + index);
        }

        byte[] theBlock = new byte[FW_BLOCK_SIZE + 2];

        // Copy the UINT16 block index into the array (2 bytes)
        byte[] rawIndex = intToTwoBytes(index, Constants.CC2540_BYTE_ORDER);
        System.arraycopy(rawIndex, 0, theBlock, 0, 2);

        // Copy the block data into the array (16 bytes)
        int blockStart = index * FW_BLOCK_SIZE;
        int length = FW_BLOCK_SIZE;
        while (blockStart + length > data().length) { length--; }
        System.arraycopy(data(), blockStart, theBlock, 2, length);

        return theBlock;
    }
}
