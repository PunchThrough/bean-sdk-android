package com.punchthrough.bean.sdk.message;

import com.punchthrough.bean.sdk.internal.utility.EnumParse;

// Enum as int technique from http://stackoverflow.com/a/3990421/254187

/**
 * Represents an accelerometer range for the Bean's accelerometer. Setting the Bean's accelerometer
 * to lower ranges increases accuracy within that range but makes it impossible to accurately read
 * accuracy outside that range.
 */
public enum AccelerometerRange implements EnumParse.ParsableEnum {
    RANGE_2G(2), RANGE_4G(4), RANGE_8G(8), RANGE_16G(16);

    private final int value;

    private AccelerometerRange(final int value) {
        this.value = value;
    }

    public int getRawValue() {
        return value;
    }
}
