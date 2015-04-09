package com.punchthrough.bean.sdk.message;

import android.test.AndroidTestCase;

public class AccelerometerRangeTest extends AndroidTestCase {

    public void testAccelRawRanges() {
        assertTrue(AccelerometerRange.RANGE_2G.getRawValue() == 2);
        assertTrue(AccelerometerRange.RANGE_4G.getRawValue() == 4);
        assertTrue(AccelerometerRange.RANGE_8G.getRawValue() == 8);
        assertTrue(AccelerometerRange.RANGE_16G.getRawValue() == 16);
    }

}
