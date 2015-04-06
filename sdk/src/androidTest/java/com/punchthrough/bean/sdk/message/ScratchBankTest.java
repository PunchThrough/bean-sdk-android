package com.punchthrough.bean.sdk.message;

import android.test.AndroidTestCase;

public class ScratchBankTest extends AndroidTestCase {

    public void testBankRawNumbers() {
        assertTrue(ScratchBank.BANK_1.getRawValue() == 0);
        assertTrue(ScratchBank.BANK_2.getRawValue() == 1);
        assertTrue(ScratchBank.BANK_3.getRawValue() == 2);
        assertTrue(ScratchBank.BANK_4.getRawValue() == 3);
        assertTrue(ScratchBank.BANK_5.getRawValue() == 4);
    }

}
