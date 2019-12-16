/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.FloatTextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.util.math.MathUtilsd;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;

public class PFMReader {

    static public TextureData readPFMTextureData(FileHandle file, boolean invert) {
        try {
            PortableFloatMap pfm = new PortableFloatMap(file.file());
            float[] floatData = pfm.pixels;
            int width = pfm.width;
            int height = pfm.height;
            if (invert)
                floatData = invertMapping(floatData, width, height);

            FloatTextureData td = new FloatTextureData(width, height, GL30.GL_RGB16F, GL30.GL_RGB, GL30.GL_FLOAT, false);
            td.prepare();
            FloatBuffer buff = td.getBuffer();
            buff.put(floatData);

            return td;
        } catch (Exception e) {
            throw new GdxRuntimeException("Couldn't read PFM file '" + file + "'", e);
        } finally {
        }
    }

    static public Pixmap readPFMPixmap(FileHandle file, boolean invert) {
        try {
            PortableFloatMap pfm = new PortableFloatMap(file.file());
            float[] floatData = pfm.pixels;
            int width = pfm.width;
            int height = pfm.height;
            if (invert)
                floatData = invertMapping(floatData, width, height);
            int totalSize = pfm.pixels.length;

            // Convert to Pixmap
            Format format = Format.RGB888;
            Pixmap pixmap = new Pixmap(width, height, format);

            ByteBuffer pixelBuf = pixmap.getPixels();
            pixelBuf.position(0);
            pixelBuf.limit(pixelBuf.capacity());

            for (int i = 0; i < totalSize; i++) {
                float f = floatData[i];
                byte b;
                if (Float.isNaN(f)) {
                    b = (byte) 0;
                } else {
                    f = (invert ? f : normalize(f)) * 255f;
                    b = (byte) f;
                }

                pixelBuf.put(b);
            }

            pixelBuf.position(0);
            pixelBuf.limit(pixelBuf.capacity());

            return pixmap;
        } catch (Exception e) {
            throw new GdxRuntimeException("Couldn't read PFM file '" + file + "'", e);
        } finally {
        }
    }

    private static float normalize(float value) {
        return MathUtilsd.clamp(value + 0.5f, 0f, 1f);
        //return value;
    }

    private static float[] invertMapping(final float[] data, int w, int h) {
        boolean bilinear = false;

        float[] out = new float[data.length];
        if (data.length != w * h * 3) {
            return null;
        }

        Set<Integer> positions = new HashSet<>();

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int p = (w * i + j) * 3;

                float r = data[p + 0];
                float g = data[p + 1];
                float b = data[p + 2];

                // Normalize
                r = normalize(r);
                g = normalize(g);
                b = normalize(b);

                // Find end location of this pixel
                int ip = (int) ((w - 1) * r);
                int jp = (int) ((h - 1) * g);

                // Store this pixel's position at end location
                int pp = (w * ip + jp) * 3;
                // Prevent double-mapping
                if (!positions.contains(pp)) {
                    out[pp + 0] = (float) i / (w - 1f);
                    out[pp + 1] = (float) j / (h - 1f);
                    out[pp + 2] = b;

                    positions.add(pp);
                }
            }
        }

        if(bilinear) {
            // Fill the gaps by bilinear interpolation
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int p = (w * i + j) * 3;

                    if (!positions.contains(p)) {
                        float r = bilinear(out, i, j, w, h, 0);
                        float g = bilinear(out, i, j, w, h, 1);
                        float b = bilinear(out, i, j, w, h, 2);

                        out[p + 0] = r;
                        out[p + 1] = g;
                        out[p + 2] = b;
                    }
                }
            }
        }

        return out;
    }

    /**
     * Performs a bilinear interpolation on the array d [w, h] with the given color channel c and
     * coordinates [i, j]
     *
     * @param d Array
     * @param i Horizontal position
     * @param j Vertical position
     * @param w Width
     * @param h Height
     * @param c Channel
     * @return
     */
    private static float bilinear(float[] d, int i, int j, int w, int h, int c) {
        int ir = i + 1 >= w ? w - 1 : i + 1;
        int il = i - 1 < 0 ? 0 : i - 1;
        int ju = j + 1 >= h ? h - 1 : j + 1;
        int jd = j - 1 < 0 ? 0 : j - 1;

        float ru = d[(w * i + ju) * 3 + c];
        float rd = d[(w * i + jd) * 3 + c];
        float rr = d[(w * ir + j) * 3 + c];
        float rl = d[(w * il + j) * 3 + c];

        // Bilinear interpolation
        float rvert = MathUtilsd.lerp(ru, rd, 0.5f);
        float rhor = MathUtilsd.lerp(rl, rr, 0.5f);
        return MathUtilsd.lerp(rvert, rhor, 0.5f);
    }

    private static float[] generateIdentity(int w, int h) {
        float[] out = new float[w * h * 3];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                float u = (float) i / (float) (w - 1);
                float v = (float) j / (float) (h - 1);

                // Store this pixel's position at end location
                out[(w * i + j) * 3 + 0] = u;
                out[(w * i + j) * 3 + 1] = v;
                out[(w * i + j) * 3 + 2] = Float.NaN;
            }
        }
        return out;
    }

}
