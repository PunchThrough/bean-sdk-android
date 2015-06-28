package com.punchthrough.bean.sdk.internal.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;


/**
 * Base class that all GATT based Bluetooth profiles should extend
 */
public class BaseProfile {

    protected final GattClient mGattClient;

    public BaseProfile(GattClient client) {
        this.mGattClient = client;
    }

    public void onServicesDiscovered(GattClient client) {
    }

    public void onCharacteristicRead(GattClient client, BluetoothGattCharacteristic characteristic) {
    }

    public void onCharacteristicWrite(GattClient client, BluetoothGattCharacteristic characteristic) {
    }

    public void onCharacteristicChanged(GattClient client, BluetoothGattCharacteristic characteristic) {
    }

    public void onDescriptorRead(GattClient client, BluetoothGattDescriptor descriptor) {
    }

    public void onDescriptorWrite(GattClient client, BluetoothGattDescriptor descriptor) {
    }

    public void onReadRemoteRssi(GattClient client, int rssi) {
    }

    public void onConnected() {
    }

    public void onDisconnected() {
    }

    public void onConnectionStateChange(int newState) {
    }
}
