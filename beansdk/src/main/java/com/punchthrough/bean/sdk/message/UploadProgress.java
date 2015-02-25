package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

/**
 * Represents the upload progress of a Bean's upload in progress. Returned in the onUpdate
 * {@link com.punchthrough.bean.sdk.message.Callback} passed into
 * {@link com.punchthrough.bean.sdk.Bean#programWithSketch(com.punchthrough.bean.sdk.upload.SketchHex, Callback, Runnable)}.
 */
@AutoParcel
public abstract class UploadProgress implements Parcelable {

    /**
     * The number of chunks sent to the Bean so far.
     * @return The number of chunks sent
     */
    public abstract int chunksSent();

    /**
     * The total number of chunks being sent to the Bean.
     * @return The total number of chunks being sent
     */
    public abstract int totalChunks();

    public static UploadProgress create(int chunksSent, int totalChunks) {
        return new AutoParcel_UploadProgress(chunksSent, totalChunks);
    }

    /**
     * The completion percentage of the upload in progress
     *
     * @return Completion percentage, from 0.0 to 1.0
     */
    public float percent() {
        return ( (float) chunksSent() ) / totalChunks();
    }

}
