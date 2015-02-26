package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class FirmwareChunk implements Parcelable {

    public abstract int chunkNumber();
    public abstract byte[] data();

    public static FirmwareChunk create(int chunkNumber, byte[] data) {
        return new AutoParcel_FirmwareChunk(chunkNumber, data);
    }

}
