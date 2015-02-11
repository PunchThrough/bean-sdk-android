package com.punchthrough.bean.sdk.message;

import android.test.AndroidTestCase;

public class ScratchBankTest extends AndroidTestCase {

    public void testBankRawNumbers() {
        assertTrue(ScratchBank.BANK_1.getRawBankNumber() == 0);
        assertTrue(ScratchBank.BANK_2.getRawBankNumber() == 1);
        assertTrue(ScratchBank.BANK_3.getRawBankNumber() == 2);
        assertTrue(ScratchBank.BANK_4.getRawBankNumber() == 3);
        assertTrue(ScratchBank.BANK_5.getRawBankNumber() == 4);
    }

}
