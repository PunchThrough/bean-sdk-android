package com.punchthrough.bean.sdk.internal.utility;

import java.nio.ByteOrder;
import java.util.UUID;

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


    /**
     * UUIDs
     */
//    public static final UUID UUID_BATTERY_SERVICE = UUID.fromString("180f");
//    public static final UUID UUID_BATTERY_CHARACTERISTIC = UUID.fromString("2a19");

    public static final UUID BEAN_SERIAL_CHARACTERISTIC_UUID = UUID.fromString("a495ff11-c5b1-4b44-b512-1370f02d74de");
    public static final UUID BEAN_SERIAL_SERVICE_UUID = UUID.fromString("a495ff10-c5b1-4b44-b512-1370f02d74de");

    public static final UUID BEAN_SCRATCH_SERVICE_UUID = UUID.fromString("a495ff20-c5b1-4b44-b512-1370f02d74de");
    public static final UUID BEAN_SCRATCH_1_CHAR_UUID = UUID.fromString("a495ff21-c5b1-4b44-b512-1370f02d74de");
    public static final UUID BEAN_SCRATCH_2_CHAR_UUID = UUID.fromString("a495ff22-c5b1-4b44-b512-1370f02d74de");
    public static final UUID BEAN_SCRATCH_3_CHAR_UUID = UUID.fromString("a495ff23-c5b1-4b44-b512-1370f02d74de");
    public static final UUID BEAN_SCRATCH_4_CHAR_UUID = UUID.fromString("a495ff24-c5b1-4b44-b512-1370f02d74de");
    public static final UUID BEAN_SCRATCH_5_CHAR_UUID = UUID.fromString("a495ff25-c5b1-4b44-b512-1370f02d74de");

}
