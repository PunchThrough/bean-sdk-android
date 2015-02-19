package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.exception.HexParsingException;
import com.punchthrough.bean.sdk.internal.exception.NameLengthException;
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

    public byte[] bytes(int offset, int length) {

        if ( offset + length > bytes().length ) {
            // Arrays.copyOfRange appends 0s when the array end is exceeded.
            // Trim length manually to avoid appending extra data.
            return Arrays.copyOfRange(bytes(), offset, bytes().length);

        } else {
            return Arrays.copyOfRange(bytes(), offset, offset + length);

        }
    }

    public byte[] chunk(int chunkLength, int chunkNum) {
        int start = chunkNum * chunkLength;
        return bytes(start, chunkLength);
    }

    public int chunkCount(int chunkLength) {
        return (int) Math.ceil(bytes().length * 1.0 / chunkLength);
    }

    public List<byte[]> chunks(int chunkLength) {

        List<byte[]> chunks = new ArrayList<>();

        for (int i = 0; i < chunkCount(chunkLength); i++) {
            chunks.add(chunk(chunkLength, i));
        }

        return chunks;
    }

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
            if (rawRecordType == 0) {
                recordType = LineRecordType.DATA;
            } else if (rawRecordType == 1) {
                recordType = LineRecordType.END_OF_FILE;
            } else if (rawRecordType == 2) {
                recordType = LineRecordType.EXTENDED_SEGMENT_ADDRESS;
            } else if (rawRecordType == 3) {
                recordType = LineRecordType.START_SEGMENT_ADDRESS;
            } else if (rawRecordType == 4) {
                recordType = LineRecordType.EXTENDED_LINEAR_ADDRESS;
            } else if (rawRecordType == 5) {
                recordType = LineRecordType.START_LINEAR_ADDRESS;
            } else {
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
