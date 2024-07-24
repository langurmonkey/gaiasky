/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.color;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import gaiasky.util.Logger;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.parse.Parser;
import net.jafama.FastMath;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class ColorUtils {

    /*
     * LETS DEFINE SOME COLORS
     */

    // Google colors
    public static float[] gGreen = new float[] { 15f / 255f, 157f / 255f, 88f / 255f, 1f };
    public static Color gGreenC = getCol(gGreen);
    public static float[] gBlue = new float[] { 66f / 255f, 133f / 255f, 244f / 255f, 1f };
    public static Color gBlueC = getCol(gBlue);
    public static float[] gRed = new float[] { 219f / 255f, 68f / 255f, 55f / 255f, 1f };
    public static Color gRedC = getCol(gRed);
    public static float[] gYellow = new float[] { 244f / 255f, 160f / 255f, 0f / 255f, 1f };
    public static Color gYellowC = getCol(gYellow);
    public static float[] gWhite = new float[] { 1.0f, 1.0f, 1.0f, 1f };
    public static Color gWhiteC = getCol(gWhite);
    public static float[] gPink = new float[] { 1.0f, 102f / 255f, 1.0f, 1f };
    public static Color gPinkC = getCol(gPink);
    // Amazon orange
    public static float[] aOrange = new float[] { 1.0f, 153f / 255f, 0f / 255f, 1f };
    public static Color aOrangeC = getCol(aOrange);
    // Taco Bell purple
    public static float[] tPurple = new float[] { 12f / 255f, 32f / 255f, 130f / 255f, 1f };
    public static Color tPurpleC = getCol(tPurple);
    // DunkinDonuts
    public static float[] ddMagenta = new float[] { 218f / 255f, 24f / 255f, 132f / 255f, 1f };
    public static Color ddMagentaC = getCol(ddMagenta);
    public static float[] ddBrown = new float[] { 101f / 255f, 56f / 255f, 25f / 255f, 1f };
    public static Color ddBrownC = getCol(ddBrown);
    // Others
    public static float[] oLighterGray = new float[] { 0.85f, 0.85f, 0.85f, 1f };
    public static Color oLighterGrayC = getCol(oLighterGray);
    public static float[] oLightGray = new float[] { 0.85f, 0.85f, 0.85f, 1f };
    public static Color oLightGrayC = getCol(oLightGray);
    public static float[] oDarkGray = new float[] { 0.3f, 0.3f, 0.3f, 1f };
    public static Color oDarkGrayC = getCol(oDarkGray);
    public static float[] oCyan = new float[] { 0f, 230f / 255f, 1f, 1f };
    public static Color oCyanC = getCol(ddMagenta);
    /**
     * Highlight color array for datasets
     **/
    public static float[][] colorArray = new float[][] { gBlue, gRed, gYellow, gGreen, gPink, aOrange, tPurple, ddBrown, ddMagenta, oCyan };
    private static float[][] teffToRGB_harre;

    private static Color getCol(float[] c) {
        return new Color(c[0], c[1], c[2], c[3]);
    }

    public static float[] getColorFromIndex(int idx) {
        return colorArray[idx % colorArray.length];
    }

    public static float[] getRgbaComplimentary(float[] rgba) {
        float[] hsb = rgbToHsb(rgba);
        float hue = hsb[0] * 360f;
        hsb[0] = ((hue + 180f) % 360f) / 360f;
        float[] rgb = hsbToRgb(hsb);
        return new float[] { rgb[0], rgb[1], rgb[2], rgba[3] };
    }

    public static float[] getRgbComplimentary(float[] rgb) {
        float[] hsb = rgbToHsb(rgb);
        float hue = hsb[0] * 360f;
        hsb[0] = ((hue + 180f) % 360f) / 360f;
        hsb[0] = 1f - hsb[0];
        return hsbToRgb(hsb);
    }

    public static float[] rgbToHsb(float[] color) {
        int r = (int) (color[0] * 255f);
        int g = (int) (color[1] * 255f);
        int b = (int) (color[2] * 255f);
        return java.awt.Color.RGBtoHSB(r, g, b, null);
    }

    public static float[] hsbToRgb(float[] hsb) {
        java.awt.Color c = new java.awt.Color(java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        return new float[] { c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f };

    }

    public static String rgbaToHex(float[] color) {
        int r = (int) (color[0] * 255f);
        int g = (int) (color[1] * 255f);
        int b = (int) (color[2] * 255f);
        int a = (int) (color[3] * 255f);
        return String.format("#%02x%02x%02x%02x", r, g, b, a);
    }

    public static String rgbToHex(float[] color) {
        int r = (int) (color[0] * 255f);
        int g = (int) (color[1] * 255f);
        int b = (int) (color[2] * 255f);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    public static float[] hexToRgba(String hex) {
        return new float[] { Integer.valueOf(hex.substring(1, 3), 16) / 255f, Integer.valueOf(hex.substring(3, 5), 16) / 255f, Integer.valueOf(hex.substring(5, 7), 16) / 255f, Integer.valueOf(hex.substring(7, 9), 16) / 255f };
    }

    public static float[] hexToRgb(String hex) {
        return new float[] { Integer.valueOf(hex.substring(1, 3), 16) / 255f, Integer.valueOf(hex.substring(3, 5), 16) / 255f, Integer.valueOf(hex.substring(5, 7), 16) / 255f };

    }

    public static float normalize(float value, float min, float max) {
        if (value > max)
            return max;
        if (value < min)
            return min;
        return (value - min) / (max - min);
    }

    /**
     * Converts a scalar value that is normalized to [0:1] into a grayscale
     * color vector rgba. See: {@see http://www.particleincell.com/blog/2014/colormap/}.
     *
     * @param value The grayscale value to set to the color.
     * @param rgba The out parameter containing the color.
     */
    public static void grayscale(float value, float[] rgba) {
        rgba[0] = value;
        rgba[1] = value;
        rgba[2] = value;
        rgba[3] = 1;
    }

    /**
     * Converts a scalar normalized to the range [0:1] into a blue-white-red
     * rgba color, with blue at 0, white at 0.5 and red at 1.
     *
     * @param value The value.
     * @param rgba  The out parameter containing the color.
     */
    public static void colormap_blue_white_red(float value, float[] rgba) {
        // Make it in [-1:1]
        float a = value * 2f - 1f;
        if (a <= 0) {
            rgba[0] = rgba[1] = 1 + a;
            rgba[2] = 1;
        } else {
            rgba[0] = 1;
            rgba[1] = rgba[2] = 1 - a;
        }
    }

    /**
     * Converts a scalar normalized to the range [0:1] into a short rainbow of
     * rgba values. See: {@see http://www.particleincell.com/blog/2014/colormap/}.
     *
     * @param value The value.
     * @param rgba  The out parameter containing the color.
     */
    public static void colormap_short_rainbow(float value, float[] rgba) {
        /* plot short rainbow RGB */
        float a = (1 - value) / 0.25f; //invert and group
        final int X = (int) FastMath.floor(a); //this is the integer part
        float Y = (a - X); //fractional part from 0 to 1

        rgba[3] = 1;

        switch (X) {
        case 0:
            rgba[0] = 1;
            rgba[1] = Y;
            rgba[2] = 0;
            break;
        case 1:
            rgba[0] = 1 - Y;
            rgba[1] = 1;
            rgba[2] = 0;
            break;
        case 2:
            rgba[0] = 0;
            rgba[1] = 1;
            rgba[2] = Y;
            break;
        case 3:
            rgba[0] = 0;
            rgba[1] = 1 - Y;
            rgba[2] = 1;
            break;
        case 4:
            rgba[0] = 0;
            rgba[1] = 0;
            rgba[2] = 1;
            break;
        }
    }

    /**
     * Converts a scalar in [0..1] to a long rainbow of rgba values. See
     * <a href="http://www.particleincell.com/blog/2014/colormap/">here</a>.
     *
     * @param value The value in [0..1].
     * @param rgba  The out parameter containing the color.
     */
    public static void colormap_long_rainbow(float value, float[] rgba) {
        if (rgba == null)
            return;
        /* plot long rainbow RGB */
        float a = (1 - value) / 0.2f; //invert and group
        final int X = (int) FastMath.floor(a); //this is the integer part
        float Y = (a - X); //fractional part from 0 to 1

        rgba[3] = 1;

        switch (X) {
        case 0:
            rgba[0] = 1;
            rgba[1] = Y;
            rgba[2] = 0;
            break;
        case 1:
            rgba[0] = 1 - Y;
            rgba[1] = 1;
            rgba[2] = 0;
            break;
        case 2:
            rgba[0] = 0;
            rgba[1] = 1;
            rgba[2] = Y;
            break;
        case 3:
            rgba[0] = 0;
            rgba[1] = 1 - Y;
            rgba[2] = 1;
            break;
        case 4:
            rgba[0] = Y;
            rgba[1] = 0;
            rgba[2] = 1;
            break;
        case 5:
            rgba[0] = 1;
            rgba[1] = 0;
            rgba[2] = 1;
            break;
        }
    }

    /**
     * Converts a scalar in [0..1] to a yellow to red map. See
     * <a href="http://www.particleincell.com/blog/2014/colormap/">here</a>.
     *
     * @param value The value to convert.
     * @param rgba  The out parameter containing the color.
     */
    public static void colormap_yellow_to_red(float value, float[] rgba) {
        rgba[0] = 1;
        rgba[1] = value;
        rgba[2] = 0;
    }

    public static void colormap_blue_to_magenta(float value, float[] rgba) {
        rgba[0] = value;
        rgba[1] = 0;
        rgba[2] = 1;
    }

    /**
     * Initializes the data table needed for the Harre and Heller color conversion.
     */
    private static void initHarreData() {
        if (teffToRGB_harre == null) {
            teffToRGB_harre = new float[105][];
            FileHandle fh = Gdx.files.internal("data/teff-rgb.csv.gz");
            try (var gzipStream = new GZIPInputStream(fh.read());
                    var reader = new BufferedReader(new InputStreamReader(gzipStream))) {
                // Skip header
                reader.readLine();
                int i = 0;
                while (reader.ready()) {
                    String[] tokens = reader.readLine().split(",");

                    float[] data = new float[4];
                    data[0] = Parser.parseFloat(tokens[0]);
                    data[1] = Parser.parseFloat(tokens[1]);
                    data[2] = Parser.parseFloat(tokens[2]);
                    data[3] = Parser.parseFloat(tokens[3]);
                    teffToRGB_harre[i] = data;

                    i++;
                }
            } catch (Exception e) {
                Logger.getLogger("ColorUtils").error("Error parsing teff-rgb.csv file");
                Logger.getLogger("ColorUtils").error(e);
            }
        }
    }

    /**
     * Convert effective temperature to RGB using the Harre and Heller 2021 (Digital Color of Stars)
     * method.
     *
     * @param tEff The effective temperature of the star.
     *
     * @return The RGB color in a float array.
     *
     * @see <a href="https://ui.adsabs.harvard.edu/abs/2021arXiv210106254H/abstract">Paper at ADS</a>
     */
    public static float[] tEffToRGB_harre(double tEff) {
        initHarreData();
        float[] rgb = new float[3];
        int idx = 0;
        for (int i = 1; i < teffToRGB_harre.length; i++) {
            float tEff0 = teffToRGB_harre[i - 1][0];
            float tEff1 = teffToRGB_harre[i][0];
            if (tEff < tEff0) {
                break;
            } else if (tEff >= tEff0 && tEff < tEff1) {
                // Found
                if (tEff - tEff0 < tEff1 - tEff) {
                    idx = i - 1;
                } else {
                    idx = i;
                }
                break;
            } else {
                // Take last
                idx = i;
            }
        }

        rgb[0] = teffToRGB_harre[idx][1];
        rgb[1] = teffToRGB_harre[idx][2];
        rgb[2] = teffToRGB_harre[idx][3];
        return rgb;
    }

    /**
     * Converts effective temperature in Kelvin (1000-40000) to RGB.
     *
     * @param tEff Effective temperature.
     *
     * @return The RGB color in a float array.
     *
     * @see <a href="www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/">Temperature to RGB</a>
     */
    public static float[] tEffToRGB_rough(double tEff) {
        double r, g, b;

        double temp = tEff / 100;

        // Red
        if (temp <= 66) {
            r = 255;
        } else {
            double x = temp - 55;
            r = 351.97690566805693 + 0.114206453784165 * x - 40.25366309332127 * FastMath.log(x);
            r = MathUtilsDouble.clamp(r, 0, 255);
        }

        // Green
        double x;
        if (temp <= 66) {
            x = temp - 2;
            g = -155.25485562709179 - 0.44596950469579133 * x + 104.49216199393888 * FastMath.log(x);
        } else {
            x = temp - 50;
            g = 325.4494125711974 + 0.07943456536662342 * x - 28.0852963507957 * FastMath.log(x);
        }
        g = MathUtilsDouble.clamp(g, 0, 255);

        // Blue
        if (temp >= 66) {
            b = 255;
        } else {
            if (temp <= 19) {
                b = 0;
            } else {
                x = temp - 10;
                b = -254.76935184120902 + 0.8274096064007395 * x + 115.67994401066147 * FastMath.log(x);
                b = MathUtilsDouble.clamp(b, 0, 255);
            }
        }

        return new float[] { (float) (r / 255d), (float) (g / 255d), (float) (b / 255d) };
    }

    /**
     * Converts the color index B-V to RGB model. See <a href=
     * "http://stackoverflow.com/questions/21977786/star-b-v-color-index-to-apparent-rgb-color">here</a>
     *
     * @param bv The B-V color index
     * @return The RGB as a float array in [0..1]
     */
    public static float[] BVtoRGB(double bv) {
        double t = 4600 * ((1 / ((0.92 * bv) + 1.7)) + (1 / ((0.92 * bv) + 0.62)));
        // t to xyY
        double x = 0, y = 0;

        if (t >= 1667 && t <= 4000) {
            x = ((-0.2661239 * FastMath.pow(10, 9)) / FastMath.pow(t, 3)) + ((-0.2343580 * FastMath.pow(10, 6)) / FastMath.pow(t, 2)) + ((0.8776956 * FastMath.pow(10, 3)) / t) + 0.179910;
        } else if (t > 4000 && t <= 25000) {
            x = ((-3.0258469 * FastMath.pow(10, 9)) / FastMath.pow(t, 3)) + ((2.1070379 * FastMath.pow(10, 6)) / FastMath.pow(t, 2)) + ((0.2226347 * FastMath.pow(10, 3)) / t) + 0.240390;
        }

        if (t >= 1667 && t <= 2222) {
            y = -1.1063814 * FastMath.pow(x, 3) - 1.34811020 * FastMath.pow(x, 2) + 2.18555832 * x - 0.20219683;
        } else if (t > 2222 && t <= 4000) {
            y = -0.9549476 * FastMath.pow(x, 3) - 1.37418593 * FastMath.pow(x, 2) + 2.09137015 * x - 0.16748867;
        } else if (t > 4000 && t <= 25000) {
            y = 3.0817580 * FastMath.pow(x, 3) - 5.87338670 * FastMath.pow(x, 2) + 3.75112997 * x - 0.37001483;
        }

        // xyY to XYZ, Y = 1
        double Y = (y == 0) ? 0 : 1;
        double X = (y == 0) ? 0 : (x * Y) / y;
        double Z = (y == 0) ? 0 : ((1 - x - y) * Y) / y;

        float[] cc = new float[4];

        cc[0] = correctGamma(3.2406 * X - 1.5372 * Y - 0.4986 * Z);
        cc[1] = correctGamma(-0.9689 * X + 1.8758 * Y + 0.0415 * Z);
        cc[2] = correctGamma(0.0557 * X - 0.2040 * Y + 1.0570 * Z);

        float max = FastMath.max(1, FastMath.max(cc[2], FastMath.max(cc[0], cc[1])));

        cc[0] = FastMath.max(cc[0] / max, 0f);
        cc[1] = FastMath.max(cc[1] / max, 0f);
        cc[2] = FastMath.max(cc[2] / max, 0f);

        return cc;
    }

    private static float correctGamma(double clinear) {
        float result;
        if (clinear <= 0.0031308) {
            result = 12.92f * (float) clinear;
        } else {
            // use 0.05 for pale colors, 0.5 for vivid colors
            float a = 0.5f;
            result = (float) ((1 + a) * FastMath.pow(clinear, 1 / 2.4f) - a);
        }
        return result;
    }

    /**
     * Returns a copy of the RGB colour brightened up by the given amount
     *
     * @param rgb        The RGB color
     * @param luminosity The new luminosity amount in [0..1]
     * @return The new RGB array
     */
    public static float[] brighten(float[] rgb, float luminosity) {
        float[] hsl = rgbToHsl(rgb);
        hsl[2] = luminosity;
        return hslToRgb(hsl);
    }

    /**
     * Converts an RGB color value to HSL. Conversion formula adapted from
     * <a href="http://en.wikipedia.org/wiki/HSL_color_space">here</a>. Assumes r, g, and b are
     * contained in the set [0..255] and returns h, s, and l in the set [0..1]
     *
     * @param rgb Float array with the RGB values
     * @return Array The HSL representation
     */
    public static float[] rgbToHsl(float[] rgb) {
        float r, g, b;
        r = rgb[0];
        g = rgb[1];
        b = rgb[2];
        float max = FastMath.max(r, FastMath.max(g, b));
        float min = FastMath.min(r, FastMath.min(g, b));
        float avg = (max + min) / 2;
        float h = avg, s;

        if (max == min) {
            h = s = 0; // achromatic
        } else {
            float d = max - min;
            s = avg > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else if (max == b) {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }

        return new float[] { h, s, avg};
    }

    /**
     * Converts an HSL color value to RGB. Conversion formula adapted from
     * <a href="https://en.wikipedia.org/wiki/HSL_color_space">here</a>. Assumes h, s, and l are
     * contained in the set [0..1] and returns r, g, and b in the set [0..255].
     *
     * @param hsl Float array with the HSL values
     * @return Array The RGB representation
     */
    public static float[] hslToRgb(float[] hsl) {
        float r, g, b;
        float h, s, l;
        h = hsl[0];
        s = hsl[1];
        l = hsl[2];

        if (s == 0) {
            r = g = b = l; // achromatic
        } else {

            float q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hue2rgb(p, q, h + 1 / 3);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1 / 3);
        }

        return new float[] { r, g, b };
    }

    private static float hue2rgb(float p, float q, float t) {
        if (t < 0)
            t += 1;
        if (t > 1)
            t -= 1;
        if (t < 1 / 6)
            return p + (q - p) * 6 * t;
        if (t < 1f / 2f)
            return q;
        if (t < 2f / 3f)
            return p + (q - p) * (2 / 3 - t) * 6;
        return p;
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) FastMath.floor(hue)) * 6.0f;
            float f = h - (float) java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
            case 0:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (t * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 1:
                r = (int) (q * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 2:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (t * 255.0f + 0.5f);
                break;
            case 3:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (q * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 4:
                r = (int) (t * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 5:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (q * 255.0f + 0.5f);
                break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b);
    }

    public static int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int getBlue(int rgb) {
        return (rgb) & 0xFF;
    }

    public static boolean isZero(Color c) {
        return c.r == 0 && c.g == 0 && c.b == 0;
    }

}
