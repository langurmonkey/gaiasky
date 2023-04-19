/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.screenshot;

import gaiasky.util.Settings.ImageFormat;

public class BasicFileImageRenderer implements IFileImageRenderer {

    @Override
    public String saveScreenshot(String absoluteLocation, String baseFileName, int w, int h, boolean immediate, ImageFormat format, float quality) {
        return ImageRenderer.renderToImageGl20(absoluteLocation, baseFileName, w, h, format, quality);
    }

    @Override
    public void flush() {
        // Nothing to do
    }

}
