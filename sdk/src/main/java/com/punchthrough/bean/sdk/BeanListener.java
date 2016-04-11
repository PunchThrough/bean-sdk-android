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

package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;

/**
 * A Listener interface for communicating with the Bean.
 */
public interface BeanListener {

    /**
     * Called when the Bean is connected. Connected means that a Bluetooth GATT connection is made
     * and the setup for the Bean serial protocol is complete.
     */
    public void onConnected();

    /**
     * Called when the connection could not be established. This could either be because the Bean
     * could not be connected, or the serial connection could not be established.
     */
    public void onConnectionFailed();

    /**
     * Called when the Bean has been disconnected.
     */
    public void onDisconnected();

    /**
     * Called when a serial message is received from the Bean, e.g. a <code>Serial.write()</code>
     * from Arduino code.
     *
     * @param data the data that was sent from th bean
     */
    public void onSerialMessageReceived(byte[] data);

    /**
     * Called when one of the scratch characteristics of the Bean has updated its value.
     *
     * @param bank  the {@link com.punchthrough.bean.sdk.message.ScratchBank} that was updated
     * @param value the bank's new value
     */
    public void onScratchValueChanged(ScratchBank bank, byte[] value);

    /**
     * Called when an error occurs during sketch or firmware upload.
     *
     * @param error The {@link com.punchthrough.bean.sdk.message.BeanError} that occurred
     */
    public void onError(BeanError error);

    /**
     * Called when a new RSSI value is received, in response to a previous call to
     * {@link Bean#readRemoteRssi()}.
     *
     * @param rssi The RSSI for a connected remote device.
     */
    public void onReadRemoteRssi(int rssi);
}
