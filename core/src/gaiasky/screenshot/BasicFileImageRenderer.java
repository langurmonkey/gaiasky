/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.screenshot;

import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ImageFormat;

/**
 * Renders image files synchronously.
 * 
 * @author Toni Sagrista
 *
 */
public class BasicFileImageRenderer implements IFileImageRenderer {

    @Override
    public String saveScreenshot(String absoluteLocation, String baseFileName, int w, int h, boolean immediate, ImageFormat type, float quality) {
        return ImageRenderer.renderToImageGl20(absoluteLocation, baseFileName, w, h, type, quality);
    }

    @Override
    public void flush() {
        // Nothing to do
    }

}
