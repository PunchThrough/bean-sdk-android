package com.punchthrough.bean.sdk.internal.device;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.internal.utility.Constants;

/**
 * Encapsulation of the <a href="https://developer.bluetooth.org/TechnologyOverview/Pages/DIS.aspx">Device Information Service Profile (DIS).</a>
 */
public class DeviceProfile extends BaseProfile {

    private static final String TAG = "DeviceProfile";

    private String mSoftwareVersion;
    private String mHardwareVersion;
    private String mFirmwareVersion;
    private BluetoothGattService mDeviceService;
    private DeviceInfoCallback deviceInfoCallback;
    private FirmwareVersionCallback firmwareVersionCallback;

    public DeviceProfile(GattClient client) {
        super(client);
    }

    @Override
    public void onProfileReady() {
        List<BluetoothGattService> services = mGattClient.getServices();
        for (BluetoothGattService service : services) {
            if (service.getUuid().equals(Constants.UUID_DEVICE_INFO_SERVICE)) {
                mDeviceService = service;
            }
        }
    }

    @Override
    public void onCharacteristicRead(GattClient client, BluetoothGattCharacteristic characteristic) {

        if (characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_FIRMWARE_VERSION)) {
            mFirmwareVersion = characteristic.getStringValue(0);
        } else if (characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_HARDWARE_VERSION)) {
            mHardwareVersion = characteristic.getStringValue(0);
        } else if (characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_SOFTWARE_VERSION)) {
            mSoftwareVersion = characteristic.getStringValue(0);
        }

        if (mFirmwareVersion != null && mSoftwareVersion != null && mHardwareVersion != null && deviceInfoCallback != null) {
            DeviceInfo info = DeviceInfo.create(mHardwareVersion, mSoftwareVersion, mFirmwareVersion);
            deviceInfoCallback.onDeviceInfo(info);
            deviceInfoCallback = null;
        }

        if (mFirmwareVersion != null && firmwareVersionCallback != null) {
            firmwareVersionCallback.onComplete(mFirmwareVersion);
            firmwareVersionCallback = null;
        }
    }

    public void getDeviceInfo(DeviceInfoCallback callback) {
        deviceInfoCallback = callback;
        for (BluetoothGattCharacteristic characteristic : mDeviceService.getCharacteristics()) {
            if (characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_FIRMWARE_VERSION) ||
                    characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_HARDWARE_VERSION) ||
                    characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_SOFTWARE_VERSION)) {
                mGattClient.readCharacteristic(characteristic);
            }
        }
    }

    public void getFirmwareVersion(FirmwareVersionCallback callback) {
        firmwareVersionCallback = callback;
        mGattClient.readCharacteristic(
                mDeviceService.getCharacteristic(
                        Constants.UUID_DEVICE_INFO_CHAR_FIRMWARE_VERSION));
    }

    public String getName() {
        return "Device Info Profile";
    }

    public static interface FirmwareVersionCallback {
        public void onComplete(String version);
    }

    public static interface DeviceInfoCallback {
        public void onDeviceInfo(DeviceInfo info);
    }
}
