package com.punchthrough.bean.sdk.internal.utility;

import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Constants used throughout the Bean SDK.
 */
public class Constants {

    private static UUID smallUUID(int hexBytes) {
        return UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", hexBytes));
    }

    /**
     * Maximum allowed length for the name of a sketch being programmed to the Bean.
     */
    public static final int MAX_SKETCH_NAME_LENGTH = 20;

    /**
     * The byte order used by the CC2540
     */
    public static final ByteOrder CC2540_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    /**
     * Device Profile UUIDs
     */
    public static final UUID UUID_DEVICE_INFO_SERVICE = smallUUID(0x180A);
    public static final UUID UUID_DEVICE_INFO_CHAR_HARDWARE_VERSION = smallUUID(0x2A27);
    public static final UUID UUID_DEVICE_INFO_CHAR_FIRMWARE_VERSION = smallUUID(0x2A26);
    public static final UUID UUID_DEVICE_INFO_CHAR_SOFTWARE_VERSION = smallUUID(0x2A28);

    /**
     * Battery UUIDs
     */
    public static final UUID UUID_BATTERY_SERVICE = smallUUID(0x180F);
    public static final UUID UUID_BATTERY_CHARACTERISTIC = smallUUID(0x2A19);

    /**
     * Serial UUIDs
     */
    public static final UUID UUID_SERIAL_SERVICE = UUID.fromString("a495ff10-c5b1-4b44-b512-1370f02d74de");
    public static final UUID UUID_SERIAL_CHAR = UUID.fromString("a495ff11-c5b1-4b44-b512-1370f02d74de");

    /**
     * Scratch UUIDs
     */
    public static final UUID UUID_SCRATCH_SERVICE = UUID.fromString("a495ff20-c5b1-4b44-b512-1370f02d74de");
    public static final UUID UUID_SCRATCH_CHAR_1 = UUID.fromString("a495ff21-c5b1-4b44-b512-1370f02d74de");
    public static final UUID UUID_SCRATCH_CHAR_2 = UUID.fromString("a495ff22-c5b1-4b44-b512-1370f02d74de");
    public static final UUID UUID_SCRATCH_CHAR_3 = UUID.fromString("a495ff23-c5b1-4b44-b512-1370f02d74de");
    public static final UUID UUID_SCRATCH_CHAR_4 = UUID.fromString("a495ff24-c5b1-4b44-b512-1370f02d74de");
    public static final UUID UUID_SCRATCH_CHAR_5 = UUID.fromString("a495ff25-c5b1-4b44-b512-1370f02d74de");

    /**
     * OAD UUIDs
     */
    public static final UUID UUID_OAD_SERVICE = UUID.fromString("F000FFC0-0451-4000-B000-000000000000");
    public static final UUID UUID_OAD_CHAR_IDENTIFY = UUID.fromString("F000FFC1-0451-4000-B000-000000000000");
    public static final UUID UUID_OAD_CHAR_BLOCK = UUID.fromString("F000FFC2-0451-4000-B000-000000000000");

    // Used for registering BLE characteristic notifications
    public static final UUID UUID_CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

}
