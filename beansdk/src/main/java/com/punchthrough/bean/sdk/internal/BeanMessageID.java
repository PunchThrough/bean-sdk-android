package com.punchthrough.bean.sdk.internal;

import com.punchthrough.bean.sdk.internal.utility.EnumParse;

// Enum as int technique from http://stackoverflow.com/a/3990421/254187

/**
 * Represents commands to be sent to/received from the Bean.
 */
public enum BeanMessageID implements EnumParse.ParsableEnum {
    SERIAL_DATA(0x0000),
    BT_SET_ADV(0x0500),
    BT_SET_CONN(0x0502),
    BT_SET_LOCAL_NAME(0x0504),
    BT_SET_PIN(0x0506),
    BT_SET_TX_PWR(0x0508),
    BT_GET_CONFIG(0x0510),
    BT_SET_CONFIG(0x0511),
    BT_SET_CONFIG_NOSAVE(0x0540),
    BT_END_GATE(0x0550),
    BT_ADV_ONOFF(0x0512),
    BT_SET_SCRATCH(0x0514),
    BT_GET_SCRATCH(0x0515),
    BT_RESTART(0x0520),
    BL_CMD_START(0x1000),
    BL_FW_BLOCK(0x1001),
    BL_STATUS(0x1002),
    BL_GET_META(0x1003),
    CC_LED_WRITE(0x2000),
    CC_LED_WRITE_ALL(0x2001),
    CC_LED_READ_ALL(0x2002),
    CC_ACCEL_READ(0x2010),
    CC_TEMP_READ(0x2011),
    CC_BATT_READ(0x2015),
    CC_POWER_ARDUINO(0x2020),
    CC_GET_AR_POWER(0x2021),
    CC_ACCEL_GET_RANGE(0x2030),
    CC_ACCEL_SET_RANGE(0x2035),
    AR_SLEEP(0x3000),
    ERROR_CC(0x4000),
    DB_LOOPBACK(0xFE00),
    DB_COUNTER(0xFE01),
    DB_E2E_LOOPBACK(0xFE02),
    DB_PTM(0xFE03);

    private final int value;

    private BeanMessageID(final int value) {
        this.value = value;
    }

    public int getRawValue() {
        return value;
    }
}
