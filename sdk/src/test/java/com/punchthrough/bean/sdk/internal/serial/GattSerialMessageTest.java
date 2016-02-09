package com.punchthrough.bean.sdk.internal.serial;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class GattSerialMessageTest {

    @Test
    public void testParseMessage() {
        byte[] payload = new byte[]{0x0, 0x0, 0x61};
        GattSerialMessage message = GattSerialMessage.fromPayload(payload);
        byte[] result = GattSerialMessage.parse(message.getBuffer().readByteArray());
        assertThat(Arrays.equals(payload, result)).isTrue();
    }

    @Test
    public void testParseMessage2() {
        byte[] payload = new byte[]{0x0, 0x0, 0x61};
        byte[] serial = new byte[]{3, 0, 0, 0, 97, 89, -125};
        byte[] result = GattSerialMessage.parse(serial);
        assertThat(Arrays.equals(payload, result)).isTrue();
    }
}
