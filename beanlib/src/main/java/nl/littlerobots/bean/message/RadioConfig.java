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

package nl.littlerobots.bean.message;

import android.os.Parcelable;
import android.text.TextUtils;

import java.nio.charset.Charset;

import auto.parcel.AutoParcel;
import okio.Buffer;

@AutoParcel
public abstract class RadioConfig implements Parcelable, Message {

    public static RadioConfig create(String name, int advertisementInterval, int connectionInterval, TX_POWER power) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        if (name.length() > 20) {
            throw new IllegalArgumentException("Name must be max 20 characters");
        }
        return new AutoParcel_RadioConfig(name, advertisementInterval, connectionInterval, power);
    }

    public static RadioConfig fromPayload(Buffer buffer) {
        int advertisementInterval = buffer.readShortLe() & 0xffff;
        int connectionInterval = buffer.readShortLe() & 0xffff;
        int power = buffer.readByte() & 0xff;
        String localName = buffer.readString(buffer.size() - 1, Charset.forName("UTF-8"));
        int size = buffer.readByte() & 0xff;
        localName = localName.substring(0, size);
        return new AutoParcel_RadioConfig(localName, advertisementInterval, connectionInterval, TX_POWER.values()[power]);
    }

    public abstract String name();

    public abstract int advertisementInterval();

    public abstract int connectionInterval();

    public abstract TX_POWER power();

    @Override
    public byte[] toPayload() {
        Buffer buffer = new Buffer();
        buffer.writeShortLe(advertisementInterval() & 0xffff);
        buffer.writeShortLe(connectionInterval() & 0xffff);
        buffer.writeByte(power().ordinal() & 0xff);
        StringBuilder sb = new StringBuilder(name());
        sb.setLength(20);
        buffer.writeString(sb.toString(), Charset.forName("UTF-8"));
        buffer.writeByte(name().length() & 0xff);
        return buffer.readByteArray();
    }

    public enum TX_POWER {TX_4DB, TX_0DB, TX_NEG6DB, TX_NEG23DB}
}
