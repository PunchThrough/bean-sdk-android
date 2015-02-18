package com.punchthrough.bean.sdk.utility;

import android.test.AndroidTestCase;

import static com.punchthrough.bean.sdk.internal.utility.Misc.intToByte;
import static org.assertj.core.api.Assertions.assertThat;

public class MiscTest extends AndroidTestCase {

    public void testIntToByte() {
        assertThat(intToByte(0xFF)).isEqualTo((byte) 0xFF);
    }

}
