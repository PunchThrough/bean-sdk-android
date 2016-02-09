package com.punchthrough.bean.sdk.bootloader;

import com.punchthrough.bean.sdk.internal.upload.sketch.BeanState;
import com.punchthrough.bean.sdk.internal.upload.sketch.BeanSubstate;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.message.Status;

import org.junit.Test;

import okio.Buffer;

import static org.assertj.core.api.Assertions.assertThat;

public class StatusTest {

    @Test
    public void testStatusFromPayload() throws NoEnumFoundException {
        Buffer buffer = new Buffer();

        buffer.writeByte(0x04);  // State: VERIFY

        buffer.writeByte(0x0E);  // Substate: HELLO

        buffer.writeByte(0x12);  // Blocks sent: 4616
        buffer.writeByte(0x08);

        buffer.writeByte(0x34);  // Bytes sent: 13393
        buffer.writeByte(0x51);

        Status status = Status.fromPayload(buffer);
        assertThat(status.beanState()).isEqualTo(BeanState.VERIFY);
        assertThat(status.beanSubstate()).isEqualTo(BeanSubstate.HELLO);
        assertThat(status.blocksSent()).isEqualTo(4616);
        assertThat(status.bytesSent()).isEqualTo(13393);
    }

}
