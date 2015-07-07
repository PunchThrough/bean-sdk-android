package com.punchthrough.bean.sdk.internal.battery;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;


/**
 * Encapsulation of the <a href="https://developer.bluetooth.org/TechnologyOverview/Pages/BAS.aspx">Device Information Service Profile (DIS).</a>
 */
public class BatteryProfile extends BaseProfile {

    private static final int BATTERY_SERVICE_UID = 0x180F;
    private static final int BATTERY_CHARACTERISTIC_UID = 0x2A19;

    private BluetoothGattService mBatteryService;
    private BatteryLevelCallback mCallback;

    public BatteryProfile(GattClient client) {
        super(client);
    }

    @Override
    public void onConnected() {
        mGattClient.discoverServices();
    }

    @Override
    public void onServicesDiscovered(GattClient client) {
        List<BluetoothGattService> services = client.getServices();
        for (BluetoothGattService service : services) {
            if ((service.getUuid().getMostSignificantBits() >> 32) == BATTERY_SERVICE_UID) {
                mBatteryService = service;
            }
        }
    }

    public void getBatteryLevel(BatteryLevelCallback callback) {
        mCallback = callback;
        for (BluetoothGattCharacteristic characteristic : mBatteryService.getCharacteristics()) {
            if ((characteristic.getUuid().getMostSignificantBits() >> 32) == BATTERY_CHARACTERISTIC_UID) {
                mGattClient.readCharacteristic(characteristic);
            }
        }
    }

    @Override
    public void onCharacteristicRead(GattClient client, BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getUuid().getMostSignificantBits() >> 32) == BATTERY_CHARACTERISTIC_UID) {
            byte[] value = characteristic.getValue();
            if (mCallback != null) {
                int percentage = value[0] & 0xff;
                if (percentage > 100) {
                    percentage = 100;
                } else if (percentage < 0) {
                    percentage = 0;
                }
                mCallback.onBatteryLevel(percentage);
                mCallback = null;
            }
        }
    }

    public static interface BatteryLevelCallback {
        public void onBatteryLevel(int percentage);
    }
}
