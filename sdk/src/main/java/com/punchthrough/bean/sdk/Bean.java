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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.punchthrough.bean.sdk.internal.BeanMessageID;
import com.punchthrough.bean.sdk.internal.battery.BatteryProfile.BatteryLevelCallback;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.device.DeviceProfile;
import com.punchthrough.bean.sdk.internal.device.DeviceProfile.DeviceInfoCallback;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.internal.serial.GattSerialMessage;
import com.punchthrough.bean.sdk.internal.serial.GattSerialTransportProfile;
import com.punchthrough.bean.sdk.internal.upload.firmware.OADProfile;
import com.punchthrough.bean.sdk.internal.upload.sketch.BeanState;
import com.punchthrough.bean.sdk.internal.upload.sketch.SketchUploadState;
import com.punchthrough.bean.sdk.internal.utility.Chunk;
import com.punchthrough.bean.sdk.message.Acceleration;
import com.punchthrough.bean.sdk.message.AccelerometerRange;
import com.punchthrough.bean.sdk.message.BatteryLevel;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.LedColor;
import com.punchthrough.bean.sdk.message.Message;
import com.punchthrough.bean.sdk.message.RadioConfig;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.message.ScratchData;
import com.punchthrough.bean.sdk.message.SketchMetadata;
import com.punchthrough.bean.sdk.message.Status;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.FirmwareBundle;
import com.punchthrough.bean.sdk.upload.SketchHex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okio.Buffer;

import static com.punchthrough.bean.sdk.internal.Protocol.APP_MSG_RESPONSE_BIT;
import static com.punchthrough.bean.sdk.internal.utility.Convert.intToByte;

/**
 * Represents a physical Bean.
 */
public class Bean implements Parcelable {

