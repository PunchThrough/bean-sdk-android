package com.punchthrough.bean.sdk.internal.upload.firmware;

public enum FirmwareUploadState {
    /**
     * No firmware upload is in progress
     */
    INACTIVE,
    /**
     * Waiting for notify to be enabled on OAD characteristics
     */
    AWAIT_NOTIFY_ENABLED,
    /**
     * Waiting for current OAD request header
     */
    AWAIT_CURRENT_HEADER,
    /**
     * Wrote the OAD response header, waiting for the device to accept a FW transfer
     */
    AWAIT_XFER_ACCEPT,
    /**
     * FW transfer accepted, currently sending chunks
     */
    SEND_FW_CHUNKS,
    /**
     * Last FW chunk sent, waiting for device to confirm transfer is complete
     */
    AWAIT_COMPLETION
}
