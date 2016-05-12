package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.device.DeviceProfile;
import com.punchthrough.bean.sdk.internal.exception.OADException;
import com.punchthrough.bean.sdk.internal.utility.Constants;
import com.punchthrough.bean.sdk.internal.utility.Convert;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.FirmwareBundle;
import com.punchthrough.bean.sdk.upload.FirmwareImage;

import java.util.Arrays;

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

    /* The current state of the OAD state-machine */
    private OADState oadState = OADState.INACTIVE;

    /* The last offered image or the accepted image depending on state of the OAD process */
    private FirmwareImage currentImage;

    /* Bundle of firmware images provided by the client */
    private FirmwareBundle firmwareBundle;

    /* Oad Listener */
    private OADListener oadListener;

    /* The maximum allowed blocks queued and/or in-flight before receiving a new request */
    private final int MAX_IN_AIR_BLOCKS = 8;

    /* Keeps track of the next block to send which is not equal to the block requested */
    private int nextBlock = 0;

    /* Used to record KB/s during block transfers */
    private long blockTransferStarted = 0;

    public OADProfile(GattClient client) {
        super(client);
        reset();
    }

    private OADApproval oadApproval = new OADApproval() {

        public boolean approved = false;

        @Override
        public void allow() {
            Log.i(TAG, "Client has allowed the OAD Process to continue.");
            approved = true;
            startOfferingImages();
        }

        @Override
        public void deny() {
            Log.i(TAG, "Client denied the OAD Process from continuing.");
            approved = false;
            finish();
        }

        @Override
        public void reset() {
            approved = false;
        }

        @Override
        public boolean isApproved() {
            return approved;
        }
    };

    private void setState(OADState state) {
        Log.i(TAG, String.format("OAD State Change: %s -> %s", oadState.name(), state.name()));
        oadState = state;
    }

    private void reset() {
        setState(OADState.INACTIVE);
        currentImage = null;
        oadApproval.reset();
    }

    private void offerNextImage() {
        if (oadState == OADState.OFFERING_IMAGES) {
            try {
                currentImage = firmwareBundle.getNextImage();
                if (currentImage != null) {
                    Log.i(TAG, "Offering image: " + currentImage.name());
                    writeToCharacteristic(oadIdentify, currentImage.metadata());
                }
            } catch (OADException e) {
                // This gets thrown if the firmware bundle is "exhausted", meaning the Bean
                // has rejected all of the images in the bundle
                Log.e(TAG, e.getMessage());
                fail(BeanError.BEAN_REJECTED_FW);
            }
        } else {
            Log.e(TAG, "Got notification on OAD Identify while in unexpected state: " + oadState);
        }
    }

    private void startOfferingImages() {
        setState(OADState.OFFERING_IMAGES);
        currentImage = null;
        firmwareBundle.reset();
        offerNextImage();
    }

    private void onNotificationIdentify(BluetoothGattCharacteristic characteristic) {
        offerNextImage();
    }

    private void onNotificationBlock(BluetoothGattCharacteristic characteristic) {
        /**
         * Received a notification on Block characteristic
         *
         * A notification to this characteristic means the Bean has accepted the most recent
         * firmware file we have offered, which is stored as `this.currentImage`. It is now
         * time to start sending blocks of FW to the device.
         *
         * @param characteristic BLE characteristic with a value equal to the the block number
         */

        int requestedBlock = Convert.twoBytesToInt(characteristic.getValue(), Constants.CC2540_BYTE_ORDER);

        // Check for First block
        if (requestedBlock == 0) {
            Log.i(TAG, String.format("Image accepted (Name: %s) (Size: %s bytes)",currentImage.name(), currentImage.sizeBytes()));
            blockTransferStarted = System.currentTimeMillis() / 1000L;
            setState(OADState.BLOCK_XFER);
            nextBlock = 0;
        }

        // Normal BLOCK XFER state logic
        while (oadState == OADState.BLOCK_XFER &&
               nextBlock <= currentImage.blockCount() - 1 &&
               nextBlock < (requestedBlock + MAX_IN_AIR_BLOCKS)) {

            writeToCharacteristic(oadBlock, currentImage.block(nextBlock));
            oadListener.progress(UploadProgress.create(requestedBlock, currentImage.blockCount()));

            if (nextBlock % 50 == 0) {
                Log.i(TAG, String.format("OAD Block SENT: %s/%s", nextBlock, currentImage.blockCount()));
            }

            nextBlock++;
        }

        // Check for final block
        if (nextBlock >= currentImage.blockCount()) {

            // Log final block
            Log.i(TAG, String.format("OAD Block SENT: %s/%s", nextBlock - 1, currentImage.blockCount()));

            // Log timing and throughput
            long secondsElapsed = System.currentTimeMillis() / 1000L - blockTransferStarted;
            double KBs = 0;
            if (secondsElapsed > 0) {
                KBs = (double) (currentImage.sizeBytes() / secondsElapsed) / 1000;
            }
            String blkTimeMsg = String.format(
                    "Sent %d blocks in %d seconds (%.2f KB/s)",
                    currentImage.blockCount(),
                    secondsElapsed,
                    KBs
            );
            Log.i(TAG, blkTimeMsg);

            // Change states
            setState(OADState.RECONNECTING);
            Log.i(TAG, "Waiting for device to reconnect...");
            nextBlock = 0;
        }

    }

    /**
     * Setup BLOCK and IDENTIFY characteristics
     */
    private void setupOAD() {
        BluetoothGattService oadService = mGattClient.getService(Constants.UUID_OAD_SERVICE);
        if (oadService == null) {
            fail(BeanError.MISSING_OAD_SERVICE);
            return;
        }

        oadIdentify = oadService.getCharacteristic(Constants.UUID_OAD_CHAR_IDENTIFY);
        if (oadIdentify == null) {
            fail(BeanError.MISSING_OAD_IDENTIFY);
            return;
        }

        oadBlock = oadService.getCharacteristic(Constants.UUID_OAD_CHAR_BLOCK);
        if (oadBlock == null) {
            fail(BeanError.MISSING_OAD_BLOCK);
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
            fail(BeanError.ENABLE_OAD_NOTIFY_FAILED);
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

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Constants.UUID_CLIENT_CHAR_CONFIG);
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
        if (beanVersion.contains("OAD")) {
            Log.i(TAG, "Bundle version: " + bundleVersion);
            Log.i(TAG, "Bean version: " + beanVersion);
            return true;
        } else {
            try {
                long parsedVersion = Long.parseLong(beanVersion.split(" ")[0]);
                Log.i(TAG, "Bundle version: " + bundleVersion);
                Log.i(TAG, "Bean version: " + parsedVersion);
                if (bundleVersion > parsedVersion) {
                    return true;
                } else {
                    Log.i(TAG, "No update required!");
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Couldn't parse Bean Version: " + beanVersion);
                fail(BeanError.UNPARSABLE_FW_VERSION);
            }
        }
        return false;
    }

    private void checkFirmwareVersion() {
        Log.i(TAG, "Checking Firmware version...");
        setState(OADState.CHECKING_FW_VERSION);
        mGattClient.getDeviceProfile().getFirmwareVersion(new DeviceProfile.VersionCallback() {
            @Override
            public void onComplete(String version) {

                boolean updateNeeded = needsUpdate(firmwareBundle.version(), version);
                if (updateNeeded && oadApproval.isApproved()) {
                    // Needs update and client has approved, keep the update going
                    startOfferingImages();
                } else if (updateNeeded && !oadApproval.isApproved()) {
                    // Needs update but client has not approved, ask for approval
                    oadListener.updateRequired(true);
                } else {
                    // Does not need update
                    oadListener.updateRequired(false);
                    finish();
                }
            }
        });
    }

    /**
     * Stop the firmware upload and alert the OADListener
     *
     * @param error The error to be returned to the user
     */
    private void fail(BeanError error) {
        oadListener.error(error);
        reset();
    }

    private void finish() {
        Log.i(TAG, "OAD Finished");
        reset();
        oadListener.complete();
    }

    /****************************************************************************
                                   PUBLIC METHODS
     ****************************************************************************/

    public String getName() {
        return "OAD Profile";
    }

    public boolean uploadInProgress() {
        return oadState != OADState.INACTIVE;
    }

    public OADState getState() {
        return oadState;
    }

    @Override
    public void onProfileReady() {
        setupOAD();
        setupNotifications();
    }

    @Override
    public void beanReady() {
        if (uploadInProgress()) {
            checkFirmwareVersion();
            BeanManager.getInstance().cancelDiscovery();
        }
    }

    @Override
    public void onBeanConnected() {
        Log.i(TAG, "OAD Profile Detected Bean Connection");
    }

    @Override
    public void onBeanDisconnected() {
        Log.i(TAG, "OAD Profile Detected Bean Disconnection");
    }

    @Override
    public void onBeanConnectionFailed() {
        Log.i(TAG, "OAD Profile Detected Connection Failure, Likely a device reboot");
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
     * @param listener      Listener object to alert client of events/state of the Firmware update process
     */
    public OADApproval programWithFirmware(final FirmwareBundle bundle, OADListener listener) {

        if (!mGattClient.isConnected()) {
            listener.error(BeanError.NOT_CONNECTED);
        }

        Log.i(TAG, "Starting firmware update procedure");

        // Save state for this firmware procedure
        this.oadListener = listener;
        this.firmwareBundle = bundle;

        checkFirmwareVersion();

        return this.oadApproval;
    }

    public interface OADListener {
        public void complete();
        public void error(BeanError error);
        public void progress(UploadProgress uploadProgress);
        public void updateRequired(boolean required);
    }

    public interface OADApproval {
        public void allow();
        public void deny();
        public void reset();
        public boolean isApproved();
    }
}
