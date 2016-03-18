package com.punchthrough.bean.sdk.upload;

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

    public String version() {
        return images.get(0).version();
    }

    public FirmwareImage getNextImage() {
        FirmwareImage i = images.get(imageCounter);

        if (imageCounter >= images.size() - 1) {
            imageCounter = 0;
        } else {
            imageCounter++;
        }

        return i;
    }

}
