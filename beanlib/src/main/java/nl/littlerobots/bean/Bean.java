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

package nl.littlerobots.bean;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.littlerobots.bean.internal.serial.GattSerialMessage;
import nl.littlerobots.bean.internal.serial.GattSerialTransport;
import nl.littlerobots.bean.internal.serial.GattSerialTransport.Listener;
import nl.littlerobots.bean.message.Acceleration;
import nl.littlerobots.bean.message.Callback;
import nl.littlerobots.bean.message.Led;
import nl.littlerobots.bean.message.Message;
import nl.littlerobots.bean.message.RadioConfig;
import nl.littlerobots.bean.message.ScratchData;
import nl.littlerobots.bean.message.SketchMetaData;
import okio.Buffer;

import static nl.littlerobots.bean.internal.Protocol.APP_MSG_RESPONSE_BIT;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_BL_GET_META;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_BT_ADV_ONOFF;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_BT_GET_CONFIG;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_BT_GET_SCRATCH;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_BT_SET_CONFIG;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_BT_SET_SCRATCH;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_CC_ACCEL_READ;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_CC_LED_READ_ALL;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_CC_LED_WRITE_ALL;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_CC_TEMP_READ;
import static nl.littlerobots.bean.internal.Protocol.MSG_ID_SERIAL_DATA;

