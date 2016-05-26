package com.punchthrough.bean.sdk.internal.battery;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.utility.Constants;


/**
 * Encapsulation of the <a href="https://developer.bluetooth.org/TechnologyOverview/Pages/BAS.aspx">Device Information Service Profile (DIS).</a>
 */
public class BatteryProfile extends BaseProfile {

    protected static final String TAG = "BatteryProfile";
    protected boolean ready = false;
    private BluetoothGattService mBatteryService;
    private BatteryLevelCallback mCallback;

    public BatteryProfile(GattClient client) {
        super(client);
    }

    @Override
    public void onProfileReady() {
        List<BluetoothGattService> services = mGattClient.getServices();
        for (BluetoothGattService service : services) {
            if (service.getUuid().equals(Constants.UUID_BATTERY_SERVICE)) {
                mBatteryService = service;
            }
        }
        ready = true;
    }

    public void getBatteryLevel(BatteryLevelCallback callback) {
        mCallback = callback;
        for (BluetoothGattCharacteristic characteristic : mBatteryService.getCharacteristics()) {
            if (characteristic.getUuid().equals(Constants.UUID_BATTERY_CHARACTERISTIC)) {
                mGattClient.readCharacteristic(characteristic);
            }
        }
    }

    @Override
    public void onCharacteristicRead(GattClient client, BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(Constants.UUID_BATTERY_CHARACTERISTIC)) {
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

    public String getName() {
        return TAG;
    }

    public boolean isReady() {
        return ready;
    }

    public void clearReady() {
        ready = false;
    }

    public static interface BatteryLevelCallback {
        public void onBatteryLevel(int percentage);
    }
}
