package com.punchthrough.bean.sdk.internal.utility;

import java.nio.ByteOrder;

/**
 * Constants used throughout the Bean SDK.
 */
public class Constants {

    /**
     * Maximum allowed length for the name of a sketch being programmed to the Bean.
     */
    public static final int MAX_SKETCH_NAME_LENGTH = 20;

    /**
     * The byte order used by the CC2540
     */
    public static final ByteOrder CC2540_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    /**
     * The firmware Unique IDs that indicate Image Type A or B
     */
    public static final String IMAGE_A_ID = "AAAA";
    public static final String IMAGE_B_ID = "BBBB";

}
