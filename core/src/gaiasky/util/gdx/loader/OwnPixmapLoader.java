/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.PixmapLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class OwnPixmapLoader extends PixmapLoader {
    private static final Log logger = Logger.getLogger(OwnPixmapLoader.class);

    Pixmap pixmap;

    public OwnPixmapLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, PixmapParameter parameter) {
        if (file.extension().equalsIgnoreCase("jxl")) {
            try {
                BufferedImage image = ImageIO.read(file.read());

                int w = image.getWidth();
                int h = image.getHeight();
                pixmap = new Pixmap(w, h, Format.RGBA8888);

                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        int argb = image.getRGB(x, y);
                        int blue =  argb & 255;
                        int green = (argb >> 8) & 255;
                        int red =   (argb >> 16) & 255;
                        int alpha = (argb >> 24) & 255;
                        pixmap.setColor(red / 255f, green / 255f, blue/255f, alpha / 255f);
                        pixmap.drawPixel(x, y);
                    }
                }
            } catch (IOException e) {
                logger.error(e, "Error loading JPEG-XL image.");
            }
        } else {
            // Default to regular loader.
            super.loadAsync(manager, fileName, file, parameter);
        }
    }

    @Override
    public Pixmap loadSync(AssetManager manager, String fileName, FileHandle file, PixmapParameter parameter) {
        if (this.pixmap != null) {
            Pixmap pixmap = this.pixmap;
            this.pixmap = null;
            return pixmap;
        } else {
            return super.loadSync(manager, fileName, file, parameter);
        }
    }
}
