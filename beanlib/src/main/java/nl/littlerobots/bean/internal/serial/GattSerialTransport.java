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

package nl.littlerobots.bean.internal.serial;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import nl.littlerobots.beanlib.BuildConfig;
import okio.Buffer;

public class GattSerialTransport {
    public static final int PACKET_TX_MAX_PAYLOAD_LENGTH = 19;
    private static final UUID BEAN_SERIAL_CHARACTERISTIC_UUID = UUID.fromString("a495ff11-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SERIAL_SERVICE_UUID = UUID.fromString("a495ff10-c5b1-4b44-b512-1370f02d74de");
    private static final String TAG = "GattSerialTransport";
    private static final UUID BEAN_SCRATCH_SERVICE_UUID = UUID.fromString("a495ff20-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_0_UUID = UUID.fromString("a495ff21-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_1_UUID = UUID.fromString("a495ff22-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_2_UUID = UUID.fromString("a495ff23-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_3_UUID = UUID.fromString("a495ff24-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_4_UUID = UUID.fromString("a495ff25-c5b1-4b44-b512-1370f02d74de");

    private static final List<UUID> BEAN_SCRATCH_UUIDS = Arrays.asList(BEAN_SCRATCH_0_UUID, BEAN_SCRATCH_1_UUID, BEAN_SCRATCH_2_UUID, BEAN_SCRATCH_3_UUID, BEAN_SCRATCH_4_UUID);

    private final BluetoothDevice mDevice;
    private WeakReference<Listener> mListener = new WeakReference<>(null);
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mSerialCharacteristic;
    private boolean mReadyToSend = false;
    private final Runnable mDequeueRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mPendingPackets.isEmpty()) {
                if (mReadyToSend && mSerialCharacteristic != null && mGatt != null) {
                    mReadyToSend = false;
                    GattSerialPacket packet = mPendingPackets.remove(0);
                    mSerialCharacteristic.setValue(packet.getPacketData());
                    if (BuildConfig.DEBUG) {
                        logWritten(mSerialCharacteristic.getValue());
                    }
                    mGatt.writeCharacteristic(mSerialCharacteristic);
                }
                mHandler.postDelayed(this, 150);
            }
        }
    };
    private List<GattSerialPacket> mPendingPackets = new ArrayList<>(32);
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int mOutgoingMessageCount = 0;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onConnectionStateChange, status = " + status + ", state = " + newState);
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Non success state, disconnecting");
                abort();
            } else if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Connected, discover services");
                }
                discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Disconnected");
                }
                abort();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Non success state, disconnecting");
                abort();
            } else {
                BluetoothGattService service = gatt.getService(BEAN_SERIAL_SERVICE_UUID);
                mSerialCharacteristic = service.getCharacteristic(BEAN_SERIAL_CHARACTERISTIC_UUID);
                if (mSerialCharacteristic == null) {
                    Log.w(TAG, "Did not find bean serial on device, disconnecting");
                    abort();
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "Connected");
                    }

                    mGatt.setCharacteristicNotification(mSerialCharacteristic, true);
                    for (BluetoothGattDescriptor descriptor : mSerialCharacteristic.getDescriptors()) {
                        if ((descriptor.getUuid().getMostSignificantBits() >> 32) == 0x2902) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                    service = gatt.getService(BEAN_SCRATCH_SERVICE_UUID);
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                            if ((descriptor.getUuid().getMostSignificantBits() >> 32) == 0x2902) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Write characteristic failed, disconnecting");
                abort();
            } else {
                mHandler.removeCallbacks(mDequeueRunnable);
                mReadyToSend = true;
                mHandler.post(mDequeueRunnable);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (characteristic == mSerialCharacteristic) {
                byte[] data = mMessageAssembler.assemble(new GattSerialPacket(characteristic.getValue()));
                if (data != null) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Received data");
                    }
                    Listener listener = mListener.get();
                    if (listener != null) {
                        listener.onMessageReceived(data);
                    } else {
                        disconnect();
                    }
                }
            } else {
                // scratch
                int index = BEAN_SCRATCH_UUIDS.indexOf(characteristic.getUuid());
                if (index > -1) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Received scratch bank update (" + index + ")");
                    }
                    Listener listener = mListener.get();
                    if (listener != null) {
                        listener.onScratchValueChanged(index, characteristic.getValue());
                    } else {
                        disconnect();
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                if (descriptor.getCharacteristic() == mSerialCharacteristic) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        abort();
                    } else {
                        // connection has completed
                        mReadyToSend = true;
                        mOutgoingMessageCount = 0;

                        if (BuildConfig.DEBUG) {
                            Log.i(TAG, "Setup complete");
                        }

                        Listener listener = mListener.get();
                        if (listener != null) {
                            listener.onConnected();
                        } else {
                            Log.e(TAG, "No listener, this must be a stale connection --> disconnect");
                            disconnect();
                        }
                    }
                }
            }
        }
    };
    private MessageAssembler mMessageAssembler = new MessageAssembler();

    public GattSerialTransport(Listener listener, BluetoothDevice device) {
        mDevice = device;
        mListener = new WeakReference<>(listener);
    }

    private void logWritten(byte[] value) {
        StringBuilder sb = new StringBuilder();
        for (byte b : value) {
            sb.append(Integer.toHexString(b & 0xff));
            sb.append(" ");
        }
        Log.d(TAG, "Write: " + sb.toString());
    }

    private void discoverServices() {
        mGatt.discoverServices();
    }

    public void connect(Context context) {
        if (mGatt != null) {
            Listener listener = mListener.get();
            if (listener != null) {
                listener.onConnected();
            } else {
                disconnect();
            }
        } else {
            mHandler.removeCallbacksAndMessages(null);
            mPendingPackets.clear();
            mGatt = mDevice.connectGatt(context, false, mGattCallback);
        }
    }

    private void abort() {
        boolean wasConnected = mSerialCharacteristic != null;
        disconnect();
        Listener listener = mListener.get();
        if (listener != null) {
            if (wasConnected) {
                listener.onDisconnected();
            } else {
                listener.onConnectionFailed();
            }
        }
    }

    public void disconnect() {
        mReadyToSend = false;
        mPendingPackets.clear();
        mHandler.removeCallbacksAndMessages(null);
        if (mGatt != null) {
            mGatt.close();
        }
        mSerialCharacteristic = null;
        mGatt = null;
    }

    public void sendMessage(Buffer message) {
        // create packet, add to queue, schedule
        if (mGatt == null) {
            //TODO maybe don't throw here since disconnection could have raced us
            throw new IllegalStateException("Not connected");
        }
        int packets = (int) (message.size() / PACKET_TX_MAX_PAYLOAD_LENGTH);
        mOutgoingMessageCount = (mOutgoingMessageCount + 1) % 4;
        int size = (int) message.size();
        for (int i = 0; i < size; i += PACKET_TX_MAX_PAYLOAD_LENGTH) {
            GattSerialPacket packet = new GattSerialPacket(i == 0, mOutgoingMessageCount, packets--, message);
            mPendingPackets.add(packet);
        }
        mHandler.post(mDequeueRunnable);
    }

    // This listener is only for communicating with the Bean class
    public static interface Listener {
        public void onConnected();
        public void onConnectionFailed();
        public void onDisconnected();
        public void onMessageReceived(byte[] data);
        public void onScratchValueChanged(int bank, byte[] value);
    }
}
