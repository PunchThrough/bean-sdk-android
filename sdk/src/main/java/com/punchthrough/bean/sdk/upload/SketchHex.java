package com.punchthrough.bean.sdk.upload;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.exception.HexParsingException;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.internal.intelhex.Line;
import com.punchthrough.bean.sdk.internal.intelhex.LineRecordType;
import com.punchthrough.bean.sdk.internal.utility.Chunk;
import com.punchthrough.bean.sdk.internal.utility.Constants;
import com.punchthrough.bean.sdk.internal.utility.EnumParse;

import org.apache.commons.codec.DecoderException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import auto.parcel.AutoParcel;

import static com.punchthrough.bean.sdk.internal.utility.Convert.asciiHexToBytes;
import static com.punchthrough.bean.sdk.internal.utility.Convert.bytesToInt;

/**
 * Represents a Sketch (Arduino code snippet) in hex form.
 */
@AutoParcel
public abstract class SketchHex implements Parcelable, Chunk.Chunkable {

    public abstract String sketchName();

    public abstract byte[] bytes();

    @Override
    public byte[] getChunkableData() {
        return bytes();
    }

    /**
     * Initialize a SketchHex object with no data.
     */
    public static SketchHex create(String sketchName) throws HexParsingException {
        return SketchHex.create(sketchName, "");
    }

    /**
     * Initialize a SketchHex object with a string of Intel Hex data.
     *
     * @param sketchName The name of the sketch.
     * @param hexString The Intel Hex data as a string
     * @return The new SketchHex object
     * @throws com.punchthrough.bean.sdk.internal.exception.HexParsingException
     *         if the string data being parsed is not valid Intel Hex
     */
    public static SketchHex create(String sketchName, String hexString) throws HexParsingException {

        if (sketchName.length() > Constants.MAX_SKETCH_NAME_LENGTH) {
            sketchName = sketchName.substring(0, Constants.MAX_SKETCH_NAME_LENGTH);
        }

        List<Line> lines = parseHexStringToLines(hexString);
        byte[] bytes = convertLinesToBytes(lines);
        return new AutoParcel_SketchHex(sketchName, bytes);
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
                recordType = EnumParse.enumWithRawValue(LineRecordType.class, rawRecordType);
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
