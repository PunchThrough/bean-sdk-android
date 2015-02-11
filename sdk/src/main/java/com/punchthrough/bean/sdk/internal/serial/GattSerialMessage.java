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

package com.punchthrough.bean.sdk.internal.serial;

import android.util.Log;

import okio.Buffer;

public class GattSerialMessage {

    private static final String TAG = "GattSerialMessage";
    private final Buffer mBuffer;

    private GattSerialMessage(Buffer buffer) {
        this.mBuffer = buffer;
    }

    public static GattSerialMessage fromPayload(byte[] payload) {
        Buffer buffer = new Buffer();
        byte[] header = new byte[2];
        header[0] = (byte) (payload.length & 0xff);
        header[1] = 0;
        int crc = computeCRC16(header, 0, header.length);
        crc = computeCRC16(crc, payload, 0, payload.length);
        buffer.write(header);
        buffer.write(payload);
        buffer.writeByte(crc & 0xff);
        buffer.writeByte((crc >> 8) & 0xff);
        return new GattSerialMessage(buffer);
    }

    public static byte[] parse(byte[] payload) {
        int length = (payload[0] & 0xff);
        Buffer buffer = new Buffer();
        buffer.write(payload, 2, length);
        int crc = computeCRC16(payload, 0, payload.length - 2);
        int checkCrc = (((payload[payload.length - 1] & 0xff) << 8)) + (payload[payload.length - 2] & 0xff);
        if (crc != checkCrc) {
            Log.w(TAG, "Checksum failed");
            throw new IllegalStateException("Checksum mismatch");
        }
        return buffer.readByteArray();
    }

    static int computeCRC16(byte[] data, int offset, int length) {
        return computeCRC16(0xffff, data, offset, length);
    }

    static int computeCRC16(int startingCrc, byte[] data, int offset, int length) {

        int crc = (startingCrc & 0xffff);

        for (int i = offset; i < offset + length; i++) {
            crc = ((crc >> 8) & 0xffff) | ((crc << 8) & 0xffff);
            crc ^= data[i] & 0xff;
            crc &= 0xffff;
            crc ^= ((crc & 0xff) >> 4);
            crc &= 0xffff;
            crc ^= (crc << 8) << 4;
            crc &= 0xffff;
            crc ^= ((crc & 0xff) << 4) << 1;
            crc &= 0xffff;
        }

        return crc & 0xffff;
    }

    public Buffer getBuffer() {
        return mBuffer;
    }
}