    /**
     * Used by Android {@link android.os.Parcel}.
     */
    public static final Creator<Bean> CREATOR = new Creator<Bean>() {

        @Override
        public Bean createFromParcel(Parcel source) {
            Log.i(TAG, "Creating Bean from Parcel!");

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

    /**
     * TAG = "BeanSDK". Used for debug messages.
     */
    private static final String TAG = "BeanSDK";

    /**
     * The default {@link com.punchthrough.bean.sdk.BeanListener}. Replaced by the listener passed
     * into
     * {@link com.punchthrough.bean.sdk.Bean#connect(android.content.Context, BeanListener)}.
     * Provides warning messages when connections occur or fail in an invalid state.
     */
    private BeanListener internalBeanListener = new BeanListener() {
        @Override
        public void onConnected() {
            Log.w(TAG, "onConnected after disconnect from device " + getDevice().getAddress());
        }

        @Override
        public void onConnectionFailed() {
            Log.w(TAG, "onConnectionFailed after disconnect from device " +
                    getDevice().getAddress());
        }

        @Override
        public void onDisconnected() {}

        @Override
        public void onSerialMessageReceived(byte[] data) {}

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {}

        @Override
        public void onError(BeanError error) {
            Log.e(TAG, "Bean returned error: " + error);
        }

        @Override
        public void onReadRemoteRssi(final int rssi) {}
    };

    /**
     * The {@link com.punchthrough.bean.sdk.BeanListener} that provides data back to the class that
     * sets up the Bean object. Passed into
     * {@link com.punchthrough.bean.sdk.Bean#connect(android.content.Context, BeanListener)}.
     */
    private BeanListener beanListener = internalBeanListener;

    /**
     * The GattClient associated with this Bean.
     */
    private final GattClient gattClient;

    /**
     * The BluetoothDevice representing this physical Bean.
     */
    private final BluetoothDevice device;

    /**
     * Handler created for each Bean
     */
    private final Handler handler;

    /**
     * Last known Android Context (Activity)
     */
    private Context lastKnownContext;

    /**
     * <p>
     * Holds callbacks initiated by messages coming from the Bean.
     *
     * </p><p>
     *
     * The handling of callbacks is somewhat complex. Here's how they work:
     *
     * </p><p>
     *
     * 1. Client:
     * <pre>
     * bean.readLed(Callback<LedColor> cb { ... })
     * </pre>
     *
     * </p><p>
     *
     * 2. Bean class:
     * <pre>
     * cbList = beanCallbacks.get(readLed) - or create if cbList doesn't exist yet
     * cbList.add(cb)
     * </pre>
     *
     * </p><p>
     *
     * 3. On data from Bean, ledResult:
     * <pre>
     * cb = cbList.remove(0) - cbList is a FIFO queue, so that two readLed() calls are called
     *                         back in order when their results appear
     * cb.onResult(ledResult)
     * </pre>
     *
     * </p><p>
     *
     * This is to handle the case where, for example, <code>readLed()</code> is called, then
     * <code>readLed()</code> is called
     * again before results are returned for the first call. If the callbacks map were a simple
     * <code>Map&lt;MessageID, Callback&lt;?&gt;&gt;</code>, the first callback would never receive
     * its data and the second callback would get called twice.
     *
     * </p><p>
     *
     * By using a <code>Map&lt;MessageID, List&lt;Callback&lt;?&gt;&gt;</code>, we can handle each
     * call in order and guarantee each callback is called exactly once.
     * </p>
     */
    private HashMap<BeanMessageID, List<Callback<?>>> beanCallbacks = new HashMap<>(16);


    // These class variables are used for sketch uploads.
    /**
     * The maximum amount of time, in ms, that passes between state updates from the Bean before
     * sketch upload process is aborted
     */
    private static final int SKETCH_UPLOAD_STATE_TIMEOUT = 3000;

    /**
     * The time, in ms, between block being sent. Blocks are sent to the Bean without waiting for
     * acks, so setting this value lower will accelerate sketch programming. Setting this value too
     * low will send blocks faster than BLE/the Bean can handle them.
     */
    private static final int SKETCH_BLOCK_SEND_INTERVAL = 200;

    /**
     * The maximum sketch block size. The last block may be smaller.
     */
    private static final int MAX_BLOCK_SIZE_BYTES = 64;

    /**
     * State of the current sketch upload process.
     */
    private SketchUploadState sketchUploadState = SketchUploadState.INACTIVE;

    /**
     * sketchStateTimeout throws an error if too much time passes without an update from the Bean
     * asking programming to begin
     */
    private Timer sketchStateTimeout;

    /**
     * Sends the next block of sketch data
     */
    private Timer sketchBlockSendTimeout;

    /**
     * Holds all blocks of sketch data being sent to Bean
     */
    private List<byte[]> sketchBlocksToSend;

    /**
     * Index of the next sketch block to be sent to Bean
     */
    private int currSketchBlockNum;

    /**
     * Passes in an UploadProgress object when progress is made in the sketch upload process
     */
    private Callback<UploadProgress> onSketchUploadProgress;

    /**
     * Called when the sketch upload process completes successfully
     */
    private Runnable onSketchUploadComplete;

    /**
     * Create a Bean using its {@link android.bluetooth.BluetoothDevice}
     * The Bean will not be connected until {@link #connect(android.content.Context, BeanListener)} is called.
     *
     * @param device The BluetoothDevice representing a Bean
     */
    public Bean(BluetoothDevice device) {
        this.device = device;
        this.handler = new Handler(Looper.getMainLooper());
        this.gattClient = new GattClient(handler, device);
        init();
    }

    /**
     * Alternate Bean constructor practicing dependency injection, currently used by tests only
     *
     */
    public Bean(BluetoothDevice device, GattClient client, final Handler handler) {
        this.device = device;
        this.gattClient = client;
        this.handler = handler;
        init();
    }

    private void init() {

        GattClient.ConnectionListener connectionListener = new GattClient.ConnectionListener() {
            @Override
            public void onConnected() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        beanListener.onConnected();
                    }
                });
            }

