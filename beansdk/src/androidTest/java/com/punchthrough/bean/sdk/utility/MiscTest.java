package com.punchthrough.bean.sdk.utility;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.internal.BeanMessageID;
import com.punchthrough.bean.sdk.internal.bootloader.BeanState;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;

import java.nio.ByteOrder;

import static com.punchthrough.bean.sdk.internal.utility.Misc.enumWithRawValue;
import static com.punchthrough.bean.sdk.internal.utility.Misc.intArrayToByteArray;
import static com.punchthrough.bean.sdk.internal.utility.Misc.intToByte;
import static com.punchthrough.bean.sdk.internal.utility.Misc.intToUInt32;
import static org.assertj.core.api.Assertions.assertThat;

public class MiscTest extends AndroidTestCase {

    public void testIntToByte() {
        assertThat(intToByte(0xFF)).isEqualTo((byte) 0xFF);
    }

    public void testEnumWithRawInt() throws NoEnumFoundException {
        assertThat(enumWithRawValue(BeanMessageID.class, 0x1001)).isEqualTo(BeanMessageID.BL_FW_BLOCK);
    }

    public void testEnumWithRawByte() throws NoEnumFoundException {
        byte state = 5;
        assertThat(enumWithRawValue(BeanState.class, state)).isEqualTo(BeanState.COMPLETE);
    }

    public void testEnumWithUnparsableValue() {
        try {
            enumWithRawValue(BeanMessageID.class, 0x9999);
            fail("Expected a NoEnumFound exception to be thrown when parsing an enum from an " +
                    "invalid value");
        } catch (NoEnumFoundException e) {
            assertThat(e).isNotNull();
        }
    }

    public void testUnsigningInts() {

        assertThat(intToUInt32(0, ByteOrder.BIG_ENDIAN)).isEqualTo(intArrayToByteArray(
                new int[]{0x00, 0x00, 0x00, 0x00}));

        assertThat(intToUInt32(6400, ByteOrder.BIG_ENDIAN)).isEqualTo(intArrayToByteArray(
                new int[]{0x00, 0x00, 0x19, 0x00}));

        assertThat(intToUInt32(65535, ByteOrder.BIG_ENDIAN)).isEqualTo(intArrayToByteArray(
                new int[]{0x00, 0x00, 0xFF, 0xFF}));

        assertThat(intToUInt32(2147483647, ByteOrder.BIG_ENDIAN)).isEqualTo(intArrayToByteArray(
                new int[]{0x7F, 0xFF, 0xFF, 0xFF}));

        assertThat(intToUInt32(6400, ByteOrder.LITTLE_ENDIAN)).isEqualTo(intArrayToByteArray(
                new int[]{0x00, 0x19, 0x00, 0x00}));

        assertThat(intToUInt32(65535, ByteOrder.LITTLE_ENDIAN)).isEqualTo(intArrayToByteArray(
                new int[]{0xFF, 0xFF, 0x00, 0x00}));

    }

}
