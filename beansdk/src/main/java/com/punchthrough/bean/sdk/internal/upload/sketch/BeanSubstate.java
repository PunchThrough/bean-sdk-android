package com.punchthrough.bean.sdk.internal.upload.sketch;

import com.punchthrough.bean.sdk.internal.utility.EnumParse;

/* AppMessages.h -> BL_STATE_T: Bootloader substates (lower-level than HL_STATE)
 *                              (INIT = 0x00, WRITE_ADDRESS = 0x01, etc.)
 * {
 *     BL_STATE_INIT = 0x00,
 *     BL_STATE_WRITE_ADDRESS,
 *     BL_STATE_WRITE_ADDRESS_ACK,
 *     BL_STATE_WRITE_CHUNK,
 *     BL_STATE_WRITE_CHUNK_ACK,
 *     BL_STATE_READ_ADDRESS,
 *     BL_STATE_READ_ADDRESS_ACK,
 *     BL_STATE_READ_CHUNK,
 *     BL_STATE_READ_CHUNK_ACK,
 *     BL_STATE_VERIFY,
 *     BL_STATE_DONE,
 *     BL_STATE_DONE_ACK,
 *
 *     BL_STATE_START,
 *     BL_STATE_START_ACK,
 *     BL_STATE_HELLO,
 *     BL_STATE_HELLO_ACK,
 *     BL_STATE_START_RSTAGAIN,
 *
 *     BL_STATE_DONE_RESET,
 *     BL_STATE_PROG_MODE,
 *     BL_STATE_PROG_MODE_ACK,
 *     BL_STATE_DEVICE_SIG,
 *     BL_STATE_DEVICE_SIG_ACK,
 *     BL_STATE_WRITE_CHUNK_TWO,
 *     BL_STATE_ERROR
 * }
 */
public enum BeanSubstate implements EnumParse.ParsableEnum {
    INIT(0x00),
    WRITE_ADDRESS(0x01),
    WRITE_ADDRESS_ACK(0x02),
    WRITE_CHUNK(0x03),
    WRITE_CHUNK_ACK(0x04),
    READ_ADDRESS(0x05),
    READ_ADDRESS_ACK(0x06),
    READ_CHUNK(0x07),
    READ_CHUNK_ACK(0x08),
    VERIFY(0x09),
    DONE(0x0A),
    DONE_ACK(0x0B),

    START(0x0C),
    START_ACK(0x0D),
    HELLO(0x0E),
    HELLO_ACK(0x0F),
    START_RSTAGAIN(0x10),

    DONE_RESET(0x11),
    PROG_MODE(0x12),
    PROG_MODE_ACK(0x13),
    DEVICE_SIG(0x14),
    DEVICE_SIG_ACK(0x15),
    WRITE_CHUNK_TWO(0x16),
    ERROR(0x17);

    private final int value;

    private BeanSubstate(final int value) {
        this.value = value;
    }

    public int getRawValue() {
        return value;
    }
}
