/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import org.apache.commons.imaging.*;
import org.apache.commons.imaging.ImageInfo.ColorType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ImageUtils {

    public static boolean isMonochrome(final File file) throws IOException, ImageReadException {
        final ImageInfo imageInfo = Imaging.getImageInfo(file);
        return imageInfo.getColorType() == ColorType.GRAYSCALE;
    }

    /**
     * Converts the image in the incoming file to RGB if it is a monochrome image
     *
     * @param file The image to convert
     *
     * @return A boolean indicating whether the conversion was carried out (i.e. the image was actually monochrome)
     */
    public static boolean monochromeToRGB(final File file) throws IOException, ImageReadException, ImageWriteException {
        final ImageInfo imageInfo = Imaging.getImageInfo(file);
        if (imageInfo.getColorType() == ColorType.GRAYSCALE) {

            final BufferedImage monochrome = ImageIO.read(file);
            BufferedImage rgb = monochromeToRGB(monochrome);

            String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            ImageFormat imageFormat;
            try {
                imageFormat = ImageFormats.valueOf(extension.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                // Default
                imageFormat = ImageFormats.PNG;
            }

            Imaging.writeImage(rgb, file, imageFormat);
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
