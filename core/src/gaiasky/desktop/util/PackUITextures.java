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
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.scale[0] = 1.5f;
        settings.jpegQuality = 0.95f;
        settings.filterMag = Texture.TextureFilter.Linear;
        settings.filterMin = Texture.TextureFilter.Linear;

        // Use current path variable
        String gs = (new java.io.File("")).getAbsolutePath();

        try {
            // Process
            TexturePacker.process(settings, gs + "/assets/skins/raw/source/", gs + String.format("/assets/skins/%s/", "default"), "default");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
