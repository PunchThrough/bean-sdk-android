package com.punchthrough.bean.sdk.message;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LedColorTest {

    @Test
    public void testLedColorEquals() {
        LedColor color = LedColor.create(112, 113, 114);
        assertThat(color.equals(LedColor.create(112, 113, 114))).isTrue();
        assertThat(color.equals(LedColor.create(7, 8, 9))).isFalse();
    }

}
