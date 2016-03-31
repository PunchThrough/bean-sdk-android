package com.punchthrough.bean.sdk.internal.upload.firmware;

public enum OADState {
    /**
     * No firmware upload is in progress
     */
    INACTIVE,

    /**
     * Making sure the Bean needs an update based on the bundle version
     */
    CHECKING_FW_VERSION,

    /**
     * Offering each image available in the FW bundle (4 total - A, B, C, D)
     */
    OFFERING_IMAGES,

    /**
     * Transferring the blocks of an accepted image
     */
    BLOCK_XFER,

    /**
     * Waiting for device to reboot and reconnect
     */
    RECONNECTING,

}
