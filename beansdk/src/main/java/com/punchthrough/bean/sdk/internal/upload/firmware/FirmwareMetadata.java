package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.exception.MetadataParsingException;

import java.nio.ByteOrder;
import java.util.Arrays;

import auto.parcel.AutoParcel;

import static com.punchthrough.bean.sdk.internal.utility.Misc.twoBytesToInt;

@AutoParcel
public abstract class FirmwareMetadata implements Parcelable {

    public abstract int version();
    public abstract int length();
    public abstract byte[] uniqueID();

    private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    public static FirmwareMetadata fromPayload(byte[] payload) throws MetadataParsingException {

        if (payload.length != 8) {
            throw new MetadataParsingException(
                    "Metadata length must be 8, found " + payload.length);
        }

        int version = twoBytesToInt(Arrays.copyOfRange(payload, 0, 2), BYTE_ORDER);
        int length = twoBytesToInt(Arrays.copyOfRange(payload, 2, 4), BYTE_ORDER);
        byte[] uniqueID = Arrays.copyOfRange(payload, 4, 8);

        return new AutoParcel_FirmwareMetadata(version, length, uniqueID);

    }

}
