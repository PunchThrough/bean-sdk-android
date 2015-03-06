package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.exception.MetadataParsingException;
import com.punchthrough.bean.sdk.internal.utility.Constants;

import java.nio.ByteBuffer;
import java.util.Arrays;

import auto.parcel.AutoParcel;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intToTwoBytes;
import static com.punchthrough.bean.sdk.internal.utility.Convert.twoBytesToInt;

@AutoParcel
public abstract class FirmwareMetadata implements Parcelable {

    public int version() {
        return twoBytesToInt(Arrays.copyOfRange(data(), 0, 2), Constants.CC2540_BYTE_ORDER);
    }

    public int length() {
        return twoBytesToInt(Arrays.copyOfRange(data(), 2, 4), Constants.CC2540_BYTE_ORDER);
    }

    public byte[] uniqueID() {
        return Arrays.copyOfRange(data(), 4, 8);
    }

    protected abstract byte[] data();

    public static FirmwareMetadata create(int version, int length, byte[] uniqueID) {

        ByteBuffer buffer = ByteBuffer.allocate(8);

        buffer.put(intToTwoBytes(version, Constants.CC2540_BYTE_ORDER));
        buffer.put(intToTwoBytes(length, Constants.CC2540_BYTE_ORDER));
        buffer.put(uniqueID);

        return new AutoParcel_FirmwareMetadata(buffer.array());

    }

    public static FirmwareMetadata fromPayload(byte[] payload) throws MetadataParsingException {

        if (payload.length != 8) {
            throw new MetadataParsingException(
                    "Metadata length must be 8, found " + payload.length);
        }

        return new AutoParcel_FirmwareMetadata(payload);

    }

    public byte[] toPayload() {
        return data();
    }

    /**
     * <p>
     * The {@link com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType}
     * of the image represented by this metadata.
     * </p>
     *
     * <p>
     * Determined by {@link FirmwareMetadata#uniqueID()},
     * which is "AAAA" or "BBBB" for A and B images
     * respectively. Images A and B are identical but occupy different areas of CC storage.
     * </p>
     *
     * @return  {@link com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType#A},
     *          {@link com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType#B}, or
     *          null if {@link FirmwareMetadata#uniqueID()} is not "AAAA" or "BBBB"
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

}
