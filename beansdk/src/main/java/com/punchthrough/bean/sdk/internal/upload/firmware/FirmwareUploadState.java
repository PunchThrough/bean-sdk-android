package com.punchthrough.bean.sdk.internal.upload.firmware;

public enum FirmwareUploadState {
    IDLE,
    ENABLE_NOTIFY,
    REQUEST_CURRENT_HEADER,
    AWAIT_XFER_ACCEPT,
    SEND_FW_CHUNKS,
    AWAIT_COMPLETION
}
