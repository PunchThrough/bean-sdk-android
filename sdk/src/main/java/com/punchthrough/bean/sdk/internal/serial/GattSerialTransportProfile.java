package com.punchthrough.bean.sdk.internal.serial;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.util.Log;

import com.punchthrough.bean.sdk.BuildConfig;
import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.internal.utility.EnumParse;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.internal.utility.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okio.Buffer;

/**
 * Custom BLE profile that allows serial communications
 */
public class GattSerialTransportProfile extends BaseProfile {

    protected static final String TAG = "GattSerialXportProfile";

    // Constants
    public static final int PACKET_TX_MAX_PAYLOAD_LENGTH = 19;
    private static final List<UUID> BEAN_SCRATCH_UUIDS = Arrays.asList(
            Constants.UUID_SCRATCH_CHAR_1,
            Constants.UUID_SCRATCH_CHAR_2,
            Constants.UUID_SCRATCH_CHAR_3,
            Constants.UUID_SCRATCH_CHAR_4,
            Constants.UUID_SCRATCH_CHAR_5
    );

    // Internal dependencies
    private SerialListener mListener;
    private BluetoothGattCharacteristic mSerialCharacteristic;
    private Handler mHandler;
    private MessageAssembler mMessageAssembler = new MessageAssembler();

    // Internal state
    private boolean ready = false;
    private boolean mReadyToSend = false;
    private List<GattSerialPacket> mPendingPackets = new ArrayList<>(32);
    private int mOutgoingMessageCount = 0;

    private final Runnable mDequeueRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mPendingPackets.isEmpty()) {
                if (mReadyToSend && mSerialCharacteristic != null) {
                    mReadyToSend = false;
                    GattSerialPacket packet = mPendingPackets.remove(0);
                    mSerialCharacteristic.setValue(packet.getPacketData());
                    if (!mGattClient.writeCharacteristic(mSerialCharacteristic)) {
                        Log.e(TAG, "Failed char write");
                    }
                } else {
                    mHandler.postDelayed(this, 150);
                }
            }
        }
    };

    public GattSerialTransportProfile(GattClient client, Handler handler) {
        super(client);
        mHandler = handler;
    }

    @Override
    public void onProfileReady() {

        BluetoothGattService service = mGattClient.getService(Constants.UUID_SERIAL_SERVICE);
        mSerialCharacteristic = service.getCharacteristic(Constants.UUID_SERIAL_CHAR);
        if (mSerialCharacteristic == null) {
            Log.w(TAG, "Did not find bean serial on device");
            abort("Did not find bean serial on device");
        } else {

            // Enable Notifications for Serial chars
            mGattClient.setCharacteristicNotification(mSerialCharacteristic, true);
            for (BluetoothGattDescriptor descriptor : mSerialCharacteristic.getDescriptors()) {
                if ((descriptor.getUuid().getMostSignificantBits() >> 32) == 0x2902) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mGattClient.writeDescriptor(descriptor);
                }
            }

            service = mGattClient.getService(Constants.UUID_SCRATCH_SERVICE);

            boolean hasScratchChars = true;

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                if (characteristic.getDescriptors().size() < 2) {
                    /* This Bean has a scratch characteristic with less than two
                     * descriptors.
                     *
                     * This is a very old (probably factory-firmware) Bean. These Beans have
                     * malformed scratch characteristic tables. If Android writes to one of
                     * these Beans' scratch characteristic tables, it will close the
                     * connection.
                     *
                     * To fix this problem, update these Beans to the latest firmware before
                     * use.
                     */
                    hasScratchChars = false;
                }
            }

            if (hasScratchChars) {
                // Enable Notifications for Scratch chars
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    mGattClient.setCharacteristicNotification(characteristic, true);
                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        if ((descriptor.getUuid().getMostSignificantBits() >> 32) == 0x2902) {

                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mGattClient.writeDescriptor(descriptor);
                        }
                    }
                }
            }

            mMessageAssembler.reset();
            mReadyToSend = true;
            mOutgoingMessageCount = 0;
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Setup complete");
            }
            ready = true;
        }
    }

    @Override
    public void onCharacteristicWrite(GattClient client, BluetoothGattCharacteristic characteristic) {
        if (mSerialCharacteristic == characteristic) {
            mHandler.removeCallbacks(mDequeueRunnable);
            mReadyToSend = true;
            mHandler.post(mDequeueRunnable);
        }
    }

    @Override
    public void onCharacteristicChanged(GattClient client, BluetoothGattCharacteristic characteristic) {
        if (characteristic == mSerialCharacteristic) {
            byte[] data = mMessageAssembler.assemble(new GattSerialPacket(characteristic.getValue()));
            if (data != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Received data");
                }
                if (mListener != null) {
                    mListener.onMessageReceived(data);
                } else {
                    client.disconnect();
                }
            }
        } else {
            // scratch
            int index = BEAN_SCRATCH_UUIDS.indexOf(characteristic.getUuid());
            if (index > -1) {
                index += 1;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Received scratch bank update (" + index + ")");
                }
                if (mListener != null) {
                    try {
                        ScratchBank bank = EnumParse.enumWithRawValue(ScratchBank.class, index);
                        mListener.onScratchValueChanged(bank, characteristic.getValue());
                    } catch (NoEnumFoundException e) {
                        Log.e(TAG, "Couldn't parse bank enum from scratch bank with index " +
                                index);
                        e.printStackTrace();
                    }
                } else {
                    client.disconnect();
                }
            }
        }
    }

    @Override
    public void onReadRemoteRssi(GattClient client, int rssi) {
        final SerialListener listener = mListener;
        if (listener != null) {
            listener.onReadRemoteRssi(rssi);
        } else {
            abort("No listener, this must be a stale connection --> disconnect");
        }
    }

    public void abort(String message) {
        mListener.onError(message);
    }

    public void sendMessage(Buffer message) {
        if (mSerialCharacteristic == null) {
            Log.e(TAG, "Unexpected: mSerialCharacteristic is null");
        }

        // create packet, add to queue, schedule
        int packets = (int) (message.size() / PACKET_TX_MAX_PAYLOAD_LENGTH);
        mOutgoingMessageCount = (mOutgoingMessageCount + 1) % 4;
        int size = (int) message.size();
        for (int i = 0; i < size; i += PACKET_TX_MAX_PAYLOAD_LENGTH) {
            GattSerialPacket packet = new GattSerialPacket(i == 0, mOutgoingMessageCount, packets--, message);
            mPendingPackets.add(packet);
        }
        mHandler.post(mDequeueRunnable);
    }

    /**
     * Sets a listener that will be alerted for serial and scratch events
     *
     * @param listener SerialListener object
     */
    public void setListener(SerialListener listener) {
        this.mListener = listener;
    }

    public String getName() {
        return TAG;
    }

    public boolean isReady() {
        return ready;
    }

    public void clearReady() {
        ready = false;
    }

    // This listener is only for communicating with the Bean class
    public static interface SerialListener {

        public void onMessageReceived(byte[] data);

        public void onScratchValueChanged(ScratchBank bank, byte[] value);

        public void onError(String message);

        public void onReadRemoteRssi(int rssi);
    }
}
