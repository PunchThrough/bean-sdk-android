package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class UploadProgress implements Parcelable {

    public abstract int chunksSent();
    public abstract int totalChunks();

    public static UploadProgress create(int chunksSent, int totalChunks) {
        return new AutoParcel_UploadProgress(chunksSent, totalChunks);
    }

    public float percent() {
        return ( (float) chunksSent() ) / totalChunks();
    }

}
