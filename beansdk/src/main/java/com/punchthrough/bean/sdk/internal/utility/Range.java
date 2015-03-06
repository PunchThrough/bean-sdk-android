package com.punchthrough.bean.sdk.internal.utility;

/**
 * Utilities for constraining numbers to specific ranges.
 */
public class Range {

    /**
     * Clamp an int to a min/max value.
     *
     * @param n     The value to be clamped
     * @param min   The minimum
     * @param max   The maximum
     * @return      The value passed in, or minimum if n &lt; minimum, or maximum if n &gt; maximum
     */
    public static int clamp(int n, int min, int max) {
        if (n < min) return min;
        if (n > max) return max;
        return n;
    }

    /**
     * Clamp an int to the uint8 (0-255) range.
     *
     * @param n The value to be clamped
     * @return  The value clamped between 0 and 255
     */
    public static int clampToUInt8(int n) {
        return clamp(n, 0, 255);
    }

}
