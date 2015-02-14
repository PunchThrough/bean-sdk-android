package com.punchthrough.bean.sdk.internal.utility;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class Misc {

    public static int clamp(int n, int min, int max) {
        if (n < min) return min;
        if (n > max) return max;
        return n;
    }

    public static int clampToUInt8(int n) {
        return clamp(n, 0, 255);
    }

    public static byte[] asciiHexToBytes(String hex) throws DecoderException {
        return Hex.decodeHex(hex.toCharArray());
    }

    // From http://stackoverflow.com/a/4768950/254187
    public static int bytesToInt(byte high, byte low) {
        return ( (high & 0xFF) << 8 ) | ( low & 0xFF );
    }

}
