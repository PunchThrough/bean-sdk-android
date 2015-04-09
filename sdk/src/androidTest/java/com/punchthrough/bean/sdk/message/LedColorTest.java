package com.punchthrough.bean.sdk.message;

import android.test.AndroidTestCase;

public class LedColorTest extends AndroidTestCase {

    public void testLedColorEquals() {
        LedColor color = LedColor.create(112, 113, 114);
        assertTrue(color.equals(LedColor.create(112, 113, 114)));
        assertFalse(color.equals(LedColor.create(7, 8, 9)));
    }

}
