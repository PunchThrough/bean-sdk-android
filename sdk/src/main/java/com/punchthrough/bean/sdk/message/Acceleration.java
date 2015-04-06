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

import auto.parcel.AutoParcel;
import okio.Buffer;

/**
 * Represents an acceleration reading from the Bean, returning values in Gs.
 */
@AutoParcel
public abstract class Acceleration implements Parcelable {
    public static Acceleration fromPayload(Buffer buffer) {
        int x = buffer.readShortLe();
        int y = buffer.readShortLe();
        int z = buffer.readShortLe();
        int sensitivity = buffer.readByte() & 0xff;
        double lsbGConversionFactor = sensitivity / 512.0;
        return new AutoParcel_Acceleration(x * lsbGConversionFactor, y * lsbGConversionFactor, z * lsbGConversionFactor);
    }

    public abstract double x();

    public abstract double y();

    public abstract double z();
}
