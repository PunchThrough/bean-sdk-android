/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Little Robots
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.upload.SketchHex;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.zip.CRC32;

import auto.parcel.AutoParcel;
import okio.Buffer;
import okio.ByteString;

import static com.punchthrough.bean.sdk.internal.utility.Constants.MAX_SKETCH_NAME_LENGTH;
import static com.punchthrough.bean.sdk.internal.utility.Convert.intToByte;
import static com.punchthrough.bean.sdk.internal.utility.Convert.intToUInt32;

/**
 * Represents sketch metadata. This data is sent when programming a Bean so that the Bean knows the
 * name, size, and programming time of the sketch being sent.
 */
@AutoParcel
public abstract class SketchMetadata implements Parcelable {

    public abstract int hexSize();

    public abstract int hexCrc();

    public abstract Date timestamp();

    public abstract String hexName();

    public static SketchMetadata create(int hexSize, int hexCrc, Date timestamp, String hexName) {
        return new AutoParcel_SketchMetadata(hexSize, hexCrc, timestamp, hexName);
    }

    /**
     * Create a SketchMetadata object with the given
     * {@link com.punchthrough.bean.sdk.upload.SketchHex} data and programmed-at timestamp.
     * @param hex       The {@link com.punchthrough.bean.sdk.upload.SketchHex} to be sent
     * @param timestamp The time the bean will indicate it was programmed
     * @return          The SketchMetadata object
     */
    public static SketchMetadata create(SketchHex hex, Date timestamp) {
        int hexSize = hex.bytes().length;
        String hexName = hex.sketchName();

        CRC32 crc = new CRC32();
        crc.update(hex.bytes());
        int hexCrc = (int) crc.getValue();

        return SketchMetadata.create(hexSize, hexCrc, timestamp, hexName);
    }

    /**
     * Get this object's metadata as a payload of bytes, ready to send to Bean in a
     * {@link com.punchthrough.bean.sdk.internal.BeanMessageID#BL_CMD_START} message.
     * @return The SketchMetadata in payload form
     */
    public Buffer toPayload() {
        /* From AppMessages.h: BL_SKETCH_META_DATA_T struct
         *
         * {
         *   PTD_UINT32 hexSize;
         *   PTD_UINT32 hexCrc;
         *   PTD_UINT32 timestamp;
         *   PTD_UINT8 hexNameSize;
         *   PTD_UINT8 hexName[MAX_SKETCH_NAME_SIZE];
         * }
         */
        Buffer buffer = new Buffer();

        // Pad name to 20 bytes to fill buffer completely. It will be truncated by the Bean.
        // Name has already been truncated to max length by the SketchHex constructor.
        String fullName = hexName();
        while (fullName.length() < MAX_SKETCH_NAME_LENGTH) {
            fullName += " ";
        }

        // !! The Bean uses little endian. Don't screw this up like I did!
        ByteOrder endian = ByteOrder.LITTLE_ENDIAN;

        byte[] hexSize = intToUInt32(hexSize(), endian);
        byte[] hexCrc = intToUInt32(hexCrc(), endian);
        byte[] timestamp = intToUInt32( (int) (new Date().getTime() / 1000), endian);
        byte nameLength = intToByte(hexName().length());
        ByteString name = ByteString.encodeUtf8(fullName);

        buffer.write(hexSize);
        buffer.write(hexCrc);
        buffer.write(timestamp);
        buffer.writeByte(nameLength);
        buffer.write(name);

        return buffer;
    }

    public static SketchMetadata fromPayload(Buffer buffer) {
        int hexSize = buffer.readIntLe();
        int hexCrc = buffer.readIntLe();
        long timestamp = (buffer.readIntLe() & 0xffffffffL) * 1000L;
        int hexNameSize = buffer.readByte() & 0xff;
        String hexName = "";
        if (hexNameSize > 0 && hexNameSize <= 20) {
            hexName = buffer.readString(hexNameSize, Charset.forName("UTF-8"));
        }
        return new AutoParcel_SketchMetadata(hexSize, hexCrc, new Date(timestamp), hexName);
    }

}
