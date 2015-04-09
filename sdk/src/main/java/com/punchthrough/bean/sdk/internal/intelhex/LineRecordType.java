package com.punchthrough.bean.sdk.internal.intelhex;

import com.punchthrough.bean.sdk.internal.utility.EnumParse;

public enum LineRecordType implements EnumParse.ParsableEnum {
    DATA(0),
    END_OF_FILE(1),
    EXTENDED_SEGMENT_ADDRESS(2),
    START_SEGMENT_ADDRESS(3),
    EXTENDED_LINEAR_ADDRESS(4),
    START_LINEAR_ADDRESS(5);

    private final int value;

    private LineRecordType(final int value) {
        this.value = value;
    }

    public int getRawValue() {
        return value;
    }
}
