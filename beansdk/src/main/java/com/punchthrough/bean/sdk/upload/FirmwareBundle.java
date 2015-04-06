package com.punchthrough.bean.sdk.upload;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.upload.firmware.FirmwareImageType;

import auto.parcel.AutoParcel;

/**
 * Represents a bundle of A and B firmware images to be sent to the Bean.
 */
@AutoParcel
public abstract class FirmwareBundle implements Parcelable {

    public abstract FirmwareImage imageA();
    public abstract FirmwareImage imageB();

    /**
     * Create a new firmware bundle from an A and B image.
     * @param imageA The A image
     * @param imageB The B image
     * @return       The firmware bundle
     * @throws java.lang.IllegalArgumentException if either image is of the wrong type
     */
    public static FirmwareBundle create(FirmwareImage imageA, FirmwareImage imageB) {
        if (imageA.type() != FirmwareImageType.A) {
            throw new IllegalArgumentException("Image A was not of type A");
        } else if (imageB.type() != FirmwareImageType.B) {
            throw new IllegalArgumentException("Image B was not of type B");
        }
        return new AutoParcel_FirmwareBundle(imageA, imageB);
    }

}
