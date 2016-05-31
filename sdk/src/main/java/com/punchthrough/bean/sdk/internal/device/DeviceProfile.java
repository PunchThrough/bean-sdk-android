package com.punchthrough.bean.sdk.internal.device;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.List;

import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.utility.Convert;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.internal.utility.Constants;

/**
 * Encapsulation of the <a href="https://developer.bluetooth.org/TechnologyOverview/Pages/DIS.aspx">Device Information Service Profile (DIS).</a>
 */
public class DeviceProfile extends BaseProfile {

    protected static final String TAG = "DeviceProfile";

    private boolean ready = false;
    private String mSoftwareVersion;
    private String mHardwareVersion;
    private String mFirmwareVersion;
    private BluetoothGattService mDeviceService;
    private DeviceInfoCallback deviceInfoCallback;
    private VersionCallback firmwareVersionCallback;
    private VersionCallback hardwareVersionCallback;

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
        ready = true;
    }

    @Override
    public void onCharacteristicRead(GattClient client, BluetoothGattCharacteristic characteristic) {

        if (characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_FIRMWARE_VERSION)) {
            Log.i(TAG, "Read response (FW Version): " + Convert.bytesToHexString(characteristic.getValue()));
            mFirmwareVersion = characteristic.getStringValue(0);
        } else if (characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_HARDWARE_VERSION)) {
            Log.i(TAG, "Read response (HW Version): " + Convert.bytesToHexString(characteristic.getValue()));
            mHardwareVersion = characteristic.getStringValue(0);
        } else if (characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_SOFTWARE_VERSION)) {
            Log.i(TAG, "Read response (SW Version): " + Convert.bytesToHexString(characteristic.getValue()));
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

        if (mHardwareVersion != null && hardwareVersionCallback != null) {
            hardwareVersionCallback.onComplete(mHardwareVersion);
            hardwareVersionCallback = null;
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

    public void getFirmwareVersion(VersionCallback callback) {
        firmwareVersionCallback = callback;
        mGattClient.readCharacteristic(
                mDeviceService.getCharacteristic(
                        Constants.UUID_DEVICE_INFO_CHAR_FIRMWARE_VERSION));
    }

    public void getHardwareVersion(VersionCallback callback) {
        hardwareVersionCallback = callback;
        mGattClient.readCharacteristic(
                mDeviceService.getCharacteristic(
                        Constants.UUID_DEVICE_INFO_CHAR_HARDWARE_VERSION));
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

    public static interface VersionCallback {
        public void onComplete(String version);
    }

    public static interface DeviceInfoCallback {
        public void onDeviceInfo(DeviceInfo info);
    }
}
