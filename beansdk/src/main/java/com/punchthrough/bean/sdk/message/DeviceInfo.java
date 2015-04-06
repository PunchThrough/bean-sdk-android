package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

/**
 * Represents the hardware, software, and firmware versions of the Bean.
 */
@AutoParcel
public abstract class DeviceInfo implements Parcelable {
    public static final String UNKNOWN_VERSION = "";

    public static DeviceInfo create() {
        return new AutoParcel_DeviceInfo(UNKNOWN_VERSION, UNKNOWN_VERSION, UNKNOWN_VERSION);
    }

    public static DeviceInfo create(String hardwareVersion, String softwareVersion, String firmwareVersion) {
        return new AutoParcel_DeviceInfo(hardwareVersion, softwareVersion, firmwareVersion);
    }

    public abstract String hardwareVersion();

    public abstract String softwareVersion();

    public abstract String firmwareVersion();
}
