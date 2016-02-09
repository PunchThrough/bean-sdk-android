package com.punchthrough.bean.sdk.message;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AccelerometerRangeTest {

    @Test
    public void testAccelRawRanges() {
        assertThat(AccelerometerRange.RANGE_4G.getRawValue()).isEqualTo(4);
        assertThat(AccelerometerRange.RANGE_8G.getRawValue()).isEqualTo(8);
        assertThat(AccelerometerRange.RANGE_2G.getRawValue()).isEqualTo(2);
        assertThat(AccelerometerRange.RANGE_16G.getRawValue() ).isEqualTo(16);
    }

}
