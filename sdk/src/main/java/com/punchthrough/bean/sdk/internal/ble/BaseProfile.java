package com.punchthrough.bean.sdk.internal.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;


/**
 * Base class that all GATT based Bluetooth profiles should extend
 */
public class BaseProfile {

    protected final GattClient mGattClient;

    public BaseProfile(GattClient client) {
        this.mGattClient = client;
    }

    public String getName() {
        return "Base Profile";
    }

    public void beanReady() {}

    public void onProfileReady() {}

    public void onBeanConnected() {}

    public void onBeanDisconnected() {}

    public void onBeanConnectionFailed() {}

    public void onCharacteristicRead(GattClient client, BluetoothGattCharacteristic characteristic) {}

    public void onCharacteristicWrite(GattClient client, BluetoothGattCharacteristic characteristic) {}

    public void onCharacteristicChanged(GattClient client, BluetoothGattCharacteristic characteristic) {}

    public void onDescriptorRead(GattClient client, BluetoothGattDescriptor descriptor) {}

    public void onDescriptorWrite(GattClient client, BluetoothGattDescriptor descriptor) {}

    public void onReadRemoteRssi(GattClient client, int rssi) {}

}
