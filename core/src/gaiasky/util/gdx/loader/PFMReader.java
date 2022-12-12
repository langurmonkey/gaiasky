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
import gaiasky.util.Logger;
import gaiasky.util.math.MathUtilsDouble;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PFMReader {
    private static final Logger.Log logger = Logger.getLogger(PFMReader.class);

    static public TextureData readPFMTextureData(FileHandle file, boolean invert) {

        try {
            PortableFloatMap pfm = new PortableFloatMap(file.file());
            float[] data = pfm.pixels;
            int width = pfm.width;
            int height = pfm.height;
            if (invert)
                data = invertWarp(data, width, height);

            FloatTextureData td = new FloatTextureData(width, height, GL30.GL_RGB16F, GL30.GL_RGB, GL30.GL_FLOAT, false);
            td.prepare();
            FloatBuffer buff = td.getBuffer();
            buff.put(data);

            return td;
        } catch (Exception e) {
            throw new GdxRuntimeException("Couldn't read PFM file '" + file + "'", e);
        } finally {
        }
    }

    static public PFMData readPFMData(FileHandle file, boolean invert) {
        try {
            PortableFloatMap pfm = new PortableFloatMap(file.file());
            float[] data = pfm.pixels;
            int width = pfm.width;
            int height = pfm.height;
            //data = generateMapping(width, height, val -> val * val);
            if (invert) {
                data = invertWarp(data, width, height);
            } else {
                // Normalize
                for (int i = 0; i < data.length; i++)
                    data[i] = (float) nor(data[i]);
            }

            return new PFMData(data, width, height);
        } catch (Exception e) {
            throw new GdxRuntimeException("Couldn't read PFM file '" + file + "'", e);
        } finally {
        }
    }

    static public Pixmap readPFMPixmap(FileHandle file, boolean invert) {
        try {
            PortableFloatMap pfm = new PortableFloatMap(file.file());
            float[] data = pfm.pixels;
            int width = pfm.width;
            int height = pfm.height;
            //int width = 100;
            //int height = 100;
            //float[] floatData = generateMapping(width, height, val -> val);
            if (invert)
                data = invertWarp(data, width, height);
            int totalSize = pfm.pixels.length;
            //int totalSize = width * height * 3;

            // Convert to Pixmap
            Format format = Format.RGB888;
            Pixmap pixmap = new Pixmap(width, height, format);

            ByteBuffer pixelBuf = pixmap.getPixels();
            pixelBuf.position(0);
            pixelBuf.limit(pixelBuf.capacity());

            for (int i = 0; i < totalSize; i++) {
                float f = data[i];
                byte b;
                if (Float.isNaN(f)) {
                    b = (byte) 0;
                } else {
                    f = (float) ((invert ? f : nor(f)) * 255.0);
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

    private static double nor(double value) {
        return MathUtilsDouble.clamp(value + 0.5d, 0d, 1d);
        //return value;
    }

    static public PFMData constructPFMData(int width, int height, Function<Float, Float> f) {
        try {
            float[] data = generateMapping(width, height, f);
            return new PFMData(data, width, height);
        } catch (Exception e) {
            throw new GdxRuntimeException("Couldn't construct PFM data", e);
        } finally {
        }
    }

    private static float[] generateMapping(int w, int h, Function<Float, Float> f) {
        float[] out = new float[w * h * 3];

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                float u = (float) i / (float) (w - 1);
                float v = (float) j / (float) (h - 1);

                // Store this pixel's position at end location
                out[(w * j + i) * 3 + 0] = f.apply(u);
                out[(w * j + i) * 3 + 1] = f.apply(v);
                out[(w * j + i) * 3 + 2] = Float.NaN;
            }
        }
        return out;
    }

    public static List<Quad> generateMesh(float[] d, int w, int h) {
        List<Quad> mesh = new ArrayList<>();
        for (int j = 0; j < h - 1; j++) {
            for (int i = 0; i < w - 1; i++) {
                double u = i / (w - 1d);
                double v = j / (h - 1d);
                double[] origuv = new double[] { u, v };

                int bl = (w * j + i) * 3;
                int br = (w * j + i + 1) * 3;
                int tl = (w * (j + 1) + i) * 3;
                int tr = (w * (j + 1) + i + 1) * 3;
                double[] positions = new double[] { nor(d[bl]), nor(d[bl + 1]), nor(d[br]), nor(d[br + 1]), nor(d[tr]), nor(d[tr + 1]), nor(d[tl]), nor(d[tl + 1]) };

                Quad quad = new Quad(positions, origuv);
                mesh.add(quad);
            }
        }
        return mesh;
    }

    /**
     * Inverts the warp function. The mapping must be invertible, i.e. no folds must be
     * present.
     *
     * @param d The data
     * @param w The source width
     * @param h The source height
     *
     * @return
     */
    private static float[] invertWarp(final float[] d, int w, int h) throws RuntimeException {
        // Create transformed mesh
        List<Quad> mesh = generateMesh(d, w, h);

        double du = 1d / (w - 1d);
        double dv = 1d / (h - 1d);

        boolean warnedFolds = false;
        boolean warnedRange = false;
        // Go over every pixel in final image and map it to original
        float[] out = new float[d.length];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int p = (w * j + i) * 3;
                double u = i / (w - 1d);
                double v = j / (h - 1d);

                List<Quad> matches = mesh.stream().filter(quad -> quad.containsAlt02(u, v)).collect(Collectors.toList());
                if (matches.size() == 0) {
                    // Black
                    out[p + 0] = 0;
                    out[p + 1] = 0;
                    out[p + 2] = Float.NaN;
                    if (!warnedRange) {
                        logger.warn("WARN: The geometry warp forward function does not cover the whole image!");
                        warnedRange = true;
                    }
                } else {
                    if (matches.size() > 1 && !warnedFolds) {
                        // It has folds, warn
                        logger.warn("WARN: The geometry warp forward function has folds!");
                        warnedFolds = true;
                    }
                    // Take first and do inverse bilinear interpolation
                    Quad quad = matches.get(0);
                    double[] uv = quad.invBilinear(u, v);

                    double finalU = quad.origUV[0] + du * uv[0];
                    double finalV = quad.origUV[1] + dv * uv[1];

                    out[p + 0] = (float) finalU;
                    out[p + 1] = (float) finalV;
                    out[p + 2] = Float.NaN;
                }
            }
        }

        return out;
    }

    /**
     * Quad with the mapped and original positions
     */
    private static class Quad {
        double[] positions, origUV;

        public Quad(double[] positions, double[] origUV) {
            this.positions = positions;
            this.origUV = origUV;
        }

        public double areaTri(double x1, double y1, double x2, double y2, double x3, double y3) {
            return 0.5 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
        }

        /**
         * Contains method using areas of triangles
         */
        public boolean containsAlt01(double x, double y) {
            double quadArea = areaTri(positions[0], positions[1], positions[2], positions[3], positions[4], positions[5]) + areaTri(positions[0], positions[1], positions[4], positions[5], positions[6], positions[7]);

            double a1 = areaTri(x, y, positions[0], positions[1], positions[2], positions[3]);
            double a2 = areaTri(x, y, positions[2], positions[3], positions[4], positions[5]);
            double a3 = areaTri(x, y, positions[4], positions[5], positions[6], positions[7]);
            double a4 = areaTri(x, y, positions[6], positions[7], positions[0], positions[1]);

            return (a1 + a2 + a3 + a4) <= quadArea;
        }

        /**
         * Contains method using code in https://github.com/substack/point-in-polygon
         */
        public boolean containsAlt02(double x, double y) {
            boolean inside = false;
            final int nverts = 4;

            for (int i = 0, j = nverts - 1; i < nverts; j = i++) {
                double x1 = positions[i * 2], y1 = positions[i * 2 + 1];
                double x2 = positions[j * 2], y2 = positions[j * 2 + 1];

                boolean intersect = ((y1 > y) != (y2 > y)) && (x < (x2 - x1) * (y - y1) / (y2 - y1) + x1);
                if (intersect)
                    inside = !inside;
            }

            return inside;
        }

        /**
         * Default contains method
         */
        public boolean contains(double x, double y) {
            final int numFloats = 8;
            int intersects = 0;

            for (int i = 0; i < numFloats; i += 2) {
                double x1 = positions[i];
                double y1 = positions[i + 1];
                double x2 = positions[(i + 2) % numFloats];
                double y2 = positions[(i + 3) % numFloats];
                if (((y1 <= y && y < y2) || (y2 <= y && y < y1)) && x < ((x2 - x1) / (y2 - y1) * (y - y1) + x1))
                    intersects++;
            }
            return (intersects & 1) == 1;
        }

        double cross2d(double[] a, double[] b) {
            return a[0] * b[1] - a[1] * b[0];
        }

        private double[] checkPointOnPosition(double x, double y) {
            if (x == positions[0] && y == positions[1]) {
                return new double[] { 0, 0 };
            }
            if (x == positions[2] && y == positions[3]) {
                return new double[] { 1, 0 };
            }
            if (x == positions[4] && y == positions[5]) {
                return new double[] { 1, 1 };
            }
            if (x == positions[6] && y == positions[7]) {
                return new double[] { 0, 1 };
            }
            return null;
        }

        // given a point p and a quad defined by four points {a,b,c,d}, return the bilinear
        // coordinates of p in the quad. Returns (-1,-1) if the point is outside of the quad.
        public double[] invBilinear(double x, double y) {
            double[] uv;
            if ((uv = checkPointOnPosition(x, y)) != null) {
                return uv;
            }

            double[] p = new double[] { x, y };
            double[] a = new double[] { positions[0], positions[1] };
            double[] b = new double[] { positions[2], positions[3] };
            double[] c = new double[] { positions[4], positions[5] };
            double[] d = new double[] { positions[6], positions[7] };

            double[] e = new double[] { b[0] - a[0], b[1] - a[1] };
            double[] f = new double[] { d[0] - a[0], d[1] - a[1] };
            double[] g = new double[] { a[0] - b[0] + c[0] - d[0], a[1] - b[1] + c[1] - d[1] };
            double[] h = new double[] { p[0] - a[0], p[1] - a[1] };

            double k2 = cross2d(g, f);
            double k1 = cross2d(e, f) + cross2d(h, g);
            double k0 = cross2d(h, e);

            double w = k1 * k1 - 4d * k0 * k2;
            if (w < 0d)
                return new double[] { -1d, -1d };
            w = Math.sqrt(w);

            // will fail for k0=0, which is only on the ba edge
            double v = 2d * k0 / (-k1 - w);
            if (v < 0.0 || v > 1.0)
                v = 2d * k0 / (-k1 + w);

            double u = (h[0] - f[0] * v) / (e[0] + g[0] * v);
            if (u < 0.0 || u > 1.0 || v < 0.0 || v > 1.0)
                return new double[] { -1d, -1d };
            return new double[] { u, v };
        }

    }
}
