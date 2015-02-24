package com.punchthrough.bean.sdk.message;

/**
 * Represents an error returned by the Bean.
 */
public enum BeanError {
    /**
     * Timed out during sketch programming, before sending chunks: Bean took too long to update its
     * current state
     */
    STATE_TIMEOUT,
    /**
     * Timed out configuring OAD characteristics
     */
    OAD_CONFIG_TIMEOUT,
    /**
     * Timed out requesting current firmware version
     */
    CURR_FW_VER_TIMEOUT,
    /**
     * Timed out starting firmware download
     */
    FW_START_TIMEOUT,
    /**
     * Timed out while sending firmware packets
     */
    FW_DOWNLOAD_TIMEOUT,
    /**
     * Bean did not provide a reason for the error
     */
    UNKNOWN
}
