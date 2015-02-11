package com.punchthrough.bean.sdk.internal.utility;

public class Misc {

    public static int clamp(int n, int min, int max) {
        if (n < min) return min;
        if (n > max) return max;
        return n;
    }

    public static int clampToUInt8(int n) {
        return clamp(n, 0, 255);
    }

}
