package com.punchthrough.bean.sdk.message;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.internal.exception.HexParsingException;
import com.punchthrough.bean.sdk.internal.exception.NameLengthException;

import static com.punchthrough.bean.sdk.internal.utility.Misc.intToByte;
import static org.assertj.core.api.Assertions.*;

public class SketchHexTest extends AndroidTestCase {

    public void testHexParsingAndSketchNaming() throws HexParsingException, NameLengthException {

        // From http://en.wikipedia.org/wiki/Intel_HEX
        String asciiHexData =
                //        Bytes 1-8       Bytes 9-16
                //        |...............|...............
                ":10010000214601360121470136007EFE09D2190140\n" +
                ":100110002146017E17C20001FF5F16002148011928\n" +
                ":10012000194E79234623965778239EDA3F01B2CAA7\n" +
                ":100130003F0156702B5E712B722B732146013421C7\n" +
                ":00000001FF";

        // Produced from the above hex by http://hex2bin.sourceforge.net/
        int[] rawHexDataInts = new int[]{
                0x21, 0x46, 0x01, 0x36, 0x01, 0x21, 0x47, 0x01,  //  1-16
                0x36, 0x00, 0x7E, 0xFE, 0x09, 0xD2, 0x19, 0x01,
                0x21, 0x46, 0x01, 0x7E, 0x17, 0xC2, 0x00, 0x01,  // 17-32
                0xFF, 0x5F, 0x16, 0x00, 0x21, 0x48, 0x01, 0x19,
                0x19, 0x4E, 0x79, 0x23, 0x46, 0x23, 0x96, 0x57,  // 33-48
                0x78, 0x23, 0x9E, 0xDA, 0x3F, 0x01, 0xB2, 0xCA,
                0x3F, 0x01, 0x56, 0x70, 0x2B, 0x5E, 0x71, 0x2B,  // 49-64
                0x72, 0x2B, 0x73, 0x21, 0x46, 0x01, 0x34, 0x21,
        };

        byte[] rawHexData = new byte[rawHexDataInts.length];
        int i = 0;
        for (int n : rawHexDataInts) {
            rawHexData[i] = intToByte(n);
            i++;
        }

        SketchHex sketchHex = new SketchHex(asciiHexData);

        sketchHex.setSketchName("StairCar");
        assertThat(sketchHex.getSketchName()).isEqualTo("StairCar");

        try {
            sketchHex.setSketchName("LongerThan20CharactersByQuiteABit_TheSequel_TurboEdition");
            fail();
        } catch (NameLengthException e) {
            assertThat(e).hasMessage("Sketch name must be 20 characters or less");
        }

        assertThat(sketchHex.getBytes()).isEqualTo(rawHexData);
    }

}
