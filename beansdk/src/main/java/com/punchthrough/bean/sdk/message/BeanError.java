package com.punchthrough.bean.sdk.message;

/**
 * Represents an error returned by the Bean.
 */
public enum BeanError {
    /**
     * Timed out while waiting for state to update during programming, but before sending chunks
     */
    STATE_TIMEOUT,
    /**
     * Bean did not provide a reason for the error
     */
    UNKNOWN
}
