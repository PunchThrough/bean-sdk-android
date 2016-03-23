package com.punchthrough.bean.sdk.internal.scratch;

import android.bluetooth.BluetoothGattCharacteristic;

import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;

public class ScratchProfile extends BaseProfile {

    public ScratchProfile(GattClient client) {
        super(client);
    }

    @Override
    public void onProfileReady() {}

    @Override
    public void onCharacteristicRead(GattClient client, BluetoothGattCharacteristic characteristic) {}

    public String getName() {
        return "Scratch Profile";
    }
}
