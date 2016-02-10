package com.punchthrough.bean.sdk.utility;

import com.punchthrough.bean.sdk.internal.BeanMessageID;
import com.punchthrough.bean.sdk.internal.upload.sketch.BeanState;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.internal.utility.EnumParse;

import org.junit.Test;

import java.nio.ByteOrder;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intArrayToByteArray;
import static com.punchthrough.bean.sdk.internal.utility.Convert.intToByte;
import static com.punchthrough.bean.sdk.internal.utility.Convert.intToTwoBytes;
import static com.punchthrough.bean.sdk.internal.utility.Convert.intToUInt32;
import static com.punchthrough.bean.sdk.internal.utility.Convert.twoBytesToInt;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

public class MiscTest {

    @Test
    public void testIntToByte() {
        assertThat(intToByte(0xFF)).isEqualTo((byte) 0xFF);
    }

    @Test
    public void testEnumWithRawInt() throws NoEnumFoundException {
        assertThat(EnumParse.enumWithRawValue(BeanMessageID.class, 0x1001)).isEqualTo(BeanMessageID.BL_FW_BLOCK);
    }

    @Test
    public void testEnumWithRawByte() throws NoEnumFoundException {
        byte state = 5;
        assertThat(EnumParse.enumWithRawValue(BeanState.class, state)).isEqualTo(BeanState.COMPLETE);
    }

    @Test
    public void testEnumWithUnparsableValue() {
        try {
            EnumParse.enumWithRawValue(BeanMessageID.class, 0x9999);
            fail("Expected a NoEnumFound exception to be thrown when parsing an enum from an invalid value");
        } catch (NoEnumFoundException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
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

    @Test
    public void testParsingTwoBytesWithOrder() {

        assertThat(twoBytesToInt(
                intArrayToByteArray(new int[] {0x43, 0x0C}),
                ByteOrder.BIG_ENDIAN))
                .isEqualTo(17164);

        assertThat(twoBytesToInt(
                intArrayToByteArray(new int[] {0x0C, 0x43}),
                ByteOrder.LITTLE_ENDIAN))
                .isEqualTo(17164);

        assertThat(twoBytesToInt(
                intArrayToByteArray(new int[] {0x13, 0xB2}),
                ByteOrder.BIG_ENDIAN))
                .isEqualTo(5042);

        assertThat(twoBytesToInt(
                intArrayToByteArray(new int[] {0xB2, 0x13}),
                ByteOrder.LITTLE_ENDIAN))
                .isEqualTo(5042);

    }

    @Test
    public void testIntToTwoBytes() {

        assertThat(intToTwoBytes(17164, ByteOrder.BIG_ENDIAN))
                .isEqualTo(intArrayToByteArray(new int[] {0x43, 0x0C}));

        assertThat(intToTwoBytes(17164, ByteOrder.LITTLE_ENDIAN))
                .isEqualTo(intArrayToByteArray(new int[] {0x0C, 0x43}));

        assertThat(intToTwoBytes(5042, ByteOrder.BIG_ENDIAN))
                .isEqualTo(intArrayToByteArray(new int[] {0x13, 0xB2}));

        assertThat(intToTwoBytes(5042, ByteOrder.LITTLE_ENDIAN))
                .isEqualTo(intArrayToByteArray(new int[] {0xB2, 0x13}));

    }

}
