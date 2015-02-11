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

import com.punchthrough.bean.sdk.BuildConfig;

import java.io.IOException;

import okio.Buffer;

public class MessageAssembler {
    private static final String TAG = "MessageAssembler";
    private Buffer mBuffer = new Buffer();
    private int mMessageIndex;
    private boolean mFirstPacket = true;
    private int mPacketIndex;

    public byte[] assemble(GattSerialPacket packet) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "assemble: First packet = " + packet.isFirstPacket() + ", index = " + packet.getMessageCount() + " pending = " + packet.getPendingCount());
        }
        if (packet.isFirstPacket()) {
            if (mFirstPacket) {
                mFirstPacket = false;
            } else if (packet.getMessageCount() != ((++mMessageIndex) % 4)) {
                Log.w(TAG, "Message count is out of sequence " + packet.getMessageCount() + " vs " + mMessageIndex);
            }
            mMessageIndex = packet.getMessageCount();
            mPacketIndex = packet.getPendingCount();
        } else {
            if (packet.getMessageCount() != mMessageIndex) {
                throw new IllegalStateException("Unexpected message count " + packet.getMessageCount() + ", expected " + mMessageIndex);
            }
            mPacketIndex--;
            if (packet.getPendingCount() != mPacketIndex) {
                throw new IllegalStateException("Unexpected pending count " + packet.getPendingCount() + ", expected " + mPacketIndex);
            }
        }

        if (packet.isFirstPacket() && mBuffer.size() > 0) {
            throw new IllegalStateException("Received first packet while trying to assemble previous packets");
        } else if (!packet.isFirstPacket() && mBuffer.size() == 0) {
            throw new IllegalStateException("Received non start packet without any data");
        }
        writeToBuffer(packet.getPayload());
        if (packet.getPendingCount() == 0) {
            // we're done, this will also clear the buffer
            return GattSerialMessage.parse(mBuffer.readByteArray());
        }
        // not yet done
        return null;
    }

    private void writeToBuffer(Buffer source) {
        try {
            mBuffer.writeAll(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        mFirstPacket = true;
    }
}
