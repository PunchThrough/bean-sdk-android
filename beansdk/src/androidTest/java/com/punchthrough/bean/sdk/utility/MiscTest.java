package com.punchthrough.bean.sdk.utility;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.internal.MessageID;
import com.punchthrough.bean.sdk.internal.bootloader.BeanState;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;

import static com.punchthrough.bean.sdk.internal.utility.Misc.enumWithRawValue;
import static com.punchthrough.bean.sdk.internal.utility.Misc.intToByte;
import static org.assertj.core.api.Assertions.assertThat;

public class MiscTest extends AndroidTestCase {

    public void testIntToByte() {
        assertThat(intToByte(0xFF)).isEqualTo((byte) 0xFF);
    }

    public void testEnumWithRawInt() throws NoEnumFoundException {
        assertThat(enumWithRawValue(MessageID.class, 0x1001)).isEqualTo(MessageID.BL_FW_BLOCK);
    }

    public void testEnumWithRawByte() throws NoEnumFoundException {
        byte state = 5;
        assertThat(enumWithRawValue(BeanState.class, state)).isEqualTo(BeanState.COMPLETE);
    }

    public void testEnumWithUnparsableValue() {
        try {
            enumWithRawValue(MessageID.class, 0x9999);
            fail("Expected a NoEnumFound exception to be thrown when parsing an enum from an " +
                    "invalid value");
        } catch (NoEnumFoundException e) {
            assertThat(e).isNotNull();
        }
    }

}
