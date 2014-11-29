package nl.littlerobots.bean.internal.serial;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import nl.littlerobots.bean.internal.ble.BaseProfile;
import nl.littlerobots.bean.internal.ble.GattClient;
import nl.littlerobots.beanlib.BuildConfig;
import okio.Buffer;

public class GattSerialTransportProfile extends BaseProfile {
    public static final int PACKET_TX_MAX_PAYLOAD_LENGTH = 19;
    private static final String TAG = "GattSerialTransportProfile";
    private static final UUID BEAN_SERIAL_CHARACTERISTIC_UUID = UUID.fromString("a495ff11-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SERIAL_SERVICE_UUID = UUID.fromString("a495ff10-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_SERVICE_UUID = UUID.fromString("a495ff20-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_0_UUID = UUID.fromString("a495ff21-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_1_UUID = UUID.fromString("a495ff22-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_2_UUID = UUID.fromString("a495ff23-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_3_UUID = UUID.fromString("a495ff24-c5b1-4b44-b512-1370f02d74de");
    private static final UUID BEAN_SCRATCH_4_UUID = UUID.fromString("a495ff25-c5b1-4b44-b512-1370f02d74de");

    private static final List<UUID> BEAN_SCRATCH_UUIDS = Arrays.asList(BEAN_SCRATCH_0_UUID, BEAN_SCRATCH_1_UUID, BEAN_SCRATCH_2_UUID, BEAN_SCRATCH_3_UUID, BEAN_SCRATCH_4_UUID);
    private WeakReference<Listener> mListener = new WeakReference<>(null);
    private BluetoothGattCharacteristic mSerialCharacteristic;
    private boolean mReadyToSend = false;

    private final Runnable mDequeueRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mPendingPackets.isEmpty()) {
                if (mReadyToSend && mSerialCharacteristic != null) {
                    mReadyToSend = false;
                    GattSerialPacket packet = mPendingPackets.remove(0);
                    mSerialCharacteristic.setValue(packet.getPacketData());
                    mGattClient.writeCharacteristic(mSerialCharacteristic);
                }
                mHandler.postDelayed(this, 150);
            }
        }
    };
    private List<GattSerialPacket> mPendingPackets = new ArrayList<>(32);
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int mOutgoingMessageCount = 0;
    private MessageAssembler mMessageAssembler = new MessageAssembler();

    public GattSerialTransportProfile(GattClient client) {
        super(client);
    }

    @Override
    public void onConnectionStateChange(int newState) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            mGattClient.discoverServices();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            abort();
        }
    }

    @Override
    public void onServicesDiscovered(GattClient client) {
        BluetoothGattService service = client.getService(BEAN_SERIAL_SERVICE_UUID);
        mSerialCharacteristic = service.getCharacteristic(BEAN_SERIAL_CHARACTERISTIC_UUID);
        if (mSerialCharacteristic == null) {
            Log.w(TAG, "Did not find bean serial on device, disconnecting");
            abort();
        } else {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Connected");
            }

            client.setCharacteristicNotification(mSerialCharacteristic, true);
            for (BluetoothGattDescriptor descriptor : mSerialCharacteristic.getDescriptors()) {
                if ((descriptor.getUuid().getMostSignificantBits() >> 32) == 0x2902) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    client.writeDescriptor(descriptor);
                }
            }
            service = client.getService(BEAN_SCRATCH_SERVICE_UUID);
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                client.setCharacteristicNotification(characteristic, true);
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    if ((descriptor.getUuid().getMostSignificantBits() >> 32) == 0x2902) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        client.writeDescriptor(descriptor);
                    }
                }
            }
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
                Listener listener = mListener.get();
                if (listener != null) {
                    listener.onMessageReceived(data);
                } else {
                    client.disconnect();
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
                    client.disconnect();
                }
            }
        }
    }

    @Override
    public void onDescriptorWrite(GattClient client, BluetoothGattDescriptor descriptor) {
        if (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
            if (descriptor.getCharacteristic() == mSerialCharacteristic) {
                // connection has completed
                mMessageAssembler.reset();
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
                    abort();
                }
            }
        }
    }

    private void abort() {
        boolean wasConnected = mSerialCharacteristic != null;
        mSerialCharacteristic = null;
        Listener listener = mListener.get();
        if (listener != null) {
            if (wasConnected) {
                listener.onDisconnected();
            } else {
                listener.onConnectionFailed();
            }
        }
        mGattClient.disconnect();
    }

    public void sendMessage(Buffer message) {
        // create packet, add to queue, schedule
        if (mSerialCharacteristic == null) {
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

    public void setListener(Listener listener) {
        this.mListener = new WeakReference<>(listener);
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
