package gaia.cu9.ari.gaiaorbit.screenshot;

import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ImageFormat;

public interface IFileImageRenderer {

    /**
     * Renders a screenshot in the given location with the given prefix and the
     * given size.
     * 
     * @param folder
     *            Folder to save.
     * @param fileprefix
     *            The file name prefix.
     * @param w
     *            The width.
     * @param h
     *            The height.
     * @param immediate
     *            Forces synchronous immediate write to disk.
     * @param type
     *            The image type, JPG or PNG
     * @param quality
     *            The quality in the case of JPG in [0..1]
     * @return String with the path to the screenshot image file
     */
    public String saveScreenshot(String folder, String fileprefix, int w, int h, boolean immediate, ImageFormat type, float quality);

    /**
     * Flushes the renderer causing the images to be written, if needed.
     */
    public void flush();

}
