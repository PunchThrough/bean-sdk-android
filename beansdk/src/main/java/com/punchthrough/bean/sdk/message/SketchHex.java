package com.punchthrough.bean.sdk.message;

import android.os.Parcel;
import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.exception.HexParsingException;
import com.punchthrough.bean.sdk.internal.exception.NameLengthException;
import com.punchthrough.bean.sdk.internal.intelhex.Line;
import com.punchthrough.bean.sdk.internal.intelhex.LineRecordType;

import org.apache.commons.codec.DecoderException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import static com.punchthrough.bean.sdk.internal.utility.Misc.asciiHexToBytes;
import static com.punchthrough.bean.sdk.internal.utility.Misc.bytesToInt;

public class SketchHex implements Parcelable {

    private static final int MAX_SKETCH_NAME_LENGTH = 20;

    private String sketchName;
    private List<Line> lines = new ArrayList<>();

    /**
     * Initialize a SketchHex object with a string of Intel Hex data.
     * @param hexString The Intel Hex data as a string
     */
    public SketchHex(String hexString) throws HexParsingException {
        parseHexString(hexString);
    }

    /**
     * Retrieve the name of the sketch.
     * @return The name of the sketch
     */
    public String getSketchName() {
        return sketchName;
    }

    /**
     * Set the name of this sketch for programming to a Bean.
     * @param sketchName The new name of the sketch
     */
    public void setSketchName(String sketchName) throws NameLengthException {
        if (sketchName.length() > MAX_SKETCH_NAME_LENGTH) {
            throw new NameLengthException(String.format(
                    "Sketch name must be %d characters or less", MAX_SKETCH_NAME_LENGTH));
        }
        this.sketchName = sketchName;
    }

    public byte[] getBytes() {
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
    }

    protected SketchHex(Parcel in) {
        sketchName = in.readString();
        if (in.readByte() == 0x01) {
            lines = new ArrayList<Line>();
            in.readList(lines, Line.class.getClassLoader());
        } else {
            lines = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sketchName);
        if (lines == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(lines);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<SketchHex> CREATOR = new Parcelable.Creator<SketchHex>() {
        @Override
        public SketchHex createFromParcel(Parcel in) {
            return new SketchHex(in);
        }

        @Override
        public SketchHex[] newArray(int size) {
            return new SketchHex[size];
        }
    };
}
