package com.punchthrough.bean.sdk.internal.upload.firmware;

import com.punchthrough.bean.sdk.internal.exception.MetadataParsingException;
import com.punchthrough.bean.sdk.internal.utility.Constants;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intToTwoBytes;
import static com.punchthrough.bean.sdk.internal.utility.Convert.twoBytesToInt;


public class FirmwareMetadata {

    byte[] metaData;

    public static FirmwareMetadata fromPayload(byte[] payload) throws MetadataParsingException {

        if (payload.length != 8) {
            throw new MetadataParsingException(
                    "Metadata length must be 8, found " + payload.length);
        }

        int version = twoBytesToInt(Arrays.copyOfRange(payload, 0, 2), Constants.CC2540_BYTE_ORDER);
        int length = twoBytesToInt(Arrays.copyOfRange(payload, 2, 4), Constants.CC2540_BYTE_ORDER);
        byte[] uniqueID = Arrays.copyOfRange(payload, 4, 8);

        return new FirmwareMetadata(version, length, uniqueID);

    }

    public FirmwareMetadata(int version, int length, byte[] uniqueID) {
        ByteBuffer buffer = ByteBuffer.allocate(8);

        buffer.put(intToTwoBytes(version, Constants.CC2540_BYTE_ORDER));
        buffer.put(intToTwoBytes(length, Constants.CC2540_BYTE_ORDER));
        buffer.put(uniqueID);

        this.metaData = buffer.array();
    }

    public int version() {
        return twoBytesToInt(Arrays.copyOfRange(data(), 0, 2), Constants.CC2540_BYTE_ORDER);
    }

    public int length() {
        return twoBytesToInt(Arrays.copyOfRange(data(), 2, 4), Constants.CC2540_BYTE_ORDER);
    }

    public byte[] uniqueID() {
        return Arrays.copyOfRange(data(), 4, 8);
    }

    protected byte[] data() {
        return metaData;
    }

    public byte[] toPayload() {
        return data();
    }

}
