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
            floatData = generateSqrt(width, height);
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
        //return MathUtilsd.clamp(value + 0.5f, 0f, 1f);
        return value;
    }

    /**
     * Inverts the mapping in data. The mapping must be bijective, i.e. no folds must be
     * present. If folds are present, the inverse function can't decide which of the sources
     * to choose.
     *
     * @param data
     * @param w
     * @param h
     * @return
     */
    private static float[] invertMapping(final float[] data, int w, int h) {
        boolean bilinear = true;

        float[] out = new float[data.length];
        if (data.length != w * h * 3) {
            return null;
        }

        Set<Integer> positions = new HashSet<>();

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int p = (w * j + i) * 3;

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
                int pp = (w * jp + ip) * 3;
                // Prevent double-mapping
                if (true || !positions.contains(pp)) {
                    out[pp + 0] = (float) i / (w - 1f);
                    out[pp + 1] = (float) j / (h - 1f);
                    out[pp + 2] = b;

                    positions.add(pp);
                }
            }
        }

        if (bilinear) {
            while(bilinearInterpolation(out, w, h, positions) != 0){};
        }

        return out;
    }

    private static int bilinearInterpolation(float[] out, int w, int h, Set<Integer> positions) {
        int untreated = 0;
        // Fill the gaps by bilinear interpolation in the horizontal and vertical directions
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int p = (w * j + i) * 3;

                if (!positions.contains(p)) {
                    float r = bilinear(out, i, j, w, h, 0, positions);
                    float g = bilinear(out, i, j, w, h, 1, positions);
                    float b = bilinear(out, i, j, w, h, 2, positions);

                    if(!Float.isNaN(r) && !Float.isNaN(g)) {
                        out[p + 0] = r;
                        out[p + 1] = g;
                        out[p + 2] = b;

                        positions.add(p);
                    } else {
                        untreated++;
                    }
                }
            }
        }
        return untreated;
    }

    /**
     * Performs a bilinear interpolation on the array d [w, h] with the given color channel c and
     * coordinates [i, j]
     *
     * @param d         Array
     * @param i         Horizontal position
     * @param j         Vertical position
     * @param w         Width
     * @param h         Height
     * @param c         Channel
     * @param positions Set with all treated positions
     * @return
     */
    private static float bilinear(float[] d, int i, int j, int w, int h, int c, Set<Integer> positions) {
        int ir = findRight(d, i, j, w, positions);
        int il = findLeft(d, i, j, w, positions);
        int ju = findUp(d, i, j, w, h, positions);
        int jd = findDown(d, i, j, w, h, positions);

        float rvert, rhor;

        // Horizontal
        if (ir > w - 1 && il < 0) {
            // No horizontal
            rhor = Float.NaN;
        } else if (ir > w - 1) {
            rhor = d[(w * j + il) * 3 + c];
        } else if (il < 0) {
            rhor = d[(w * j + ir) * 3 + c];
        } else {
            float rr = d[(w * j + ir) * 3 + c];
            float rl = d[(w * j + il) * 3 + c];
            rhor = MathUtilsd.lerp(rl, rr, 0.5f);
        }

        // Vertical
        if (ju > h - 1 && jd < 0) {
            // No vertical
            rvert = Float.NaN;
        } else if (ju > h - 1) {
            rvert = d[(w * jd + i) * 3 + c];
        } else if (jd < 0) {
            rvert = d[(w * ju + i) * 3 + c];
        } else {
            float ru = d[(w * ju + i) * 3 + c];
            float rd = d[(w * jd + i) * 3 + c];
            rvert = MathUtilsd.lerp(ru, rd, 0.5f);
        }

        float total;
        if(Float.isNaN(rhor) && Float.isNaN(rvert)){
            total = Float.NaN;
        } else if (Float.isNaN(rhor)) {
            total = rvert;
        }else if(Float.isNaN(rvert)){
            total = rhor;
        } else {
            total = MathUtilsd.lerp(rvert, rhor, 0.5f);
        }
        return total;
    }

    private static int findRight(float[] d, int i, int j, int w, Set<Integer> positions) {
        int ir = i + 1;
        int p = (w * j + ir) * 3;
        while (ir < w - 1 && !positions.contains(p)) {
            ir += 1;
            p = (w * j + ir) * 3;
        }
        return ir;
    }

    private static int findLeft(float[] d, int i, int j, int w, Set<Integer> positions) {
        int il = i - 1;
        int p = (w * j + il) * 3;
        while (il > 0 && !positions.contains(p)) {
            il -= 1;
            p = (w * j + il) * 3;
        }
        return il;
    }

    private static int findUp(float[] d, int i, int j, int w, int h, Set<Integer> positions) {
        int ju = j + 1;
        int p = (w * ju + i) * 3;
        while (ju < h - 1 && !positions.contains(p)) {
            ju += 1;
            p = (w * ju + i) * 3;
        }
        return ju;
    }

    private static int findDown(float[] d, int i, int j, int w, int h, Set<Integer> positions) {
        int jd = j - 1;
        int p = (w * jd + i) * 3;
        while (jd > 0 && !positions.contains(p)) {
            jd -= 1;
            p = (w * jd + i) * 3;
        }
        return jd;
    }

    private static float[] generateIdentity(int w, int h) {
        float[] out = new float[w * h * 3];

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                float u = (float) i / (float) (w - 1);
                float v = (float) j / (float) (h - 1);

                // Store this pixel's position at end location
                out[(w * j + i) * 3 + 0] = u;
                out[(w * j + i) * 3 + 1] = v;
                out[(w * j + i) * 3 + 2] = Float.NaN;
            }
        }
        return out;
    }

    private static float[] generateSqrt(int w, int h) {
        float[] out = new float[w * h * 3];

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                float u = (float) i / (float) (w - 1);
                float v = (float) j / (float) (h - 1);

                // Store this pixel's position at end location
                out[(w * j + i) * 3 + 0] = (float) Math.sqrt(u);
                out[(w * j + i) * 3 + 1] = (float) Math.sqrt(v);
                out[(w * j + i) * 3 + 2] = Float.NaN;
            }
        }
        return out;
    }

}
