package com.punchthrough.bean.sdk.message;

import com.punchthrough.bean.sdk.internal.exception.HexParsingException;
import com.punchthrough.bean.sdk.internal.intelhex.Line;
import com.punchthrough.bean.sdk.internal.intelhex.LineRecordType;

import org.apache.commons.codec.DecoderException;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import static com.punchthrough.bean.sdk.internal.utility.Misc.asciiHexToBytes;
import static com.punchthrough.bean.sdk.internal.utility.Misc.bytesToInt;

public class SketchHex {

    /**
     * Initialize a SketchHex object with a string of Intel Hex data.
     * @param hexString The Intel Hex data as a string
     */
    public SketchHex(String hexString) throws HexParsingException {
        parseHexString(hexString);
    }

    private String sketchName;

    public String getSketchName() {
        return sketchName;
    }

    public void setSketchName(String sketchName) {
        this.sketchName = sketchName;
    }

    private void parseHexString(String hexString) throws HexParsingException {
        List<String> rawLines = Arrays.asList(hexString.split("\n"));
        ListIterator<String> iterator = rawLines.listIterator();
        while (iterator.hasNext()) {

            int rawLineNum = iterator.nextIndex();
            String rawLine = iterator.next();

            if (rawLine.length() < 11) continue;

            if ( ! rawLine.startsWith(":") ) {
                throw new HexParsingException(String.format(
                        "Couldn't parse hex: line %d did not start with ':'", rawLineNum));
            }

            byte[] rawBytes;
            try {
                rawBytes = asciiHexToBytes(rawLine);
            } catch (DecoderException e) {
                throw new HexParsingException(String.format(
                        "Couldn't parse hex: line %d ASCII could not be parsed to byte array: %s",
                        rawLineNum, e.getLocalizedMessage()));
            }

            Line line = new Line();
            line.setByteCount(rawBytes[1]);
            line.setAddress(bytesToInt(rawBytes[2], rawBytes[3]));

            byte rawRecordType = rawBytes[4];
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
                line.setData(Arrays.copyOfRange(rawBytes, 5, 5 + line.getByteCount()));
            }

            line.setChecksum(rawBytes[5 + line.getByteCount()]);

            if (line.getData().length != line.getByteCount()) {
                throw new HexParsingException(String.format(
                        "Couldn't parse hex: line %d had data length %d " +
                                "but indicated byte count %d",
                        rawLineNum, line.getData().length, line.getByteCount()));
            }

        }
    }
}
