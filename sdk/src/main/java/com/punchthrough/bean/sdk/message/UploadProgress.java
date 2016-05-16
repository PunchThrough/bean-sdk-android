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
     * The number of blocks sent to the Bean so far.
     * @return The number of blocks sent
     */
    public abstract int blocksSent();

    /**
     * The total number of blocks being sent to the Bean.
     * @return The total number of blocks being sent
     */
    public abstract int totalBlocks();

    public static UploadProgress create(int blocksSent, int totalBlocks) {
        return new AutoParcel_UploadProgress(blocksSent, totalBlocks);
    }

    /**
     * The completion percentage of the upload in progress
     *
     * @return Completion percentage, from 0.0 to 1.0
     */
    public float completionPercent() {
        return ((float) blocksSent() ) / totalBlocks();
    }

    public String completionBlocks() {
        return String.format("%s/%s", blocksSent(), totalBlocks());
    }

}
