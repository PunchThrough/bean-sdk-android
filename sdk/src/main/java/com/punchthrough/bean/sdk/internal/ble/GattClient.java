package com.punchthrough.bean.sdk.internal.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.punchthrough.bean.sdk.internal.battery.BatteryProfile;
import com.punchthrough.bean.sdk.internal.device.DeviceProfile;
import com.punchthrough.bean.sdk.internal.exception.UnimplementedProfileException;
import com.punchthrough.bean.sdk.internal.scratch.ScratchProfile;
import com.punchthrough.bean.sdk.internal.serial.GattSerialTransportProfile;
import com.punchthrough.bean.sdk.internal.upload.firmware.OADProfile;
import com.punchthrough.bean.sdk.internal.utility.Constants;
import com.punchthrough.bean.sdk.internal.utility.Watchdog;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * Encapsulation of a GATT client in a typical central/peripheral BLE connection where the
 * GATT client is running on the central device.
 */
public class GattClient {

    private static final String TAG = "GattClient";

    // Profiles
    private final GattSerialTransportProfile mSerialProfile;
    private final DeviceProfile mDeviceProfile;
    private final BatteryProfile mBatteryProfile;
    private final OADProfile mOADProfile;
    private final ScratchProfile mScratchProfile;
    private List<BaseProfile> mProfiles = new ArrayList<>(10);

    // Internal dependencies
    private BluetoothGatt mGatt;
    private ConnectionListener connectionListener;
    private BluetoothDevice device;

    // Internal state
    private Queue<Runnable> mOperationsQueue = new ArrayDeque<>(32);
    private boolean mOperationInProgress = false;
    private boolean mConnected = false;

    public GattClient(Handler handler, BluetoothDevice device) {
        this.device = device;
        mSerialProfile = new GattSerialTransportProfile(this, handler);
        mDeviceProfile = new DeviceProfile(this);
        mBatteryProfile = new BatteryProfile(this);
        mOADProfile = new OADProfile(this, new Watchdog(handler));
        mScratchProfile = new ScratchProfile(this);
        mProfiles.add(mSerialProfile);
        mProfiles.add(mDeviceProfile);
        mProfiles.add(mBatteryProfile);
        mProfiles.add(mScratchProfile);
        mProfiles.add(mOADProfile);
    }

    private BaseProfile profileForUUID(UUID uuid) throws UnimplementedProfileException {
        if (uuid.equals(Constants.UUID_OAD_SERVICE)) {
            return mOADProfile;
        } else if (uuid.equals(Constants.UUID_SERIAL_SERVICE)) {
            return mSerialProfile;
        } else if (uuid.equals(Constants.UUID_BATTERY_SERVICE)) {
            return mBatteryProfile;
        } else if (uuid.equals(Constants.UUID_SCRATCH_SERVICE)) {
            return mScratchProfile;
        } else if (uuid.equals(Constants.UUID_DEVICE_INFO_SERVICE)) {
            return mDeviceProfile;
        } else {
            throw new UnimplementedProfileException("No profile with UUID: " + uuid.toString());
        }
    }

    /**
     * Attempt to invalidate Androids internal GATT table cache
     *
     * http://stackoverflow.com/a/22709467/5640435
     *
     * @param gatt BluetoothGatt object
     */
    private void refreshDeviceCache(BluetoothGatt gatt) {
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                localMethod.invoke(localBluetoothGatt, new Object[0]);
            } else {
                Log.e(TAG, "Couldn't find local method: refresh");
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occurred while refreshing device");
        }
    }

    private void describeService(BluetoothGattService service) {
        Log.i(TAG, "Service Found: " + service.getUuid().toString());
        for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
            Log.i(TAG, "    Char: " + c.getUuid().toString());
        }
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (getOADProfile().uploadInProgress()) {
                    // Since an OAD update is currently in progress, only alert the OAD Profile
                    // of the Bean disconnecting, not the ConnectionListener(s)
                    getOADProfile().onBeanConnectionFailed();
                } else {
                    connectionListener.onConnectionFailed();
                }

                mConnected = false;
                return;
            }

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mConnected = true;

                // Bean is connected, before alerting the ConnectionListener(s), we must
                // discover available services (lookup GATT table).
                Log.i(TAG, "Discovering Services!");
                mGatt.discoverServices();
            }

            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                mOperationsQueue.clear();
                mOperationInProgress = false;
                mConnected = false;
                connectionListener.onDisconnected();
                for (BaseProfile profile : mProfiles) {
                    profile.onBeanDisconnected();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Failed to discover services!");
                disconnect();
            } else {
                Log.i(TAG, "Service discovery complete!");

                for (BaseProfile profile : mProfiles) {
                    profile.clearReady();
                }

                // Tell each profile that they are ready and to do any other further configuration
                // that may be necessary such as looking up available characteristics.
                Log.i(TAG, "Starting to setup each available profile!");
                for (BluetoothGattService service : mGatt.getServices()) {
                    try {
                        BaseProfile profile = profileForUUID(service.getUuid());
                        profile.onProfileReady();
                        Log.i(TAG, "Profile ready: " + profile.getName());
                    } catch (UnimplementedProfileException e) {
                        Log.e(TAG, "No profile with UUID: " + service.getUuid().toString());
                    }
                }

                if (mOADProfile.uploadInProgress()) {
                    Log.i(TAG, "OAD In progress, continuing OAD process without calling ConnectionListener.onConnected()");
                    mOADProfile.continueOAD();
                } else {

                    for (BaseProfile profile : mProfiles) {
                        if (!profile.isReady()) {
                            Log.e(TAG, "Profile NOT Discovered: " + profile.getName());
                        }
                    }

                    connectionListener.onConnected();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect();
            } else {
                fireCharacteristicsRead(characteristic);
                executeNextOperation();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect();
            } else {
                fireCharacteristicWrite(characteristic);
                executeNextOperation();
            }
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
                return;
            }
            fireReadRemoteRssi(rssi);
            executeNextOperation();
        }
    };

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

    private void fireReadRemoteRssi(int rssi) {
        for (BaseProfile profile : mProfiles) {
            profile.onReadRemoteRssi(this, rssi);
        }
    }

    /****************************************************************************
                                  PUBLIC API
     ****************************************************************************/

    public String bleAddress() {
        return device.getAddress();
    }

    public void connect(Context context, BluetoothDevice device) {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
            mConnected = false;
        }

        Log.i(TAG, "Gatt connection started");
        mGatt = device.connectGatt(context, false, mBluetoothGattCallback);
        Log.i(TAG, "Refreshing GATT Cache");
        refreshDeviceCache(mGatt);
    }

    /**
     * Sets a listener that will be alerted on connection related events
     *
     * @param listener ConnectionListener object
     */
    public void setListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public boolean isConnected() {
        return mConnected;
    }

    public List<BluetoothGattService> getServices() {
        return mGatt.getServices();
    }

    public BluetoothGattService getService(UUID uuid) {
        return mGatt.getService(uuid);
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

    public void disconnect() {
        mGatt.disconnect();
    }

    public synchronized void close() {
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

    public OADProfile getOADProfile() {
        return mOADProfile;
    }

    // This listener is only for communicating with the Bean class
    public static interface ConnectionListener {
        public void onConnected();

        public void onConnectionFailed();

        public void onDisconnected();

    }
}
