package com.punchthrough.bean.sdk.message;

import com.punchthrough.bean.sdk.internal.utility.EnumParse;

// Enum as int technique from http://stackoverflow.com/a/3990421/254187

/**
 * Represents the scratch bank data is being sent to/read from.
 */
public enum ScratchBank implements EnumParse.ParsableEnum {
    BANK_1(1), BANK_2(2), BANK_3(3), BANK_4(4), BANK_5(5);

    private final int value;

    private ScratchBank(final int value) {
        this.value = value;
    }

    public int getRawValue() {
        return value;
    }
}
