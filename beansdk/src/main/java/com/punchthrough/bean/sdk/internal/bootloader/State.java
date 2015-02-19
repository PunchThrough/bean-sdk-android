package com.punchthrough.bean.sdk.internal.bootloader;

import com.punchthrough.bean.sdk.internal.utility.RawValuable;

/* AppMessages.h -> BL_HL_STATE_T: Bootloader high-level states
 *                                 (NULL = 0x00, INIT = 0x01, READY = 0x02, etc.)
 * {
 *     BL_HL_STATE_NULL = 0x00,
 *     BL_HL_STATE_INIT,
 *     BL_HL_STATE_READY,
 *     BL_HL_STATE_PROGRAMMING,
 *     BL_HL_STATE_VERIFY,
 *     BL_HL_STATE_COMPLETE,
 *     BL_HL_STATE_ERROR
 * }
 */
public enum State implements RawValuable {
    NULL(0),
    INIT(1),
    READY(2),
    PROGRAMMING(3),
    VERIFY(4),
    COMPLETE(5),
    ERROR(6);

    private final int value;

    private State(final int value) {
        this.value = value;
    }

    public int getRawValue() {
        return value;
    }
}
