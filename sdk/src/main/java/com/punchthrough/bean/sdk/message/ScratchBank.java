package com.punchthrough.bean.sdk.message;

import com.punchthrough.bean.sdk.internal.utility.EnumParse;

// Enum as int technique from http://stackoverflow.com/a/3990421/254187

/**
 * Represents the scratch bank data is being sent to/read from.
 */
public enum ScratchBank implements EnumParse.ParsableEnum {
    BANK_1(0), BANK_2(1), BANK_3(2), BANK_4(3), BANK_5(4);

    private final int value;

    private ScratchBank(final int value) {
        this.value = value;
    }

    public int getRawValue() {
        return value;
    }
}
