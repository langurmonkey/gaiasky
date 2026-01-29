/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;

public class PackUITextures {
    public static void main(String[] args) {
        TexturePacker.Settings x2settings = new TexturePacker.Settings();
        x2settings.scale[0] = 1.5f;
        x2settings.jpegQuality = 0.95f;
        x2settings.paddingX = 2;
        x2settings.paddingY = 2;
        x2settings.filterMag = Texture.TextureFilter.Linear;
        x2settings.filterMin = Texture.TextureFilter.Linear;

        final Color purple = Color.decode("#ff00ff");
        final String themeName = "default";

        // Use current path variable
        String gs = (new java.io.File("")).getAbsolutePath();

            try {

                // Process
                TexturePacker.process(x2settings, gs + "/assets/skins/raw/source/", gs + String.format("/assets/skins/%s/", themeName), themeName);

                // Process and copy theme JSON file.
                File templateFile = new File(gs + "/assets/skins/raw/source.json");
                File outputDir = new File(gs + String.format("/assets/skins/%s/", themeName));
                if (!outputDir.exists()) {
                    if (outputDir.mkdirs()) {
                        System.out.println("Created output directory");
                    }
                }
                File outputFile = new File(outputDir, themeName + ".json");

                String jsonTemplate = Files.readString(templateFile.toPath());
                String result = replaceColors(jsonTemplate, purple);

                Files.writeString(outputFile.toPath(), result);
                System.out.println("Written to: " + outputFile.getAbsolutePath());


            } catch (Exception e) {
                System.out.println(String.format("Error generating theme: %s", themeName));
                throw new RuntimeException(e);
            }

    }

    private static String replaceColors(String template, Color theme) {
        return template
                .replace("\"%theme_r%\"", format(theme.getRed()))
                .replace("\"%theme_g%\"", format(theme.getGreen()))
                .replace("\"%theme_b%\"", format(theme.getBlue()));
    }

    private static String format(int value) {
        return String.format("%.4f", value / 255.0);
    }

}
