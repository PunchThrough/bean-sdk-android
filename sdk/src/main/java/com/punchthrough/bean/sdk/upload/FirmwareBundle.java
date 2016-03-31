package com.punchthrough.bean.sdk.upload;

import com.punchthrough.bean.sdk.internal.exception.OADException;

import java.util.List;


/**
 * Represents a bundle of firmware images
 */
public class FirmwareBundle {

    private List<FirmwareImage> images;
    private int imageCounter;


    public FirmwareBundle(List<FirmwareImage> images) {
        this.images = images;
        this.imageCounter = 0;
    }

    public long version() {
        return images.get(0).version();
    }

    public FirmwareImage getNextImage() throws OADException {
        if (imageCounter >= images.size() ) {
            throw new OADException("Firmware bundle is exhausted, all images rejected");
        }

        FirmwareImage i = images.get(imageCounter);
        imageCounter++;
        return i;
    }

    public void reset() {
        imageCounter = 0;
    }

}
