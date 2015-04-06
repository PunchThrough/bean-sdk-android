package com.punchthrough.bean.sdk.internal.serial;

import android.test.AndroidTestCase;

import java.util.Arrays;

public class GattSerialMessageTest extends AndroidTestCase {

    public void testParseMessage() {
        byte[] payload = new byte[]{0x0, 0x0, 0x61};
        GattSerialMessage message = GattSerialMessage.fromPayload(payload);
        byte[] result = GattSerialMessage.parse(message.getBuffer().readByteArray());
        assertTrue(Arrays.equals(payload, result));
    }

    public void testParseMessage2() {
        byte[] payload = new byte[]{0x0, 0x0, 0x61};
        byte[] serial = new byte[]{3, 0, 0, 0, 97, 89, -125};

        byte[] result = GattSerialMessage.parse(serial);
        assertTrue(Arrays.equals(payload, result));
    }
}
