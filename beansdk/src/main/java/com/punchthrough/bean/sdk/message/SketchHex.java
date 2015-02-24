package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.exception.HexParsingException;
import com.punchthrough.bean.sdk.internal.exception.NameLengthException;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.internal.intelhex.Line;
import com.punchthrough.bean.sdk.internal.intelhex.LineRecordType;
import com.punchthrough.bean.sdk.internal.utility.Constants;

import org.apache.commons.codec.DecoderException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import auto.parcel.AutoParcel;

import static com.punchthrough.bean.sdk.internal.utility.Misc.asciiHexToBytes;
import static com.punchthrough.bean.sdk.internal.utility.Misc.bytesToInt;
import static com.punchthrough.bean.sdk.internal.utility.Misc.enumWithRawValue;

@AutoParcel
public abstract class SketchHex implements Parcelable {

    public abstract String sketchName();

    public abstract byte[] bytes();

    /**
     * Initialize a SketchHex object with no data.
     */
    public static SketchHex create(String sketchName)
            throws NameLengthException, HexParsingException {
        return SketchHex.create(sketchName, "");
    }

    /**
     * Initialize a SketchHex object with a string of Intel Hex data.
     * @param sketchName The name of the sketch.
     * @param hexString The Intel Hex data as a string
     */
    public static SketchHex create(String sketchName, String hexString)
            throws HexParsingException, NameLengthException {

        if (sketchName.length() > Constants.MAX_SKETCH_NAME_LENGTH) {
            throw new NameLengthException(String.format(
                    "Sketch name must be less than %d characters",
                    Constants.MAX_SKETCH_NAME_LENGTH));
        }

        List<Line> lines = parseHexStringToLines(hexString);
        byte[] bytes = convertLinesToBytes(lines);
        return new AutoParcel_SketchHex(sketchName, bytes);
    }

    /**
     * Retrieve the raw bytes represented by the parsed Intel Hex.
     *
     * @param offset The byte at which to start, zero-indexed
     * @param length The number of bytes to return. If this is greater than the number of bytes
     *               available after <code>offset</code>, it will return all available bytes,
     *               truncated at the end.
     * @return       The bytes, starting at <code>offset</code> of length <code>length</code> or
     *               less if truncated
     */
    public byte[] bytes(int offset, int length) {

        if ( offset + length > bytes().length ) {
            // Arrays.copyOfRange appends 0s when the array end is exceeded.
            // Trim length manually to avoid appending extra data.
            return Arrays.copyOfRange(bytes(), offset, bytes().length);

        } else {
            return Arrays.copyOfRange(bytes(), offset, offset + length);

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
    public byte[] chunk(int chunkLength, int chunkNum) {
        int start = chunkNum * chunkLength;
        return bytes(start, chunkLength);
    }

    /**
     * Retrieve the count of chunks for a given chunk length.
     *
     * @param chunkLength   The length of each chunk
     * @return              The number of chunks generated for a given chunk length
     */
    public int chunkCount(int chunkLength) {
        return (int) Math.ceil(bytes().length * 1.0 / chunkLength);
    }

    /**
     * Retrieve all chunks for a given chunk length.
     * The final chunk may be shorter than the other chunks if it's been truncated.
     *
     * @param chunkLength   The length of each chunk
     * @return              A list of chunks (byte arrays)
     */
    public List<byte[]> chunks(int chunkLength) {

        List<byte[]> chunks = new ArrayList<>();

        for (int i = 0; i < chunkCount(chunkLength); i++) {
            chunks.add(chunk(chunkLength, i));
        }

        return chunks;
    }

    /**
     * Parse a string of Intel Hex data to a list of
     * {@link com.punchthrough.bean.sdk.internal.intelhex.Line} objects.
     *
     * @param hexString The Intel Hex data as a string
     * @return          A list of {@link com.punchthrough.bean.sdk.internal.intelhex.Line} objects
     *                  representing the Intel Hex data
     * @throws HexParsingException  If the Intel Hex could not be parsed
     */
    private static List<Line> parseHexStringToLines(String hexString) throws HexParsingException {
        List<String> rawLines = Arrays.asList(hexString.split("\n"));
        ListIterator<String> iterator = rawLines.listIterator();

        List<Line> lines = new ArrayList<>();
        while (iterator.hasNext()) {

            int rawLineNum = iterator.nextIndex();
            String rawLine = iterator.next();

            if (rawLine.length() < 11) continue;

            if ( ! rawLine.startsWith(":") ) {
                throw new HexParsingException(String.format(
                        "Couldn't parse hex: line %d did not start with ':'", rawLineNum));
            }

            rawLine = rawLine.replaceAll("\r", "");

            // rawBytes: bytes of the string without the leading colon, parsed into hex
            byte[] rawBytes;
            try {
                rawBytes = asciiHexToBytes(rawLine.substring(1));
            } catch (DecoderException e) {
                throw new HexParsingException(String.format(
                        "Couldn't parse hex: line %d ASCII could not be parsed to byte array: %s",
                        rawLineNum, e.getLocalizedMessage()));
            }

            Line line = new Line();
            line.setByteCount(rawBytes[0]);
            line.setAddress(bytesToInt(rawBytes[1], rawBytes[2]));

            byte rawRecordType = rawBytes[3];
            LineRecordType recordType;
            try {
                recordType = enumWithRawValue(LineRecordType.class, rawRecordType);
            } catch (NoEnumFoundException e) {
                throw new HexParsingException(String.format(
                        "Couldn't parse hex: line %d had invalid record type %d",
                        rawLineNum, rawRecordType));
            }

            line.setRecordType(recordType);

            if (line.getByteCount() > 0) {
                line.setData(Arrays.copyOfRange(rawBytes, 4, 4 + line.getByteCount()));
            }

            line.setChecksum(rawBytes[4 + line.getByteCount()]);

            lines.add(line);
        }

        return lines;
    }

    /**
     * Convert a list of Intel Hex {@link com.punchthrough.bean.sdk.internal.intelhex.Line} objects
     * to an array of the raw bytes they represent.
     *
     * @param lines The List of {@link com.punchthrough.bean.sdk.internal.intelhex.Line} objects
     * @return      An array of raw bytes represented by the
     *              {@link com.punchthrough.bean.sdk.internal.intelhex.Line} objects
     */
    private static byte[] convertLinesToBytes(List<Line> lines) {
        List<Line> dataLines = new ArrayList<>();
        int byteCount = 0;
        for (Line line : lines) {
            if (line.getRecordType() == LineRecordType.DATA) {
                dataLines.add(line);
                byteCount += line.getByteCount();
            }
        }

        byte[] bytes = new byte[byteCount];
        int i = 0;
        for (Line line : dataLines) {
            for (byte b : line.getData()) {
                bytes[i] = b;
                i++;
            }
        }

        return bytes;
    }

}
