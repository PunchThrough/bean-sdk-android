package com.punchthrough.bean.sdk.message;

// Enum as int technique from http://stackoverflow.com/a/3990421/254187
public enum ScratchBank {
    BANK_1(0), BANK_2(1), BANK_3(2), BANK_4(3), BANK_5(4);

    private final int value;

    private ScratchBank(final int value) {
        this.value = value;
    }

    public int getRawBankNumber() {
        return value;
    }
}
