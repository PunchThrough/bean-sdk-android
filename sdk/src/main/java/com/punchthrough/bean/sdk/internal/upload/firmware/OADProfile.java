package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.util.Log;

import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.device.DeviceProfile;
import com.punchthrough.bean.sdk.internal.utility.Constants;
import com.punchthrough.bean.sdk.internal.utility.Convert;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.FirmwareBundle;
import com.punchthrough.bean.sdk.upload.FirmwareImage;

import java.util.Arrays;
import java.util.UUID;

public class OADProfile extends BaseProfile {
    /**
     * Custom OAD Profile for LightBlue Bean devices.
     *
     * This class encapsulates data and processes related to firmware updates to the CC2540.
     *
     */

    public static final String TAG = "OADProfile";

    // OAD Characteristic handles
    private BluetoothGattCharacteristic oadIdentify;
    private BluetoothGattCharacteristic oadBlock;

    // OAD Internal State
    private FirmwareUploadState firmwareUploadState = FirmwareUploadState.INACTIVE;
    private FirmwareImage currentImage;
    private FirmwareBundle firmwareBundle;
    private Runnable onComplete;
    private Callback<BeanError> onError;
    private Callback<UploadProgress> onProgress;

    public OADProfile(GattClient client) {
        super(client);
        resetState();
    }

    private void setState(FirmwareUploadState state) {
        Log.i(TAG, String.format("OAD State Change: %s -> %s", firmwareUploadState.name(), state.name()));
        firmwareUploadState = state;
    }

    private void resetState() {
        setState(FirmwareUploadState.INACTIVE);
        onComplete = null;
        onError = null;
        currentImage = null;
    }

    private void complete() {
        onComplete.run();
        resetState();
    }

    private void onNotificationIdentify(BluetoothGattCharacteristic characteristic) {
        currentImage = firmwareBundle.getNextImage();
        Log.i(TAG, "Offering image: " + currentImage.name());
        writeToCharacteristic(oadIdentify, currentImage.metadata());
    }

    private void onNotificationBlock(BluetoothGattCharacteristic characteristic) {
        int blk = Convert.twoBytesToInt(characteristic.getValue(), Constants.CC2540_BYTE_ORDER);

        if (blk == 0) {
            Log.i(TAG, "Image accepted: " + currentImage.name());
            Log.i(TAG, String.format("Starting Block Transfer of %d blocks", currentImage.blockCount()));
            setState(FirmwareUploadState.BLOCK_XFER);
        }

        if (blk % 100 == 0) {
            Log.i(TAG, "Block request: " + blk);
        }

        writeToCharacteristic(oadBlock, currentImage.block(blk));

        if (blk == currentImage.blockCount() - 1) {
            Log.i(TAG, "Last block sent!");

            // In theory, the device may or may not have already lost connection at this point
            // so it seems kind of silly call .disconnect() here. However, it appears
            // that you cannot call .connect() on a GattClient object twice without explicitly
            // disconnecting first!
            mGattClient.disconnect();
            Log.i(TAG, "Waiting for device to reconnect...");
        }
    }

    /**
     * Stop the firmware upload and return an error to the user's {@link #onError} handler.
     *
     * @param error The error to be returned to the user
     */
    private void throwBeanError(BeanError error) {
        if (onError != null) {
            onError.onResult(error);
        }
        resetState();
    }

    /**
     * Setup BLOCK and IDENTIFY characteristics
     */
    private void setupOAD() {
        BluetoothGattService oadService = mGattClient.getService(Constants.UUID_OAD_SERVICE);
        if (oadService == null) {
            throwBeanError(BeanError.MISSING_OAD_SERVICE);
            return;
        }

        oadIdentify = oadService.getCharacteristic(Constants.UUID_OAD_CHAR_IDENTIFY);
        if (oadIdentify == null) {
            throwBeanError(BeanError.MISSING_OAD_IDENTIFY);
            return;
        }

        oadBlock = oadService.getCharacteristic(Constants.UUID_OAD_CHAR_BLOCK);
        if (oadBlock == null) {
            throwBeanError(BeanError.MISSING_OAD_BLOCK);
            return;
        }
    }

    /**
     * Enables notifications for all OAD characteristics.
     */
    private void setupNotifications() {

        Log.d(TAG, "Enabling OAD notifications");

        boolean oadIdentifyNotifying = enableNotifyForChar(oadIdentify);
        boolean oadBlockNotifying = enableNotifyForChar(oadBlock);

        if (oadIdentifyNotifying && oadBlockNotifying) {
            Log.d(TAG, "Enable notifications successful");
        } else {
            throwBeanError(BeanError.ENABLE_OAD_NOTIFY_FAILED);
        }
    }

