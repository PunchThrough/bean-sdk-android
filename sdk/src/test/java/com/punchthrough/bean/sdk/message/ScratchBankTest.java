package com.punchthrough.bean.sdk.message;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScratchBankTest {

    @Test
    public void testBankRawNumbers() {
        assertThat(ScratchBank.BANK_1.getRawValue()).isEqualTo(1);
        assertThat(ScratchBank.BANK_2.getRawValue()).isEqualTo(2);
        assertThat(ScratchBank.BANK_3.getRawValue()).isEqualTo(3);
        assertThat(ScratchBank.BANK_4.getRawValue()).isEqualTo(4);
        assertThat(ScratchBank.BANK_5.getRawValue()).isEqualTo(5);
    }

}
