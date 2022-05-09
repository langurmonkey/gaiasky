/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.screenshot;

import gaiasky.util.Settings.ImageFormat;

public interface IFileImageRenderer {

    /**
     * Renders a screenshot in the given location with the given prefix and the
     * given size.
     *
     * @param folder     Folder to save.
     * @param fileprefix The file name prefix.
     * @param w          The width.
     * @param h          The height.
     * @param immediate  Forces synchronous immediate write to disk.
     * @param type       The image type, JPG or PNG
     * @param quality    The quality in the case of JPG in [0..1]
     * @return String with the path to the screenshot image file
     */
    String saveScreenshot(String folder, String fileprefix, int w, int h, boolean immediate, ImageFormat type, float quality);

    /**
     * Flushes the renderer causing the images to be written, if needed.
     */
    void flush();

}
