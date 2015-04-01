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
     * Timed out during sketch programming, before sending blocks: Bean took too long to update its
     * current state
     */
    STATE_TIMEOUT,
    /**
     * Firmware version to send could not be parsed from OAD request header
     */
    UNPARSABLE_FW_VERSION,
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
     * Bean rejected firmware version for being older than the current version
     */
    BEAN_REJECTED_FW,
    /**
     * Bean responded with a message with an ID we don't know anything about
     */
    UNKNOWN_MESSAGE_ID,
    /**
     * Bean did not provide a reason for the error
     */
    UNKNOWN
}
