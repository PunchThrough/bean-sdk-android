package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.device.DeviceProfile;
import com.punchthrough.bean.sdk.internal.utility.Constants;
import com.punchthrough.bean.sdk.internal.utility.Convert;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
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

    /**
     * The OAD Service contains the OAD Identify and Block characteristics
     */
    private static final UUID SERVICE_OAD = UUID.fromString("F000FFC0-0451-4000-B000-000000000000");

    /**
     * The OAD Identify characteristic is used to negotiate the start of a firmware transfer
     */
    private static final UUID CHAR_OAD_IDENTIFY = UUID.fromString("F000FFC1-0451-4000-B000-000000000000");

    /**
     * The OAD Block characteristic is used to send firmware blocks and confirm transfer completion
     */
    private static final UUID CHAR_OAD_BLOCK = UUID.fromString("F000FFC2-0451-4000-B000-000000000000");

    /**
     * The OAD Identify characteristic for this device. Assigned when firmware upload is started.
     */
    private BluetoothGattCharacteristic oadIdentify;

    /**
     * The OAD Block characteristic for this device. Assigned when firmware upload is started.
     */
    private BluetoothGattCharacteristic oadBlock;
    
    /**
     * State of the current firmware upload process.
     */
    private FirmwareUploadState firmwareUploadState;

    /**
     * The most recently offered firmware image
     */
    private FirmwareImage currentImage;

    /**
     * Firmware bundle with A and B images to send
     */
    FirmwareBundle firmwareBundle;

    /**
     * Called to inform the Bean class when firmware upload is complete.
     */
    private Runnable onComplete;

    /**
     * Called when an error causes the firmware upload to fail.
     */
    private Callback<BeanError> onError;

    public OADProfile(GattClient client) {
        super(client);
        resetState();
    }

    private void resetState() {
        firmwareUploadState = FirmwareUploadState.INACTIVE;
        onComplete = null;
        onError = null;
        currentImage = null;
    }

    private void complete() {
        onComplete.run();
        resetState();
    }


    private void onNotificationIdentify(BluetoothGattCharacteristic characteristic) {
        FirmwareImage nextImage = firmwareBundle.getNextImage();
        Log.i(TAG, "Offering image: " + nextImage.uniqueID().toString());
        writeToCharacteristic(oadIdentify, nextImage.metadata());
    }

    private void onNotificationBlock(BluetoothGattCharacteristic characteristic) {
        int blk = Convert.twoBytesToInt(characteristic.getValue(), Constants.CC2540_BYTE_ORDER);
        Log.d(TAG, "Sending requested FW block " + blk);
        writeToCharacteristic(oadBlock, currentImage.block(blk));

        if (blk == currentImage.blockCount() - 1) {
            Log.d(TAG, "Last block requested");

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
        BluetoothGattService oadService = mGattClient.getService(SERVICE_OAD);
        if (oadService == null) {
            throwBeanError(BeanError.MISSING_OAD_SERVICE);
            return;
        }

        oadIdentify = oadService.getCharacteristic(CHAR_OAD_IDENTIFY);
        if (oadIdentify == null) {
            throwBeanError(BeanError.MISSING_OAD_IDENTIFY);
            return;
        }

        oadBlock = oadService.getCharacteristic(CHAR_OAD_BLOCK);
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
        firmwareUploadState = FirmwareUploadState.OFFERING_IMAGES;

        // To request the current header, write [0x00] to OAD Identify
        writeToCharacteristic(oadIdentify, new byte[]{0x00});

    }

    /**
     * @return true if an upload is in progress
     */
    private boolean uploadInProgress() {
        return firmwareUploadState != FirmwareUploadState.INACTIVE;
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

    /****************************************************************************
                                   PUBLIC METHODS
     ****************************************************************************/

    @Override
    public void onProfileReady() {
        setupOAD();
        setupNotifications();
    }

    @Override
    public void onBeanConnected() {

    }

    @Override
    public void onBeanDisconnected() {

    }

    @Override
    public void onCharacteristicChanged(GattClient client, BluetoothGattCharacteristic characteristic) {
        if (uploadInProgress()) {

            if (characteristic.getUuid().equals(CHAR_OAD_IDENTIFY)) {
                onNotificationIdentify(characteristic);
            } else if (characteristic.getUuid().equals(CHAR_OAD_BLOCK)) {
                onNotificationBlock(characteristic);
            }
        }
    }

    public FirmwareUploadState getState() {
        return firmwareUploadState;
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

        Log.d(TAG, "Starting firmware update procedure!");

        // Save state for this firmware procedure
        this.onComplete = onComplete;
        this.onError = onError;
        this.firmwareBundle = bundle;

        Log.d(TAG, "Checking Firmware version...");
        firmwareUploadState = FirmwareUploadState.CHECKING_FW_VERSION;
        mGattClient.getDeviceProfile().getDeviceInfo(new DeviceProfile.DeviceInfoCallback() {
            @Override
            public void onDeviceInfo(DeviceInfo info) {
                long beanVersion = Long.parseLong(info.firmwareVersion().split(" ")[0]);
                if (bundle.version() > beanVersion) {
                    triggerCurrentHeader();
                } else {
                    onComplete.run();
                }
            }
        });

    }
}
