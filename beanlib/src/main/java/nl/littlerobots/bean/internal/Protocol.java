/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Little Robots
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package nl.littlerobots.bean.internal;

public interface Protocol {
    int APP_MSG_RESPONSE_BIT = 0x80;

    int MSG_ID_SERIAL_DATA = 0x0000;
    int MSG_ID_BT_SET_ADV = 0x0500;
    int MSG_ID_BT_SET_CONN = 0x0502;
    int MSG_ID_BT_SET_LOCAL_NAME = 0x0504;
    int MSG_ID_BT_SET_PIN = 0x0506;
    int MSG_ID_BT_SET_TX_PWR = 0x0508;
    int MSG_ID_BT_GET_CONFIG = 0x0510;
    int MSG_ID_BT_SET_CONFIG = 0x0511;
    int MSG_ID_BT_SET_CONFIG_NOSAVE = 0x0540;
    int MSG_ID_BT_END_GATE = 0x0550;
    int MSG_ID_BT_ADV_ONOFF = 0x0512;
    int MSG_ID_BT_SET_SCRATCH = 0x0514;
    int MSG_ID_BT_GET_SCRATCH = 0x0515;
    int MSG_ID_BT_RESTART = 0x0520;
    int MSG_ID_BL_CMD_START = 0x1000;
    int MSG_ID_BL_FW_BLOCK = 0x1001;
    int MSG_ID_BL_STATUS = 0x1002;
    int MSG_ID_BL_GET_META = 0x1003;
    int MSG_ID_CC_LED_WRITE = 0x2000;
    int MSG_ID_CC_LED_WRITE_ALL = 0x2001;
    int MSG_ID_CC_LED_READ_ALL = 0x2002;
    int MSG_ID_CC_ACCEL_READ = 0x2010;
    int MSG_ID_CC_TEMP_READ = 0x2011;
    int MSG_ID_CC_BATT_READ = 0x2015;
    int MSG_ID_CC_POWER_ARDUINO = 0x2020;
    int MSG_ID_CC_GET_AR_POWER = 0x2021;
    int MSG_ID_CC_ACCEL_GET_RANGE = 0x2030;
    int MSG_ID_CC_ACCEL_SET_RANGE = 0x2035;
    int MSG_ID_AR_SLEEP = 0x3000;
    int MSG_ID_ERROR_CC = 0x4000;
    int MSG_ID_DB_LOOPBACK = 0xFE00;
    int MSG_ID_DB_COUNTER = 0xFE01;
    int MSG_ID_DB_E2E_LOOPBACK = 0xFE02;
    int MSG_ID_DB_PTM = 0xFE03;

}
