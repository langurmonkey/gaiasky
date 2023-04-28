/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Contains utilities to convert monochrome images to RGB.
 */
public class ImageUtils {

    public static boolean isMonochrome(final BufferedImage image) {
        return image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY;
    }

    /**
     * Converts the image in the incoming file to RGB if it is a monochrome image
     *
     * @param file The image to convert
     *
     * @return A boolean indicating whether the conversion was carried out (i.e. the image was actually monochrome)
     */
    public static boolean monochromeToRGB(final File file) throws IOException {
        final BufferedImage monochrome = ImageIO.read(file);
        if (isMonochrome(monochrome)) {
            BufferedImage rgb = monochromeToRGB(monochrome);

            String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            ImageIO.write(rgb, extension, file);
            return true;
        } else {
            return false;
        }
    }

    private static BufferedImage monochromeToRGB(BufferedImage monochrome) {
        int width = monochrome.getWidth();
        int height = monochrome.getHeight();

        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color grayColor = new Color(monochrome.getRGB(x, y));
                rgbImage.setRGB(x, y, grayColor.getRGB());
            }
        }
        return rgbImage;
    }

}