    /**
     * Enable notifications for a given characteristic.
     *
     * See <a href="https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#notification">
     *     the Android docs
     * </a>
     * on this subject.
     *
     * @param characteristic    The characteristic to enable notifications for
     * @return                  true if notifications were enabled successfully
     */
    private boolean enableNotifyForChar(BluetoothGattCharacteristic characteristic) {
        boolean result = mGattClient.setCharacteristicNotification(characteristic, true);

        String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGattClient.writeDescriptor(descriptor);
        if (result) {
            Log.d(TAG, "Enabled notify for characteristic: " + characteristic.getUuid());
        } else {
            Log.e(TAG, "Enable notify failed for characteristic: " + characteristic.getUuid());
        }
        return result;
    }

    /**
     * Request the Bean's current OAD firmware header.
     */
    private void triggerCurrentHeader() {

        Log.d(TAG, "Requesting current header");
        setState(FirmwareUploadState.OFFERING_IMAGES);

        // To request the current header, write [0x00] to OAD Identify
        writeToCharacteristic(oadIdentify, new byte[]{0x00});
    }

    /**
     * @param charc The characteristic being inspected
     * @return      true if it's the OAD Block characteristic
     */
    private boolean writeToCharacteristic(BluetoothGattCharacteristic charc, byte[] data) {
        charc.setValue(data);
        boolean result = mGattClient.writeCharacteristic(charc);
        if (result) {
            Log.d(TAG, "Wrote to characteristic: " + charc.getUuid() +
                    ", data: " + Arrays.toString(data));
        } else {
            Log.e(TAG, "Write failed to characteristic: " + charc.getUuid() +
                    ", data: " + Arrays.toString(data));
        }
        return result;
    }

    private boolean needsUpdate(Long bundleVersion, String beanVersion) {
        if (beanVersion.startsWith("OAD")) {
            Log.i(TAG, "Bundle version: " + bundleVersion);
            Log.i(TAG, "Bean version: " + beanVersion);
            return true;
        } else {
            long parsedVersion = Long.parseLong(beanVersion.split(" ")[0]);
            Log.i(TAG, "Bundle version: " + bundleVersion);
            Log.i(TAG, "Bean version: " + parsedVersion);
            if (bundleVersion > parsedVersion) {
                return true;
            } else {
                Log.i(TAG, "No update required!");
            }
        }
        return false;
    }

    private void checkFirmwareVersion() {
        Log.i(TAG, "Checking Firmware version...");
        setState(FirmwareUploadState.CHECKING_FW_VERSION);
        mGattClient.getDeviceProfile().getFirmwareVersion(new DeviceProfile.FirmwareVersionCallback() {
            @Override
            public void onComplete(String version) {
                if (needsUpdate(firmwareBundle.version(), version)) {
                    triggerCurrentHeader();
                } else {
                    finishOAD();
                }
            }
        });
    }

    private void finishOAD() {
        Log.i(TAG, "OAD Finished");
        setState(FirmwareUploadState.INACTIVE);
        onComplete.run();
    }

    /****************************************************************************
                                   PUBLIC METHODS
     ****************************************************************************/

    public String getName() {
        return "OAD Profile";
    }

    public boolean uploadInProgress() {
        return firmwareUploadState != FirmwareUploadState.INACTIVE;
    }

    @Override
    public void onProfileReady() {
        setupOAD();
        setupNotifications();
    }

    @Override
    public void onBeanConnected() {
        Log.i(TAG, "OAD Profile Detected Bean Connection!!!");
        if (uploadInProgress()) {
            checkFirmwareVersion();
        }
    }

    @Override
    public void onBeanDisconnected() {
        Log.i(TAG, "OAD Profile Detected Bean Disconnection!!!");

        if(uploadInProgress()) {
            BeanManager.getInstance().startDiscovery();
        }
    }

    @Override
    public void onCharacteristicChanged(GattClient client, BluetoothGattCharacteristic characteristic) {
        if (uploadInProgress()) {

            if (characteristic.getUuid().equals(Constants.UUID_OAD_CHAR_IDENTIFY)) {
                onNotificationIdentify(characteristic);
            } else if (characteristic.getUuid().equals(Constants.UUID_OAD_CHAR_BLOCK)) {
                onNotificationBlock(characteristic);
            }
        }
    }

    /**
     * Program the Bean's CC2540 with new firmware.
     *
     * @param bundle        The {@link com.punchthrough.bean.sdk.upload.FirmwareBundle} to be sent
     * @param onProgress    Called when progress is made during the upload
     * @param onComplete    Called when the upload is complete
     * @param onError       Called if an error occurs during the upload
     */
    public void programWithFirmware(final FirmwareBundle bundle, final Callback<UploadProgress> onProgress,
                                    final Runnable onComplete, Callback<BeanError> onError) {

        if (!mGattClient.isConnected()) {
            onError.onResult(BeanError.NOT_CONNECTED);
        }

        Log.i(TAG, "Starting firmware update procedure!");

        // Save state for this firmware procedure
        this.onComplete = onComplete;
        this.onError = onError;
        this.onProgress = onProgress;
        this.firmwareBundle = bundle;

        checkFirmwareVersion();
    }
}
