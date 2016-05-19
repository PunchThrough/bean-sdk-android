package com.punchthrough.bean.sdk.internal.utility;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Utilities to help cast data from one type to another. Useful for working with types Java doesn't
 * support such as unsigned bytes.
 */
public class Convert {


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Convert a string of ASCII hex characters (e.g. "DEADBEEF0042") to an array of bytes the hex
     * represents (e.g. [0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x42]). Treat bytes returned by this method
     * as unsigned.
     *
     * @param hex               The string of hex characters
     * @return                  An array of bytes the string represents
     * @throws org.apache.commons.codec.DecoderException If the string passed in isn't made up of valid hex bytes
     */
    public static byte[] asciiHexToBytes(String hex) throws DecoderException {
        return Hex.decodeHex(hex.toCharArray());
    }

    /**
     * Convert two unsigned bytes to one signed int.
     * <a href="http://stackoverflow.com/a/4768950/254187">Based on this StackOverflow ansewr.</a>
     *
     * @param high  The high byte
     * @param low   The low byte
     * @return      The high byte combined with the low byte as an unsigned int
     */
    public static int bytesToInt(byte high, byte low) {
        return ( (high & 0xFF) << 8 ) | ( low & 0xFF );
    }

    /**
     * Convert an array of two unsigned bytes with the given byte order to one signed int.
     *
     * @param bytes The bytes to be parsed
     * @param order The byte order to be used
     * @return      An int representing the bytes in the given order
     */
    public static int twoBytesToInt(byte[] bytes, ByteOrder order) {

        if (order == ByteOrder.BIG_ENDIAN) {
            return bytesToInt(bytes[0], bytes[1]);

        } else if (order == ByteOrder.LITTLE_ENDIAN) {
            return bytesToInt(bytes[1], bytes[0]);

        } else {
            throw new IllegalArgumentException("ByteOrder must be BIG_ENDIAN or LITTLE_ENDIAN");

        }
    }

    public static byte[] intToTwoBytes(int i, ByteOrder order) {

        byte[] bytes = ByteBuffer.allocate(4).order(order).putInt(i).array();

        if (order == ByteOrder.LITTLE_ENDIAN) {
            return Arrays.copyOfRange(bytes, 0, 2);

        } else if (order == ByteOrder.BIG_ENDIAN) {
            return Arrays.copyOfRange(bytes, 2, 4);

        } else {
            throw new IllegalArgumentException("ByteOrder must be BIG_ENDIAN or LITTLE_ENDIAN");

        }
    }

    /**
     * Convert an int to an unsigned byte.
     *
     * @param i     The int to be converted
     * @return      The int in unsigned byte form
     */
    public static byte intToByte(int i) {
        return (byte) (i & 0xFF);
    }

    /**
     * Convert an array of ints to an array of unsigned bytes. This is useful when you want to
     * construct a literal array of unsigned bytes with values greater than 127.
     * Only the lowest 8 bits of the int values are used.
     *
     * @param intArray  The array of ints to be converted
     * @return          The corresponding array of unsigned bytes
     */
    public static byte[] intArrayToByteArray(int[] intArray) {

        byte[] byteArray = new byte[intArray.length];

        for (int i = 0; i < intArray.length; i++) {
            byteArray[i] = intToByte(intArray[i]);
        }

        return byteArray;

    }

    /**
     * Convert an int to a four-byte array of its representation as an unsigned byte.
     *
     * @param i         The int to be converted
     * @param endian    The {@link java.nio.ByteOrder} endianness of the desired byte array
     * @return          The array of bytes representing the 32-bit unsigned integer
     */
    public static byte[] intToUInt32(int i, ByteOrder endian) {
        int truncated = (int) ( (long) i );
        return ByteBuffer.allocate(4).order(endian).putInt(truncated).array();
    }

}
