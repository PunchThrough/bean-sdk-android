package com.punchthrough.bean.sdk.message;

import android.test.AndroidTestCase;

public class AccelerometerRangeTest extends AndroidTestCase {

    public void testAccelRawRanges() {
        assertTrue(AccelerometerRange.RANGE_2G.getRawRange() == 2);
        assertTrue(AccelerometerRange.RANGE_4G.getRawRange() == 4);
        assertTrue(AccelerometerRange.RANGE_8G.getRawRange() == 8);
        assertTrue(AccelerometerRange.RANGE_16G.getRawRange() == 16);
    }

}
