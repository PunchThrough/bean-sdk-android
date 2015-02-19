package com.punchthrough.bean.sdk.internal.bootloader;

public enum ClientState {
    INACTIVE, RESETTING_REMOTE, SENDING_START_COMMAND, SENDING_CHUNKS, FINISHED
}