            @Override
            public void onConnectionFailed() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        beanListener.onConnectionFailed();
                    }
                });
            }

            @Override
            public void onDisconnected() {
                beanCallbacks.clear();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        beanListener.onDisconnected();
                    }
                });
            }
        };

        GattSerialTransportProfile.SerialListener serialListener = new GattSerialTransportProfile.SerialListener() {

            @Override
            public void onMessageReceived(final byte[] data) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleMessage(data);
                    }
                });
            }

            @Override
            public void onScratchValueChanged(final ScratchBank bank, final byte[] value) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        beanListener.onScratchValueChanged(bank, value);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, message);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        beanListener.onError(BeanError.GATT_SERIAL_TRANSPORT_ERROR);
                    }
                });
            }

            @Override
            public void onReadRemoteRssi(final int rssi) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        beanListener.onReadRemoteRssi(rssi);
                    }
                });
            }
        };

        gattClient.setListener(connectionListener);
        gattClient.getSerialProfile().setListener(serialListener);
    }

    /**
     * Handles incoming messages from the Bean and dispatches them to the proper handlers.
     * @param data The raw byte data received from the Bean
     */
    private void handleMessage(byte[] data) {
        Buffer buffer = new Buffer();
        buffer.write(data);
        int type = (buffer.readShort() & 0xffff) & ~(APP_MSG_RESPONSE_BIT);

        if (type == BeanMessageID.SERIAL_DATA.getRawValue()) {
            beanListener.onSerialMessageReceived(buffer.readByteArray());

        } else if (type == BeanMessageID.BT_GET_CONFIG.getRawValue()) {
            returnConfig(buffer);

        } else if (type == BeanMessageID.CC_TEMP_READ.getRawValue()) {
            returnTemperature(buffer);

        } else if (type == BeanMessageID.BL_GET_META.getRawValue()) {
            returnMetadata(buffer);

        } else if (type == BeanMessageID.BT_GET_SCRATCH.getRawValue()) {
            returnScratchData(buffer);

        } else if (type == BeanMessageID.CC_LED_READ_ALL.getRawValue()) {
            returnLed(buffer);

        } else if (type == BeanMessageID.CC_ACCEL_READ.getRawValue()) {
            returnAcceleration(buffer);

        } else if (type == BeanMessageID.CC_ACCEL_GET_RANGE.getRawValue()) {
            returnAccelerometerRange(buffer);

        // Ignore CC_LED_WRITE; it appears to be only an ack

        } else if (type == BeanMessageID.CC_GET_AR_POWER.getRawValue()) {
            returnArduinoPowerState(buffer);

        } else if (type == BeanMessageID.BL_STATUS.getRawValue()) {
            try {
                Status status = Status.fromPayload(buffer);
                handleStatus(status);

            } catch (NoEnumFoundException e) {
                Log.e(TAG, "Unable to parse status from buffer: " + buffer.toString());
                e.printStackTrace();

            }

        } else {
            String fourDigitHex = Integer.toHexString(type);
            while (fourDigitHex.length() < 4) {
                fourDigitHex = "0" + fourDigitHex;
            }
            Log.e(TAG, "Received message of unknown type 0x" + fourDigitHex);
            returnError(BeanError.UNKNOWN_MESSAGE_ID);

        }
    }

    /**
     * Fired when the Bean sends a Status message. Updates the client's internal state machine,
     * used for uploading sketches and firmware to the Bean.
     *
     * @param status The status received from the Bean
     */
    private void handleStatus(Status status) {

        Log.d(TAG, "Handling Bean status: " + status);

        BeanState beanState = status.beanState();

        if (beanState == BeanState.READY) {
            resetSketchStateTimeout();

            if (sketchUploadState == SketchUploadState.SENDING_START_COMMAND) {
                sketchUploadState = SketchUploadState.SENDING_BLOCKS;
                stopSketchStateTimeout();
                sendNextSketchBlock();

            }

        } else if (beanState == BeanState.PROGRAMMING) {
            resetSketchStateTimeout();

        } else if (beanState == BeanState.COMPLETE) {
            if (onSketchUploadComplete != null) onSketchUploadComplete.run();
            resetSketchUploadState();

        } else if (beanState == BeanState.ERROR) {
            returnUploadError(BeanError.UNKNOWN);
            resetSketchUploadState();

        }

    }

    /**
     * Cancel the state timeout timer and null it to indicate it is no longer running.
     */
    private void stopSketchStateTimeout() {
        if (sketchStateTimeout != null) {
            sketchStateTimeout.cancel();
            sketchStateTimeout = null;
        }
    }

    /**
     * Cancel the block send timer and null it to indicate it is no longer running.
     */
    private void stopSketchBlockSendTimeout() {
        if (sketchBlockSendTimeout != null) {
            sketchBlockSendTimeout.cancel();
            sketchBlockSendTimeout = null;
        }
    }

    /**
     * Reset the state timeout timer. If this timer fires, the client has waited too long for a
     * state update from the Bean and an error will be fired.
     */
    private void resetSketchStateTimeout() {
        TimerTask onTimeout = new TimerTask() {
            @Override
            public void run() {
                returnUploadError(BeanError.STATE_TIMEOUT);
            }
        };

        stopSketchStateTimeout();
        sketchStateTimeout = new Timer();
        sketchStateTimeout.schedule(onTimeout, SKETCH_UPLOAD_STATE_TIMEOUT);
    }

    /**
     * Reset the block send timer. When this timer fires, another sketch block is sent.
     */
    private void resetSketchBlockSendTimeout() {
        TimerTask onTimeout = new TimerTask() {
            @Override
            public void run() {
                sendNextSketchBlock();
            }
        };

        stopSketchBlockSendTimeout();
        sketchBlockSendTimeout = new Timer();
        sketchBlockSendTimeout.schedule(onTimeout, SKETCH_BLOCK_SEND_INTERVAL);
    }

    /**
     * Send one block of sketch data to the Bean and increment the block counter.
     */
    private void sendNextSketchBlock() {
        byte[] rawBlock = sketchBlocksToSend.get(currSketchBlockNum);
        Buffer block = new Buffer();
        block.write(rawBlock);
        sendMessage(BeanMessageID.BL_FW_BLOCK, block);

        resetSketchBlockSendTimeout();

        int blocksSent = currSketchBlockNum + 1;
        int totalBlocks = sketchBlocksToSend.size();
        onSketchUploadProgress.onResult(UploadProgress.create(blocksSent, totalBlocks));

        currSketchBlockNum++;
        if ( currSketchBlockNum >= sketchBlocksToSend.size() ) {
            resetSketchUploadState();
        }
    }

    /**
     * Reset local variables and kill timers that are used for uploading sketches.
     */
    private void resetSketchUploadState() {
        sketchBlocksToSend = null;
        currSketchBlockNum = 0;
        sketchUploadState = SketchUploadState.INACTIVE;
        stopSketchStateTimeout();
        stopSketchBlockSendTimeout();
    }

    /**
     * Call the onResult callback for {@link com.punchthrough.bean.sdk.Bean#readArduinoPowerState(com.punchthrough.bean.sdk.message.Callback)}.
     * @param buffer Raw message bytes from the Bean
     */
    private void returnArduinoPowerState(Buffer buffer) {
        Callback<Boolean> callback = getFirstCallback(BeanMessageID.CC_GET_AR_POWER);
        if (callback != null) {
            callback.onResult((buffer.readByte() & 0xff) == 1);
        }
    }

    /**
     * Call the onResult callback for {@link com.punchthrough.bean.sdk.Bean#readAccelerometerRange(com.punchthrough.bean.sdk.message.Callback)}.
     * @param buffer Raw message bytes from the Bean
     */
    private void returnAccelerometerRange(Buffer buffer) {
        Callback<Integer> callback = getFirstCallback(BeanMessageID.CC_ACCEL_GET_RANGE);
        if (callback != null) {
            callback.onResult(buffer.readByte() & 0xff);
        }
    }

    /**
     * Call the onResult callback for {@link com.punchthrough.bean.sdk.Bean#readAcceleration(com.punchthrough.bean.sdk.message.Callback)}.
     * @param buffer Raw message bytes from the Bean
     */
    private void returnAcceleration(Buffer buffer) {
        Callback<Acceleration> callback = getFirstCallback(BeanMessageID.CC_ACCEL_READ);
        if (callback != null) {
            callback.onResult(Acceleration.fromPayload(buffer));
        }
    }

    /**
     * Call the onResult callback for {@link com.punchthrough.bean.sdk.Bean#readLed(com.punchthrough.bean.sdk.message.Callback)}.
     * @param buffer Raw message bytes from the Bean
     */
    private void returnLed(Buffer buffer) {
        Callback<LedColor> callback = getFirstCallback(BeanMessageID.CC_LED_READ_ALL);
        if (callback != null) {
            callback.onResult(LedColor.fromPayload(buffer));
        }
    }

    /**
     * Call the onResult callback for {@link com.punchthrough.bean.sdk.Bean#readScratchData(com.punchthrough.bean.sdk.message.ScratchBank, com.punchthrough.bean.sdk.message.Callback)}.
     * @param buffer Raw message bytes from the Bean
     */
    private void returnScratchData(Buffer buffer) {
        Callback<ScratchData> callback = getFirstCallback(BeanMessageID.BT_GET_SCRATCH);
        if (callback != null) {
            callback.onResult(ScratchData.fromPayload(buffer));
        }
    }

    /**
     * Call the onResult callback for {@link com.punchthrough.bean.sdk.Bean#readSketchMetadata(com.punchthrough.bean.sdk.message.Callback)} (com.punchthrough.bean.sdk.message.Callback)}.
     * @param buffer Raw message bytes from the Bean
     */
    private void returnMetadata(Buffer buffer) {
        Callback<SketchMetadata> callback = getFirstCallback(BeanMessageID.BL_GET_META);
        if (callback != null) {
            callback.onResult(SketchMetadata.fromPayload(buffer));
        }
    }

    /**
     * Call the onResult callback for {@link com.punchthrough.bean.sdk.Bean#readTemperature(com.punchthrough.bean.sdk.message.Callback)} (com.punchthrough.bean.sdk.message.Callback)}.
     * @param buffer Raw message bytes from the Bean
     */
    private void returnTemperature(Buffer buffer) {
        Callback<Integer> callback = getFirstCallback(BeanMessageID.CC_TEMP_READ);
        if (callback != null) {
            callback.onResult((int) buffer.readByte());
        }
    }

    /**
     * Call the onResult callback for {@link com.punchthrough.bean.sdk.Bean#readRadioConfig(com.punchthrough.bean.sdk.message.Callback)}.
     * @param buffer Raw message bytes from the Bean
     */
    private void returnConfig(Buffer buffer) {
        RadioConfig config = RadioConfig.fromPayload(buffer);
        Callback<RadioConfig> callback = getFirstCallback(BeanMessageID.BT_GET_CONFIG);
        if (callback != null) {
            callback.onResult(config);
        }
    }

    /**
     * Call this to alert the client whenever any errors occur with a NON-upload task.
     * @param error The type of error that occurred
     */
    private void returnError(BeanError error) {
        resetSketchUploadState();
        beanListener.onError(error);
    }

    /**
     * Call this to alert the client whenever any errors occur with an upload task.
     * It resets the local upload state to clean the slate for future sketch/firmware uploads.
     *
     * @param error The type of error that occurred
     */
    private void returnUploadError(BeanError error) {
        resetSketchUploadState();
        returnError(error);
    }

    /**
     * Add a callback for a Bean message type.
     *
     * @param type      The {@link com.punchthrough.bean.sdk.internal.BeanMessageID} the callback
     *                  will answer to
     * @param callback  The callback to store
     */
    private void addCallback(BeanMessageID type, Callback<?> callback) {
        List<Callback<?>> callbacks = beanCallbacks.get(type);
        if (callbacks == null) {
            callbacks = new ArrayList<>(16);
            beanCallbacks.put(type, callbacks);
        }
        callbacks.add(callback);
    }

    /**
     * Get the first callback for a Bean message type.
     *
     * @param type  The {@link com.punchthrough.bean.sdk.internal.BeanMessageID} for which to find
     *              an associated callback
     * @param <T>   The parameter type for the callback
     * @return      The callback for the given message type, or null if none exists
     */
    @SuppressWarnings("unchecked")
    private <T> Callback<T> getFirstCallback(BeanMessageID type) {
        List<Callback<?>> callbacks = beanCallbacks.get(type);
        if (callbacks == null || callbacks.isEmpty()) {
            Log.w(TAG, "Got response without callback!");
            return null;
        }
        return (Callback<T>) callbacks.remove(0);
    }

    /**
     * Send a message to Bean with a payload.
     * @param type      The {@link com.punchthrough.bean.sdk.internal.BeanMessageID} for the message
     * @param message   The message payload to send
     */
    private void sendMessage(BeanMessageID type, Message message) {
        Buffer buffer = new Buffer();
        buffer.writeByte((type.getRawValue() >> 8) & 0xff);
        buffer.writeByte(type.getRawValue() & 0xff);
        buffer.write(message.toPayload());
        GattSerialMessage serialMessage = GattSerialMessage.fromPayload(buffer.readByteArray());
        gattClient.getSerialProfile().sendMessage(serialMessage.getBuffer());
    }

    /**
     * Send a message to Bean with a payload.
     * @param type      The {@link com.punchthrough.bean.sdk.internal.BeanMessageID} for the message
     * @param payload   The message payload to send
     */
    private void sendMessage(BeanMessageID type, Buffer payload) {
        Buffer buffer = new Buffer();
        buffer.writeByte((type.getRawValue() >> 8) & 0xff);
        buffer.writeByte(type.getRawValue() & 0xff);
        if (payload != null) {
            try {
                buffer.writeAll(payload);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        GattSerialMessage serialMessage = GattSerialMessage.fromPayload(buffer.readByteArray());
        gattClient.getSerialProfile().sendMessage(serialMessage.getBuffer());
    }

    /**
     * Send a message to Bean without a payload.
     * @param type The {@link com.punchthrough.bean.sdk.internal.BeanMessageID} for the message
     */
    private void sendMessageWithoutPayload(BeanMessageID type) {
        sendMessage(type, (Buffer) null);
    }

    /****************************************************************************
                                    PUBLIC API
     ****************************************************************************/

    public String describe() {
        return String.format("%s (%s)", getDevice().getName(), getDevice().getAddress());
    }

    public boolean isConnected() {
        return gattClient.isConnected();
    }

    public Context getLastKnownContext() {
        return lastKnownContext;
    }

    public BeanListener getBeanListener() {
        return beanListener;
    }

    /**
     * Attempt to connect to this Bean.
     *
     * @param context  the Android Context used for connection, usually the current activity
     * @param listener the Bean listener
     */
    public void connect(Context context, BeanListener listener) {
        lastKnownContext = context;
        beanListener = listener;
        gattClient.connect(context, device);
    }

    /**
     * Disconnect the Bean
     */
    public void disconnect() {
        gattClient.disconnect();
    }

    /**
     * Return the {@link android.bluetooth.BluetoothDevice} backing this Bean object
     *
     * @return the device
     */
    public BluetoothDevice getDevice() {
        return device;
    }

    /**
    *  Read the RSSI for a connected remote device. Value will be returned in {@link BeanListener#onReadRemoteRssi(int)}.
    */
    public void readRemoteRssi() {
        gattClient.readRemoteRssi();
    }

    /**
     * Request the {@link com.punchthrough.bean.sdk.message.RadioConfig}.
     *
     * @param callback the callback for the result
     */
    public void readRadioConfig(Callback<RadioConfig> callback) {
        addCallback(BeanMessageID.BT_GET_CONFIG, callback);
        sendMessageWithoutPayload(BeanMessageID.BT_GET_CONFIG);
    }

    /**
     * Set the LED color.
     *
     * @param color The {@link com.punchthrough.bean.sdk.message.LedColor} being sent to the LED
     */
    public void setLed(LedColor color) {
        Buffer buffer = new Buffer();
        buffer.writeByte(color.red());
        buffer.writeByte(color.green());
        buffer.writeByte(color.blue());
        sendMessage(BeanMessageID.CC_LED_WRITE_ALL, buffer);
    }

    /**
     * Read the LED color.
     *
     * @param callback the callback for the {@link com.punchthrough.bean.sdk.message.LedColor}
     *                 result
     */
    public void readLed(Callback<LedColor> callback) {
        addCallback(BeanMessageID.CC_LED_READ_ALL, callback);
        sendMessageWithoutPayload(BeanMessageID.CC_LED_READ_ALL);
    }

    /**
     * Set the advertising flag.
     *
     * @param enable true to enable, false to disable
     */
    public void setAdvertising(boolean enable) {
        Buffer buffer = new Buffer();
        buffer.writeByte(enable ? 1 : 0);
        sendMessage(BeanMessageID.BT_ADV_ONOFF, buffer);
    }

    /**
     * Request a temperature reading.
     *
     * @param callback the callback for the temperature result, in degrees Celsius
     */
    public void readTemperature(Callback<Integer> callback) {
        addCallback(BeanMessageID.CC_TEMP_READ, callback);
        sendMessageWithoutPayload(BeanMessageID.CC_TEMP_READ);
    }

    /**
     * Request an acceleration sensor reading.
     *
     * @param callback the callback for the {@link com.punchthrough.bean.sdk.message.Acceleration}
     *                 result
     */
    public void readAcceleration(Callback<Acceleration> callback) {
        addCallback(BeanMessageID.CC_ACCEL_READ, callback);
        sendMessageWithoutPayload(BeanMessageID.CC_ACCEL_READ);
    }

    /**
     * Request the sketch metadata.
     *
     * @param callback the callback for the {@link com.punchthrough.bean.sdk.message.SketchMetadata}
     *                 result
     */
    public void readSketchMetadata(Callback<SketchMetadata> callback) {
        addCallback(BeanMessageID.BL_GET_META, callback);
        sendMessageWithoutPayload(BeanMessageID.BL_GET_META);
    }

    /**
     * Request a scratch bank data value.
     *
     * @param bank     the {@link com.punchthrough.bean.sdk.message.ScratchBank} for which data is
     *                 being requested
     * @param callback the callback for the result
     */
    public void readScratchData(ScratchBank bank, Callback<ScratchData> callback) {
        addCallback(BeanMessageID.BT_GET_SCRATCH, callback);
        Buffer buffer = new Buffer();
        buffer.writeByte(intToByte(bank.getRawValue()));
        sendMessage(BeanMessageID.BT_GET_SCRATCH, buffer);
    }

    /**
     * Set the accelerometer range.
     *
     * @param range the {@link com.punchthrough.bean.sdk.message.AccelerometerRange} to be set
     */
    public void setAccelerometerRange(AccelerometerRange range) {
        Buffer buffer = new Buffer();
        buffer.writeByte(range.getRawValue());
        sendMessage(BeanMessageID.CC_ACCEL_SET_RANGE, buffer);
    }

    /**
     * Read the accelerometer range.
     *
     * @param callback the callback for the result
     */
    public void readAccelerometerRange(Callback<AccelerometerRange> callback) {
        addCallback(BeanMessageID.CC_ACCEL_GET_RANGE, callback);
        sendMessageWithoutPayload(BeanMessageID.CC_ACCEL_GET_RANGE);
    }

    /**
     * Set a scratch bank data value with raw bytes.
     *
     * @param bank The {@link com.punchthrough.bean.sdk.message.ScratchBank} being set
     * @param data The bytes to write into the scratch bank
     */
    public void setScratchData(ScratchBank bank, byte[] data) {
        ScratchData sd = ScratchData.create(bank, data);
        sendMessage(BeanMessageID.BT_SET_SCRATCH, sd);
    }

    /**
     * Set a scratch bank data value with a string in the form of UTF-8 bytes.
     *
     * @param bank The {@link com.punchthrough.bean.sdk.message.ScratchBank} being set
     * @param data The string data to write into the scratch bank as UTF-8
     */
    public void setScratchData(ScratchBank bank, String data) {
        ScratchData sd = ScratchData.create(bank, data);
        sendMessage(BeanMessageID.BT_SET_SCRATCH, sd);
    }

    /**
     * <p>
     * Set the radio config.
     *
     * <p/><p>
     *
     * This is equivalent to calling
     * {@link #setRadioConfig(com.punchthrough.bean.sdk.message.RadioConfig, boolean)}
     * with true for the save parameter.
     * </p>
     *
     * @param config the {@link com.punchthrough.bean.sdk.message.RadioConfig} to set
     */
    public void setRadioConfig(RadioConfig config) {
        setRadioConfig(config, true);
    }

    /**
     * Set the radio config.
     *
     * @param config the {@link com.punchthrough.bean.sdk.message.RadioConfig} to set
     * @param save   true to save the config in non-volatile storage, false otherwise.
     */
    public void setRadioConfig(RadioConfig config, boolean save) {
        sendMessage(save ? BeanMessageID.BT_SET_CONFIG : BeanMessageID.BT_SET_CONFIG_NOSAVE, config);
    }

    /**
     * Send raw bytes to the Bean as a serial message.
     *
     * @param value the message payload
     */
    public void sendSerialMessage(byte[] value) {
        Buffer buffer = new Buffer();
        buffer.write(value);
        sendMessage(BeanMessageID.SERIAL_DATA, buffer);
    }

    /**
     * Set the Bean's security code.
     *
     * @param pin    the 6 digit pin as a number, e.g. <code>123456</code>
     * @param active true to enable authenticated mode, false to disable the current pin
     */
    public void setPin(int pin, boolean active) {
        Buffer buffer = new Buffer();
        buffer.writeIntLe(pin);
        buffer.writeByte(active ? 1 : 0);
        sendMessage(BeanMessageID.BT_SET_PIN, buffer);
    }

    /**
     * Send a UTF-8 string to the Bean as a serial message.
     *
     * @param value the message to send as UTF-8 bytes
     */
    public void sendSerialMessage(String value) {
        Buffer buffer = new Buffer();
        try {
            buffer.write(value.getBytes("UTF-8"));
            sendMessage(BeanMessageID.SERIAL_DATA, buffer);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the device information (hardware, firmware and software version)
     *
     * @param callback the callback for the result
     */
    public void readDeviceInfo(final Callback<DeviceInfo> callback) {
        gattClient.getDeviceProfile().getDeviceInfo(new DeviceInfoCallback() {
            @Override
            public void onDeviceInfo(DeviceInfo info) {
                callback.onResult(info);
            }
        });
    }

    /**
     * Read the Bean hardware version
     *
     * @param callback the callback for the version string
     */
    public void readFirmwareVersion(final Callback<String> callback) {
        gattClient.getDeviceProfile().getFirmwareVersion(new DeviceProfile.VersionCallback() {
            @Override
            public void onComplete(String version) {
                callback.onResult(version);
            }
        });
    }

    /**
     * Read Bean firmware version
     *
     * @param callback the callback for the version string
     */
    public void readHardwareVersion(final Callback<String> callback) {
        gattClient.getDeviceProfile().getHardwareVersion(new DeviceProfile.VersionCallback() {
            @Override
            public void onComplete(String version) {
                callback.onResult(version);
            }
        });
    }

    /**
     * Enable or disable the Arduino.
     *
     * @param enable true to enable, false to disable
     */
    public void setArduinoEnabled(boolean enable) {
        Buffer buffer = new Buffer();
        buffer.writeByte(enable ? 1 : 0);
        sendMessage(BeanMessageID.CC_POWER_ARDUINO, buffer);
    }

    /**
     * Read the Arduino power state.
     *
     * @param callback the callback for the power state result: true if the Arduino is on
     */
    public void readArduinoPowerState(final Callback<Boolean> callback) {
        addCallback(BeanMessageID.CC_GET_AR_POWER, callback);
        sendMessageWithoutPayload(BeanMessageID.CC_GET_AR_POWER);
    }

    /**
     * Read the battery level.
     *
     * @param callback the callback for the {@link com.punchthrough.bean.sdk.message.BatteryLevel}
     *                 result
     */
    public void readBatteryLevel(final Callback<BatteryLevel> callback) {
        gattClient.getBatteryProfile().getBatteryLevel(new BatteryLevelCallback() {
            @Override
            public void onBatteryLevel(int percentage) {
                callback.onResult(new BatteryLevel(percentage));
            }
        });
    }

    /**
     * End the Arduino's serial gate, allowing serial data from the Arduino to be read by the
     * connected Android client.
     *
     * The serial gate exists to prevent chatty sketches from interfering with the behavior of
     * clients that want to send commands to the ATmega. It is enabled on initial connection to the
     * Bean.
     */
    public void endSerialGate() {
        sendMessageWithoutPayload(BeanMessageID.BT_END_GATE);
    }

    /**
     * Programs the Bean with an Arduino sketch in hex form. The Bean's sketch name and
     * programmed-at timestamp will be set from the
     * {@link com.punchthrough.bean.sdk.upload.SketchHex} object.
     *
     * @param hex           The sketch to be sent to the Bean
     * @param onProgress    Called with progress while the sketch upload is occurring
     * @param onComplete    Called when the sketch upload is complete
     */
    public void programWithSketch(SketchHex hex, Callback<UploadProgress> onProgress, Runnable onComplete) {

        // Resetting client state means we have a clean state to start. Variables are cleared and
        // the state timeout timer will not fire during firmware uploads.
        resetSketchUploadState();

        // Set onProgress and onComplete handlers
        this.onSketchUploadProgress = onProgress;
        this.onSketchUploadComplete = onComplete;

        // Prepare the sketch blocks to be sent
        sketchBlocksToSend = Chunk.chunksFrom(hex, MAX_BLOCK_SIZE_BYTES);

        // Construct and send the START payload with sketch metadata
        SketchMetadata metadata = SketchMetadata.create(hex, new Date());
        Buffer payload = metadata.toPayload();

        // If there's no data in the hex sketch, send the empty metadata to clear the Bean's sketch
        // and don't worry about sending sketch blocks
        if (hex.bytes().length > 0) {
            sketchUploadState = SketchUploadState.SENDING_START_COMMAND;
            resetSketchStateTimeout();
        }

        sendMessage(BeanMessageID.BL_CMD_START, payload);

    }

    /**
     * Programs the Bean with new firmware images.
     *
     * @param bundle        The firmware package holding A and B images to be sent to the Bean
     * @param listener      OADListener to alert the client of OAD state
     */
    public OADProfile.OADApproval programWithFirmware(FirmwareBundle bundle, OADProfile.OADListener listener) {
        return gattClient.getOADProfile().programWithFirmware(bundle, listener);
    }

    public boolean firmwareUpdateInProgress() {
        return gattClient.getOADProfile() != null && gattClient.getOADProfile().uploadInProgress();

    }

    /**
     * Used by Android {@link android.os.Parcel}.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Used by Android {@link android.os.Parcel}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(device, 0);
    }
}
