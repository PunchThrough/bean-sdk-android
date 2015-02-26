package com.punchthrough.bean.sdk.internal.utility;

import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class Misc {

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

    /**
     * Convert a string of ASCII hex characters (e.g. "DEADBEEF0042") to an array of bytes the hex
     * represents (e.g. [0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x42]). Treat bytes returned by this method
     * as unsigned.
     *
     * @param hex               The string of hex characters
     * @return                  An array of bytes the string represents
     * @throws DecoderException If the string passed in isn't made up of valid hex bytes
     */
    public static byte[] asciiHexToBytes(String hex) throws DecoderException {
        return Hex.decodeHex(hex.toCharArray());
    }

    // From http://stackoverflow.com/a/4768950/254187
    /**
     * Convert two unsigned bytes to one signed int.
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

    // Based on http://stackoverflow.com/a/16406386/254187

    /**
     * Retrieve the enum of a given type from a given raw value. Enums must implement the
     * {@link com.punchthrough.bean.sdk.internal.utility.RawValuable} interface to ensure they have
     * a {@link RawValuable#getRawValue()} method.
     *
     * @param enumClass The class of the enum type being parsed, e.g. <code>BeanState.class</code>
     * @param value     The raw int value of the enum to be retrieved
     * @param <T>       The enum type being parsed
     * @return          The enum value with the given raw value
     *
     * @throws NoEnumFoundException if the given enum type has no enum value with a raw value
     *      matching the given value
     */
    public static <T extends Enum & RawValuable> T enumWithRawValue(Class<T> enumClass, int value)
            throws NoEnumFoundException {

        for (Object oneEnumRaw : EnumSet.allOf(enumClass)) {
            T oneEnum = (T) oneEnumRaw;
            if (value == oneEnum.getRawValue()) {
                return oneEnum;
            }
        }
        throw new NoEnumFoundException(String.format(
                "No enum found for class %s with raw value %d", enumClass.getName(), value));

    }

    /**
     * Retrieve the enum of a given type from a given raw value. Enums must implement the
     * {@link com.punchthrough.bean.sdk.internal.utility.RawValuable} interface to ensure they have
     * a {@link RawValuable#getRawValue()} method.
     *
     * @param enumClass The class of the enum type being parsed, e.g. <code>BeanState.class</code>
     * @param value     The raw byte value of the enum to be retrieved
     * @param <T>       The enum type being parsed
     * @return          The enum value with the given raw value
     *
     * @throws NoEnumFoundException if the given enum type has no enum value with a raw value
     *      matching the given value
     */
    public static <T extends Enum & RawValuable> T enumWithRawValue(Class<T> enumClass, byte value)
            throws NoEnumFoundException {

        return enumWithRawValue(enumClass, (int) value);

    }

    /**
     * Retrieve a number of raw bytes at an offset.
     *
     * @param offset The byte at which to start, zero-indexed
     * @param length The number of bytes to return. If this is greater than the number of bytes
     *               available after <code>offset</code>, it will return all available bytes,
     *               truncated at the end.
     * @return       The bytes, starting at <code>offset</code> of length <code>length</code> or
     *               less if truncated
     */
    public static <T extends Chunkable> byte[] bytesFromChunkable(T chunkable, int offset, int length) {
        
        byte[] data = chunkable.getChunkableData();

        if ( offset + length > data.length ) {
            // Arrays.copyOfRange appends 0s when the array end is exceeded.
            // Trim length manually to avoid appending extra data.
            return Arrays.copyOfRange(data, offset, data.length);

        } else {
            return Arrays.copyOfRange(data, offset, offset + length);

        }
    }

    /**
     * Retrieve a chunk of raw bytes. Chunks are created by slicing the array at even intervals.
     * The final chunk may be shorter than the other chunks if it's been truncated.
     *
     * @param chunkLength   The length of each chunk
     * @param chunkNum      The chunk at which to start, zero-indexed
     * @return              The chunk (array of bytes)
     */
    public static <T extends Chunkable> byte[] chunkFromChunkable(
            T chunkable, int chunkLength, int chunkNum) {
        int start = chunkNum * chunkLength;
        return bytesFromChunkable(chunkable, start, chunkLength);
    }

    /**
     * Retrieve the count of chunks for a given chunk length.
     *
     * @param chunkLength   The length of each chunk
     * @return              The number of chunks generated for a given chunk length
     */
    public static <T extends Chunkable> int chunkCountFromChunkable(T chunkable, int chunkLength) {
        byte[] data = chunkable.getChunkableData();
        return (int) Math.ceil(data.length * 1.0 / chunkLength);
    }

    /**
     * Retrieve all chunks for a given chunk length.
     * The final chunk may be shorter than the other chunks if it's been truncated.
     *
     * @param chunkLength   The length of each chunk
     * @return              A list of chunks (byte arrays)
     */
    public static <T extends Chunkable> List<byte[]> chunksFromChunkable(T chunkable, int chunkLength) {

        List<byte[]> chunks = new ArrayList<>();

        int chunkCount = chunkCountFromChunkable(chunkable, chunkLength);
        for (int i = 0; i < chunkCount; i++) {
            byte[] chunk = chunkFromChunkable(chunkable, chunkLength, i);
            chunks.add(chunk);
        }

        return chunks;
    }

}
