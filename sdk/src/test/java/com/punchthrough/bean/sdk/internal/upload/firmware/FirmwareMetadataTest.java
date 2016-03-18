package com.punchthrough.bean.sdk.internal.upload.firmware;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirmwareMetadataTest {

    @Test
    public void testCreateMetadataFromInts() {

        FirmwareMetadata metadata = new FirmwareMetadata(42, 43, new byte[] {44, 45, 46, 47});
        assertThat(metadata.version()).isEqualTo(42);
        assertThat(metadata.length()).isEqualTo(43);
        assertThat(metadata.uniqueID()).isEqualTo(new byte[] {44, 45, 46, 47});

    }

}
