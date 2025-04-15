/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

public class PackUITextures {
    public static void main(String[] args) {
        TexturePacker.Settings x1settings = new TexturePacker.Settings();
        x1settings.scale[0] = 1f;
        x1settings.jpegQuality = 0.8f;
        x1settings.paddingX = 2;
        x1settings.paddingY = 2;
        TexturePacker.Settings x2settings = new TexturePacker.Settings();
        x2settings.scale[0] = 1.55f;
        x2settings.jpegQuality = 0.8f;
        x2settings.paddingX = 1;
        x2settings.paddingY = 1;
        x2settings.filterMag = Texture.TextureFilter.Linear;
        x2settings.filterMin = Texture.TextureFilter.MipMapLinearLinear;

        // Use current path variable
        String gs = (new java.io.File("")).getAbsolutePath();

        // Themes
        Map<String, String> themes = Map.of(
                "default", "#FCBA4C"
                //,"blue", "#92A1BF"
        );

        for (var key : themes.keySet()) {
            try {
                var hex = themes.get(key);
                Color theme = Color.decode(hex);

                // Generate theme
                PurpleReplacer.generateTheme(gs + "/assets/skins/raw/source/", gs + String.format("/assets/skins/raw/%s/", key), theme);

                // Process
                TexturePacker.process(x2settings, gs + String.format("/assets/skins/raw/%s/", key), gs + String.format("/assets/skins/%s/", key), key);

                // Generate theme2 color (more bright)
                float[] hsb = Color.RGBtoHSB(theme.getRed(), theme.getGreen(), theme.getBlue(), null);
                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2] * 1.2f);
                Color theme2 = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, theme.getAlpha());

                // Process and copy theme JSON fil
                File templateFile = new File(gs + "/assets/skins/source/source.json");
                File outputDir = new File(gs + String.format("/assets/skins/%s/", key));
                if (!outputDir.exists()) outputDir.mkdirs();
                File outputFile = new File(outputDir, key + ".json");

                String jsonTemplate = Files.readString(templateFile.toPath());
                String result = replaceColors(jsonTemplate, theme, theme2);

                Files.writeString(outputFile.toPath(), result);
                System.out.println("Written to: " + outputFile.getAbsolutePath());


            } catch (Exception e) {
                System.out.println(String.format("Error generating theme: %s", key));
                throw new RuntimeException(e);
            }
        }

    }

    private static String replaceColors(String template, Color theme, Color theme2) {
        return template
                .replace("\"%theme_r%\"", format(theme.getRed()))
                .replace("\"%theme_g%\"", format(theme.getGreen()))
                .replace("\"%theme_b%\"", format(theme.getBlue()))
                .replace("\"%theme2_r%\"", format(theme2.getRed()))
                .replace("\"%theme2_g%\"", format(theme2.getGreen()))
                .replace("\"%theme2_b%\"", format(theme2.getBlue()));
    }

    private static String format(int value) {
        return String.format("%.4f", value / 255.0);
    }

    public static class PurpleReplacer {
        public static void generateTheme(String inputPath, String outputPath, Color color) throws Exception {
            File inputDir = new File(inputPath);
            File outputDir = new File(outputPath);

            if (!inputDir.isDirectory()) {
                System.err.println("Input path is not a directory.");
                return;
            }

            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    System.err.println("Failed to create output directory.");
                    return;
                }
            }

            for (File file : Objects.requireNonNull(inputDir.listFiles())) {
                if (!file.getName().toLowerCase().endsWith(".png")) continue;

                BufferedImage img = ImageIO.read(file);

                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int argb = img.getRGB(x, y);
                        Color currentColor = new Color(argb, true);

                        if (isPurple(currentColor)) {
                            float[] hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);
                            float brightness = hsb[2];
                            float saturation = hsb[1];

                            float targetHue = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
                            int rgb = Color.HSBtoRGB(targetHue, saturation, brightness);

                            Color newColor = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, currentColor.getAlpha());
                            img.setRGB(x, y, newColor.getRGB());
                        }
                    }
                }

                File outputFile = new File(outputDir, file.getName());
                ImageIO.write(img, "png", outputFile);
                System.out.println("Saved: " + outputFile.getAbsolutePath());
            }

            System.out.println("Done.");
        }

        private static boolean isPurple(Color color) {
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();

            return r > 0 && b > 0 && g < 0.5 * Math.min(r, b);
        }
    }
}
