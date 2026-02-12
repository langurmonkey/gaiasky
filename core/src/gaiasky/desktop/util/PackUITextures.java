/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

/**
 * Generates the <code>default.atlas</code> and <code>default.png</code> files from the UI skin images
 * at <code>assets/skins/raw/source</code>.
 */
public class PackUITextures {
    public static void main(String[] args) {
        TexturePacker.Settings x2settings = new TexturePacker.Settings();
        x2settings.scale[0] = 1.5f;
        x2settings.jpegQuality = 0.95f;
        x2settings.paddingX = 2;
        x2settings.paddingY = 2;
        x2settings.filterMag = Texture.TextureFilter.Linear;
        x2settings.filterMin = Texture.TextureFilter.Linear;

        // Use current path variable
        String gs = (new java.io.File("")).getAbsolutePath();

        try {
            // Process
            TexturePacker.process(x2settings, gs + "/assets/skins/raw/source/", gs + String.format("/assets/skins/%s/", "default"), "default");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
