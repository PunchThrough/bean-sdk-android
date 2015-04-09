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

import java.io.IOException;

import okio.Buffer;

import static com.punchthrough.bean.sdk.internal.serial.GattSerialTransportProfile.PACKET_TX_MAX_PAYLOAD_LENGTH;

public class GattSerialPacket {
    private final boolean mFirstPacket;
    private final int mMessageCount;
    private final int mPendingCount;
    private byte[] mPacket;

    public GattSerialPacket(boolean startBit, int outgoingMessageCount, int pendingPacketCount, Buffer message) {
        mFirstPacket = startBit;
        mMessageCount = outgoingMessageCount;
        mPendingCount = pendingPacketCount;

        Buffer buffer = new Buffer();
        buffer.writeByte((startBit ? 0x80 : 0) | ((outgoingMessageCount << 5) & 0x60) | ((pendingPacketCount & 0x1f)));
        int size = (int) Math.min(PACKET_TX_MAX_PAYLOAD_LENGTH, message.size());
        try {
            message.readFully(buffer, size);
            mPacket = buffer.readByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GattSerialPacket(byte[] data) {
        mFirstPacket = (data[0] & 0x80) == 0x80;
        mMessageCount = (((data[0] & 0x60) >> 5));
        mPendingCount = (data[0] & 0x1f);
        mPacket = data;
    }

    public byte[] getPacketData() {
        return mPacket;
    }

    public boolean isFirstPacket() {
        return mFirstPacket;
    }

    public int getMessageCount() {
        return mMessageCount;
    }

    public int getPendingCount() {
        return mPendingCount;
    }

    public Buffer getPayload() {
        Buffer buffer = new Buffer();
        buffer.write(mPacket, 1, mPacket.length - 1);
        return buffer;
    }

    public boolean hasPayload() {
        return mPacket.length > 1;
    }
}
