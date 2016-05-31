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
import com.punchthrough.bean.sdk.internal.utility.Watchdog;
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

    protected static final String TAG = "OADProfile";

    // OAD Characteristic handles
    private BluetoothGattCharacteristic oadIdentify;
    private BluetoothGattCharacteristic oadBlock;

    private Watchdog watchdog;

    // OAD Internal State

    private static final int OAD_TIMEOUT_SECONDS = 30;
    private boolean ready = false;

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

    public OADProfile(GattClient client, Watchdog watchdog) {
        super(client);
        this.watchdog = watchdog;
        reset();
    }

    private Watchdog.WatchdogListener watchdogListener = new Watchdog.WatchdogListener() {
        @Override
        public void expired() {
            fail(BeanError.OAD_TIMEOUT);
        }
    };

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
            fail(BeanError.CLIENT_REJECTED);
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
        Log.i(TAG, String.format("(%s) OAD State Change: %s -> %s", mGattClient.bleAddress(), oadState.name(), state.name()));
        oadState = state;
        watchdog.poke();
        if (oadListener != null ) {
            oadListener.stateChange(state);
        }
    }

    /**
     * Set the state to INACTIVE and clear state variables
     */
    private void reset() {
        setState(OADState.INACTIVE);
        currentImage = null;
        firmwareBundle = null;
        nextBlock = 0;
        oadListener = null;
        watchdog.stop();
        oadApproval.reset();
    }

    /**
     * Attempt to reconnect to a Bean that is in the middle of OAD process
     */
    private void reconnect() {
        if(uploadInProgress()) {
            setState(OADState.RECONNECTING);
            BeanManager.getInstance().startDiscovery();
            Log.i(TAG, "Waiting for device to reconnect...");
        }
    }

    /**
     * Offer the next image available in the Firmware Bundle
     */
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

    /**
     * Begin the OFFERING_IMAGES state
     */
    private void startOfferingImages() {
        setState(OADState.OFFERING_IMAGES);
        currentImage = null;
        firmwareBundle.reset();
        offerNextImage();
    }

    /**
     *  Received a notification on Identify characteristic
     *
     * @param characteristic Not used
     */
    private void onNotificationIdentify(BluetoothGattCharacteristic characteristic) {
        offerNextImage();
    }

    /**
     * Received a notification on Block characteristic
     *
     * A notification to this characteristic means the Bean has accepted the most recent
     * firmware file we have offered, which is stored as `this.currentImage`. It is now
     * time to start sending blocks of FW to the device.
     *
     * @param characteristic BLE characteristic with a value equal to the the block number
     */
    private void onNotificationBlock(BluetoothGattCharacteristic characteristic) {

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

            // Write the block, tell the OAD Listener
            writeToCharacteristic(oadBlock, currentImage.block(nextBlock));
            oadListener.progress(UploadProgress.create(nextBlock + 1, currentImage.blockCount()));
            nextBlock++;
            watchdog.poke();
        }

        // Check for final block requested, for logging purposes only
        if (requestedBlock == currentImage.blockCount() - 1) {

            // Calculate throughput
            long secondsElapsed = System.currentTimeMillis() / 1000L - blockTransferStarted;
            double KBs = 0;
            if (secondsElapsed > 0) {
                KBs = (double) (currentImage.sizeBytes() / secondsElapsed) / 1000;
            }

            // Log some stats
            Log.i(TAG, String.format("Final OAD Block Requested: %s/%s", nextBlock, currentImage.blockCount()));
            Log.i(TAG, String.format("Sent %d blocks in %d seconds (%.2f KB/s)", currentImage.blockCount(), secondsElapsed, KBs));
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

        Log.i(TAG, "Enabling OAD notifications");

        boolean oadIdentifyNotifying = enableNotifyForChar(oadIdentify);
        boolean oadBlockNotifying = enableNotifyForChar(oadBlock);

        if (oadIdentifyNotifying && oadBlockNotifying) {
            Log.i(TAG, "Enable notifications successful");
        } else {
            Log.e(TAG, "Error while enabling notifications");
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

        boolean success = true;

        // Enable notifications/indications for this characteristic
        boolean successEnable = mGattClient.setCharacteristicNotification(characteristic, true);
        if (successEnable) {
            Log.i(TAG, "Enabled notify for characteristic: " + characteristic.getUuid());
        } else {
            success = false;
            Log.e(TAG, "Enable notify failed for characteristic: " + characteristic.getUuid());
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Constants.UUID_CLIENT_CHAR_CONFIG);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean successDescriptor = mGattClient.writeDescriptor(descriptor);

        if (successDescriptor) {
            Log.i(TAG, "Successfully wrote notification descriptor: " + descriptor.getUuid());
        } else {
            success = false;
            Log.e(TAG, "Failed to write notification descriptor: " + descriptor.getUuid());
        }

        return success;
    }

    /**
     * Write to a OAD characteristic
     *
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

    /**
     * Helper function to determine whether a Bean needs a FW update given a specific Bundle version
     *
     * @param bundleVersion the version string from the provided firmware bundle
     * @param beanVersion the version string provided from the Bean Device Information Service
     * @return boolean value stating whether the Bean needs an update
     */
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

    /**
     * Check the Beans FW version to determine if an update is required
     */
    private void checkFirmwareVersion() {
        Log.i(TAG, "Checking Firmware version...");
        setState(OADState.CHECKING_FW_VERSION);
        mGattClient.getDeviceProfile().getFirmwareVersion(new DeviceProfile.VersionCallback() {
            @Override
            public void onComplete(String version) {

                // Check the Bean version against the Bundle version
                boolean updateNeeded = needsUpdate(firmwareBundle.version(), version);

                if (updateNeeded && oadApproval.isApproved()) {
                    // Needs update and client has approved, keep the update going

                    startOfferingImages();

                } else if (updateNeeded && !oadApproval.isApproved()) {
                    // Needs update but client has not approved, ask for approval

                    watchdog.pause();
                    oadListener.updateRequired(true);

                } else if (!updateNeeded && !oadApproval.isApproved()){
                    // Does not need update and the client has never approved. This means
                    // no update is required, and no firmware update ever occurred

                    oadListener.updateRequired(false);
                    finishNoUpdateOccurred();

                } else if (!updateNeeded && oadApproval.isApproved()) {
                    // Does not need update, and the client has approved. This means that
                    // the firmware process actually took place, and completed

                    finishUpdateOccurred();

                } else {
                    Log.w(TAG, "Unexpected OAD Condition!");
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
        Log.e(TAG, "OAD Error: " + error.toString());
        if (uploadInProgress()) {
            oadListener.error(error);
            reset();
        }
    }

    /**
     * Finish the OAD process, similar to fail() except assumes a better outcome
     */
    private void finishUpdateOccurred() {
        Log.i(TAG, "OAD Finished: Update Occurred");
        oadListener.complete();
        reset();
    }

    private void finishNoUpdateOccurred() {
        Log.i(TAG, "OAD Finished: No update Occurred");

        // Don't reset() here, just set state to inactive. By not resetting, this allows
        // the client to force an OAD update even if there isn't one required.
        watchdog.stop();
        setState(OADState.INACTIVE);
    }

    /****************************************************************************
                                   PUBLIC METHODS
     ****************************************************************************/

    public String getName() {
        return TAG;
    }

    public boolean isReady() {
        return ready;
    }

    public void clearReady() {
        ready = false;
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
        ready = true;
    }

    public void continueOAD() {
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
        reconnect();
    }

    @Override
    public void onBeanConnectionFailed() {
        Log.i(TAG, "OAD Profile Detected Connection Failure, Likely a device reboot");
        reconnect();
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

        watchdog.start(OAD_TIMEOUT_SECONDS, watchdogListener);
        checkFirmwareVersion();

        return this.oadApproval;
    }

    /**
     * Communication interface from SDK to client
     */
    public interface OADListener {

        /**
         * Called when the OAD procedure completes without error
         */
        public void complete();

        /**
         * Called when there is an error during the OAD procedure
         *
         * @param error BeanError
         */
        public void error(BeanError error);

        /**
         * Called when forward progress has been made during OAD procedure
         *
         * @param uploadProgress UploadProgress object describing the OAD procedure progress
         */
        public void progress(UploadProgress uploadProgress);

        /**
         * Called to let the client know if a FW update is required
         *
         * @param required Boolean flag
         */
        public void updateRequired(boolean required);

        /**
         * Called when the OAD state machine changes states
         *
         * @param state OADState representing the state of the OAD procedure
         */
        public void stateChange(OADState state);
    }

    /**
     * Communication interface from client to SDK
     */
    public interface OADApproval {

        /**
         * Client should call this method to allow the OAD procedure to continue
         */
        public void allow();

        /**
         * Client should call this method to deny the OAD procedure from continuing
         */
        public void deny();

        /**
         * Reset this objects state
         */
        public void reset();

        /**
         *
         * @return Boolean flag representing the last approval status
         */
        public boolean isApproved();
    }
}