public class Bean implements Parcelable {
    public static final Creator<Bean> CREATOR = new Creator<Bean>() {
        @Override
        public Bean createFromParcel(Parcel source) {
            // ugly cast to fix bogus warning in Android Studio...
            BluetoothDevice device = source.readParcelable(((Object) this).getClass().getClassLoader());
            if (device == null) {
                throw new IllegalStateException("Device is null");
            }
            return new Bean(device);
        }

        @Override
        public Bean[] newArray(int size) {
            return new Bean[size];
        }
    };
    private static final String TAG = "Bean";
    private BeanListener mInternalBeanListener = new BeanListener() {
        @Override
        public void onConnected() {
            Log.w(TAG, "onConnected after disconnect from device " + getDevice().getAddress());
        }

        @Override
        public void onConnectionFailed() {
            Log.w(TAG, "onConnectionFailed after disconnect from device " + getDevice().getAddress());
        }

        @Override
        public void onDisconnected() {
            Log.w(TAG, "onDisconnected after disconnect from device " + getDevice().getAddress());
        }

        @Override
        public void onSerialMessageReceived(byte[] data) {
            Log.w(TAG, "onSerialMessageReceived after disconnect from device " + getDevice().getAddress());
        }
    };
    private BeanListener mBeanListener = mInternalBeanListener;
    private final BluetoothDevice mDevice;
    private GattSerialTransport mTransport;
    private boolean mConnected;
    private HashMap<Integer, List<Callback<?>>> mCallbacks = new HashMap<>(16);
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public Bean(BluetoothDevice device) {
        mDevice = device;
        Listener transportListener = new Listener() {
            @Override
            public void onConnected() {
                mConnected = true;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mBeanListener.onConnected();
                    }
                });
            }

            @Override
            public void onConnectionFailed() {
                mConnected = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mBeanListener.onConnectionFailed();
                    }
                });
            }

            @Override
            public void onDisconnected() {
                mCallbacks.clear();
                mConnected = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mBeanListener.onDisconnected();
                    }
                });
            }

            @Override
            public void onMessageReceived(final byte[] data) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleMessage(data);
                    }
                });
            }
        };
        mTransport = new GattSerialTransport(transportListener, device);
    }

    public boolean isConnected() {
        return mConnected;
    }

    public void connect(Context context, BeanListener listener) {
        if (mConnected) {
            return;
        }
        mBeanListener = listener;
        mTransport.connect(context);
    }

    public void disconnect() {
        mTransport.disconnect();
        mBeanListener = mInternalBeanListener;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void readRadioConfig(Callback<RadioConfig> callback) {
        addCallback(MSG_ID_BT_GET_CONFIG, callback);
        sendMessageWithoutPayload(MSG_ID_BT_GET_CONFIG);
    }

    public void setLed(int r, int g, int b) {
        Buffer buffer = new Buffer();
        buffer.writeByte(r);
        buffer.writeByte(g);
        buffer.writeByte(b);
        sendMessage(MSG_ID_CC_LED_WRITE_ALL, buffer);
    }

    public void readLed(Callback<Led> callback) {
        addCallback(MSG_ID_CC_LED_READ_ALL, callback);
        sendMessageWithoutPayload(MSG_ID_CC_LED_READ_ALL);
    }

    public void setAdvertising(boolean enable) {
        Buffer buffer = new Buffer();
        buffer.writeByte(enable ? 1 : 0);
        sendMessage(MSG_ID_BT_ADV_ONOFF, buffer);
    }

    public void readTemperature(Callback<Integer> callback) {
        addCallback(MSG_ID_CC_TEMP_READ, callback);
        sendMessageWithoutPayload(MSG_ID_CC_TEMP_READ);
    }

    public void readAcceleration(Callback<Acceleration> callback) {
        addCallback(MSG_ID_CC_ACCEL_READ, callback);
        sendMessageWithoutPayload(MSG_ID_CC_ACCEL_READ);
    }

    public void readSketchMetaData(Callback<SketchMetaData> callback) {
        addCallback(MSG_ID_BL_GET_META, callback);
        sendMessageWithoutPayload(MSG_ID_BL_GET_META);
    }

    public void readScratchData(int number, Callback<ScratchData> callback) {
        addCallback(MSG_ID_BT_GET_SCRATCH, callback);
        Buffer buffer = new Buffer();
        if (number < 0 || number > 5) {
            throw new IllegalArgumentException("Scratch bank must be in the range of 1-5");
        }
        buffer.writeByte(number & 0xff);
        sendMessage(MSG_ID_BT_GET_SCRATCH, buffer);
    }

    public void setScratchData(int number, byte[] data) {
        ScratchData sd = ScratchData.create(number, data);
        sendMessage(MSG_ID_BT_SET_SCRATCH, sd);
    }

    public void setScratchData(int number, String data) {
        ScratchData sd = ScratchData.create(number, data);
        sendMessage(MSG_ID_BT_SET_SCRATCH, sd);
    }

    public void updateRadioConfig(RadioConfig config) {
        sendMessage(MSG_ID_BT_SET_CONFIG, config);
    }

    public void sendSerialMessage(byte[] value) {
        Buffer buffer = new Buffer();
        buffer.write(value);
        sendMessage(MSG_ID_SERIAL_DATA, buffer);
    }

    public void sendSerialMessage(String value) {
        Buffer buffer = new Buffer();
        try {
            buffer.write(value.getBytes("UTF-8"));
            sendMessage(MSG_ID_SERIAL_DATA, buffer);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(byte[] data) {
        Buffer buffer = new Buffer();
        buffer.write(data);
        int type = (buffer.readShort() & 0xffff) & ~(APP_MSG_RESPONSE_BIT);
        switch (type) {
            case MSG_ID_SERIAL_DATA:
                mBeanListener.onSerialMessageReceived(buffer.readByteArray());
                break;
            case MSG_ID_BT_GET_CONFIG:
                returnConfig(buffer);
                break;
            case MSG_ID_CC_TEMP_READ:
                returnTemperature(buffer);
                break;
            case MSG_ID_BL_GET_META:
                returnMetaData(buffer);
                break;
            case MSG_ID_BT_GET_SCRATCH:
                returnScratchData(buffer);
                break;
            case MSG_ID_CC_LED_READ_ALL:
                returnLed(buffer);
                break;
            case MSG_ID_CC_ACCEL_READ:
                returnAcceleration(buffer);
                break;
            default:
                Log.e(TAG, "Received message of unknown type " + Integer.toHexString(type));
                disconnect();
                break;
        }
    }

    private void returnAcceleration(Buffer buffer) {
        Callback<Acceleration> callback = getFirstCallback(MSG_ID_CC_ACCEL_READ);
        if (callback != null) {
            callback.onResult(Acceleration.fromPayload(buffer));
        }
    }

    private void returnLed(Buffer buffer) {
        Callback<Led> callback = getFirstCallback(MSG_ID_CC_LED_READ_ALL);
        if (callback != null) {
            callback.onResult(Led.fromPayload(buffer));
        }
    }

    private void returnScratchData(Buffer buffer) {
        Callback<ScratchData> callback = getFirstCallback(MSG_ID_BT_GET_SCRATCH);
        if (callback != null) {
            callback.onResult(ScratchData.fromPayload(buffer));
        }
    }

    private void returnMetaData(Buffer buffer) {
        Callback<SketchMetaData> callback = getFirstCallback(MSG_ID_BL_GET_META);
        if (callback != null) {
            callback.onResult(SketchMetaData.fromPayload(buffer));
        }
    }

    private void returnTemperature(Buffer buffer) {
        Callback<Integer> callback = getFirstCallback(MSG_ID_CC_TEMP_READ);
        if (callback != null) {
            callback.onResult((int) buffer.readByte());
        }
    }

    private void returnConfig(Buffer data) {
        RadioConfig config = RadioConfig.fromPayload(data);
        Callback<RadioConfig> callback = getFirstCallback(MSG_ID_BT_GET_CONFIG);
        if (callback != null) {
            callback.onResult(config);
        }
    }

    private void addCallback(int type, Callback<?> callback) {
        List<Callback<?>> callbacks = mCallbacks.get(type);
        if (callbacks == null) {
            callbacks = new ArrayList<>(16);
            mCallbacks.put(type, callbacks);
        }
        callbacks.add(callback);
    }

    @SuppressWarnings("unchecked")
    private <T> Callback<T> getFirstCallback(int type) {
        List<Callback<?>> callbacks = mCallbacks.get(type);
        if (callbacks == null || callbacks.isEmpty()) {
            Log.w(TAG, "Got response without callback!");
            return null;
        }
        return (Callback<T>) callbacks.remove(0);
    }

    private void sendMessage(int type, Message message) {
        Buffer buffer = new Buffer();
        buffer.writeByte((type >> 8) & 0xff);
        buffer.writeByte(type & 0xff);
        buffer.write(message.toPayload());
        GattSerialMessage serialMessage = GattSerialMessage.fromPayload(buffer.readByteArray());
        mTransport.sendMessage(serialMessage.getBuffer());
    }

    private void sendMessage(int type, Buffer payload) {
        Buffer buffer = new Buffer();
        buffer.writeByte((type >> 8) & 0xff);
        buffer.writeByte(type & 0xff);
        if (payload != null) {
            try {
                buffer.writeAll(payload);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        GattSerialMessage serialMessage = GattSerialMessage.fromPayload(buffer.readByteArray());
        mTransport.sendMessage(serialMessage.getBuffer());
    }

    private void sendMessageWithoutPayload(int type) {
        sendMessage(type, (Buffer) null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mDevice, 0);
    }
}
