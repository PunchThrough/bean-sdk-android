package com.punchthrough.bean.sdk.message;

/**
 * Represents an error returned by the Bean.
 */
public enum BeanError {
    /**
     * Bean is not connected
     */
    NOT_CONNECTED,
    /**
     * Bean's services have not yet been discovered
     */
    SERVICES_NOT_DISCOVERED,
    /**
     * OAD service was not found
     */
    MISSING_OAD_SERVICE,
    /**
     * OAD Identify characteristic was not found
     */
    MISSING_OAD_IDENTIFY,
    /**
     * OAD Block characteristic was not found
     */
    MISSING_OAD_BLOCK,
    /**
     * Notifications could not be enabled for either OAD Identify, Block, or both characteristics
     */
    ENABLE_OAD_NOTIFY_FAILED,
    /**
     * Timed out during sketch programming, before sending chunks: Bean took too long to update its
     * current state
     */
    STATE_TIMEOUT,
    /**
     * Firmware metadata could not be parsed from OAD request header
     */
    UNPARSABLE_FW_METADATA,
    /**
     * Timed out requesting current firmware version
     */
    FW_VER_REQ_TIMEOUT,
    /**
     * Timed out starting firmware download
     */
    FW_START_TIMEOUT,
    /**
     * Timed out while sending firmware packets
     */
    FW_TRANSFER_TIMEOUT,
    /**
     * Timed out while waiting for confirmation of firmware upload completion
     */
    FW_COMPLETE_TIMEOUT,
    /**
     * Bean rejected firmware version for being older than the current version
     */
    BEAN_REJECTED_FW,
    /**
     * Bean did not provide a reason for the error
     */
    UNKNOWN
}
