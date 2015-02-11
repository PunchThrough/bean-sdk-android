package com.punchthrough.bean.sdk.message;

// Enum as int technique from http://stackoverflow.com/a/3990421/254187
public enum AccelerometerRange {
    RANGE_2G(2), RANGE_4G(4), RANGE_8G(8), RANGE_16G(16);

    private final int value;

    private AccelerometerRange(final int value) {
        this.value = value;
    }

    public int getRawRange() {
        return value;
    }
}
