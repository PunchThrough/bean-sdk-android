package nl.littlerobots.bean.internal.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import nl.littlerobots.bean.internal.battery.BatteryProfile;
import nl.littlerobots.bean.internal.device.DeviceProfile;
import nl.littlerobots.bean.internal.serial.GattSerialTransportProfile;

public class GattClient {
    private static final String TAG = "GattClient";
    private final GattSerialTransportProfile mSerialProfile;
    private final DeviceProfile mDeviceProfile;
    private final BatteryProfile mBatteryProfile;
    private BluetoothGatt mGatt;
    private List<BaseProfile> mProfiles = new ArrayList<>(10);
    private Queue<Runnable> mOperationsQueue = new ArrayDeque<>(32);
    private boolean mOperationInProgress = false;
    private boolean mConnected = false;
    private boolean mDiscoveringServices = false;
    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fireConnectionStateChange(BluetoothGatt.STATE_DISCONNECTED);
                disconnect();
                return;
            }
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mConnected = true;
            }
            fireConnectionStateChange(newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mDiscoveringServices = false;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect();
                return;
            }
            fireServicesDiscovered();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect();
                return;
            }
            fireCharacteristicsRead(characteristic);
            executeNextOperation();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect();
                return;
            }
            fireCharacteristicWrite(characteristic);
            executeNextOperation();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            fireCharacteristicChanged(characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect();
                return;
            }
            fireDescriptorRead(descriptor);
            executeNextOperation();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect();
                return;
            }
            fireDescriptorWrite(descriptor);
            executeNextOperation();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect();
            }
        }
    };

    public GattClient() {
        mSerialProfile = new GattSerialTransportProfile(this);
        mDeviceProfile = new DeviceProfile(this);
        mBatteryProfile = new BatteryProfile(this);
        mProfiles.add(mSerialProfile);
        mProfiles.add(mDeviceProfile);
        mProfiles.add(mBatteryProfile);
    }

    private void fireDescriptorRead(BluetoothGattDescriptor descriptor) {
        for (BaseProfile profile : mProfiles) {
            profile.onDescriptorRead(this, descriptor);
        }
    }

    private synchronized void queueOperation(Runnable operation) {
        mOperationsQueue.offer(operation);
        if (!mOperationInProgress) {
            executeNextOperation();
        }
    }

    private synchronized void executeNextOperation() {
        Runnable operation = mOperationsQueue.poll();
        if (operation != null) {
            mOperationInProgress = true;
            operation.run();
        } else {
            mOperationInProgress = false;
        }
    }

    public void connect(Context context, BluetoothDevice device) {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
        mConnected = false;
        mGatt = device.connectGatt(context, false, mBluetoothGattCallback);
    }

    private void fireDescriptorWrite(BluetoothGattDescriptor descriptor) {
        for (BaseProfile profile : mProfiles) {
            profile.onDescriptorWrite(this, descriptor);
        }
    }

    private void fireCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        for (BaseProfile profile : mProfiles) {
            profile.onCharacteristicChanged(this, characteristic);
        }
    }

    private void fireCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
        for (BaseProfile profile : mProfiles) {
            profile.onCharacteristicWrite(this, characteristic);
        }
    }

    private void fireCharacteristicsRead(BluetoothGattCharacteristic characteristic) {
        for (BaseProfile profile : mProfiles) {
            profile.onCharacteristicRead(this, characteristic);
        }
    }

    private void fireServicesDiscovered() {
        for (BaseProfile profile : mProfiles) {
            profile.onServicesDiscovered(this);
        }
    }

    private synchronized void fireConnectionStateChange(int newState) {
        if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            mOperationsQueue.clear();
            mOperationInProgress = false;
            mConnected = false;
        } else if (newState == BluetoothGatt.STATE_CONNECTED) {
            mConnected = true;
        }
        for (BaseProfile profile : mProfiles) {
            profile.onConnectionStateChange(newState);
        }
    }

    public List<BluetoothGattService> getServices() {
        return mGatt.getServices();
    }

    public BluetoothGattService getService(UUID uuid) {
        return mGatt.getService(uuid);
    }

    public boolean discoverServices() {
        if (mDiscoveringServices) {
            return true;
        }
        mDiscoveringServices = true;
        return mGatt.discoverServices();
    }

    public synchronized boolean readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        queueOperation(new Runnable() {
            @Override
            public void run() {
                if (mGatt != null) {
                    mGatt.readCharacteristic(characteristic);
                }
            }
        });
        return true;
    }

    public synchronized boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final byte[] value = characteristic.getValue();
        queueOperation(new Runnable() {
            @Override
            public void run() {
                if (mGatt != null) {
                    characteristic.setValue(value);
                    mGatt.writeCharacteristic(characteristic);
                }
            }
        });
        return true;
    }

    public boolean readDescriptor(final BluetoothGattDescriptor descriptor) {
        queueOperation(new Runnable() {
            @Override
            public void run() {
                if (mGatt != null) {
                    mGatt.readDescriptor(descriptor);
                }
            }
        });
        return true;
    }

    public boolean writeDescriptor(final BluetoothGattDescriptor descriptor) {
        final byte[] value = descriptor.getValue();
        queueOperation(new Runnable() {
            @Override
            public void run() {
                if (mGatt != null) {
                    descriptor.setValue(value);
                    mGatt.writeDescriptor(descriptor);
                }
            }
        });
        return true;
    }

    public boolean readRemoteRssi() {
        return mGatt.readRemoteRssi();
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        return mGatt.setCharacteristicNotification(characteristic, enable);
    }

    private boolean connect() {
        return mGatt != null && mGatt.connect();
    }

    public void disconnect() {
        close();
    }

    private synchronized void close() {
        if (mGatt != null) {
            mGatt.close();
        }
        mGatt = null;
    }

    public GattSerialTransportProfile getSerialProfile() {
        return mSerialProfile;
    }

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    public BatteryProfile getBatteryProfile() {
        return mBatteryProfile;
    }
}
