package com.punchthrough.bean.sdk.internal.device;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.message.DeviceInfo;

/**
 * Encapsulation of the <a href="https://developer.bluetooth.org/TechnologyOverview/Pages/DIS.aspx">Device Information Service Profile (DIS).</a>
 */
public class DeviceProfile extends BaseProfile {

    private static final int DEVICE_SERVICE_UUID = 0x180a;
    private static final int CHARACTERISTIC_HARDWARE_VERSION = 0x2A27;
    private static final int CHARACTERISTIC_FIRMWARE_VERSION = 0x2A26;
    private static final int CHARACTERISTIC_SOFTWARE_VERSION = 0x2A28;
    private static final String TAG = "DeviceProfile";

    private String mSoftwareVersion;
    private String mHardwareVersion;
    private String mFirmwareVersion;
    private BluetoothGattService mDeviceService;
    private DeviceInfoCallback mCallback;

    public DeviceProfile(GattClient client) {
        super(client);
    }

    @Override
    public void onConnectionStateChange(int newState) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            mGattClient.discoverServices();
        }
    }

    @Override
    public void onServicesDiscovered(GattClient client) {
        List<BluetoothGattService> services = client.getServices();
        for (BluetoothGattService service : services) {
            if ((service.getUuid().getMostSignificantBits() >> 32) == DEVICE_SERVICE_UUID) {
                mDeviceService = service;
            }
        }
    }

    @Override
    public void onCharacteristicRead(GattClient client, BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getUuid().getMostSignificantBits() >> 32) == CHARACTERISTIC_FIRMWARE_VERSION) {
            mFirmwareVersion = characteristic.getStringValue(0);
        } else if ((characteristic.getUuid().getMostSignificantBits() >> 32) == CHARACTERISTIC_HARDWARE_VERSION) {
            mHardwareVersion = characteristic.getStringValue(0);
        } else if ((characteristic.getUuid().getMostSignificantBits() >> 32) == CHARACTERISTIC_SOFTWARE_VERSION) {
            mSoftwareVersion = characteristic.getStringValue(0);
        }
        if (mFirmwareVersion != null && mSoftwareVersion != null && mHardwareVersion != null) {
            DeviceInfo info = DeviceInfo.create(mHardwareVersion, mSoftwareVersion, mFirmwareVersion);
            if (mCallback != null) {
                mCallback.onDeviceInfo(info);
                mCallback = null;
            }
        }
    }

    public void getDeviceInfo(DeviceInfoCallback callback) {
        mCallback = callback;
        for (BluetoothGattCharacteristic characteristic : mDeviceService.getCharacteristics()) {
            if ((characteristic.getUuid().getMostSignificantBits() >> 32) == CHARACTERISTIC_FIRMWARE_VERSION ||
                    (characteristic.getUuid().getMostSignificantBits() >> 32) == CHARACTERISTIC_SOFTWARE_VERSION ||
                    (characteristic.getUuid().getMostSignificantBits() >> 32) == CHARACTERISTIC_HARDWARE_VERSION) {
                mGattClient.readCharacteristic(characteristic);
            }
        }
    }

    public static interface DeviceInfoCallback {
        public void onDeviceInfo(DeviceInfo info);
    }
}
