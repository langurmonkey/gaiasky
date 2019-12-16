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

public class PFMReader {

    static public TextureData readPFMTextureData(FileHandle file) {
        try {
            PortableFloatMap pfm = new PortableFloatMap(file.file());
            float[] floatData = pfm.pixels;
            int width = pfm.width;
            int height = pfm.height;

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

    static public Pixmap readPFMPixmap(FileHandle file) {
        try {
            PortableFloatMap pfm = new PortableFloatMap(file.file());
            float[] floatData = pfm.pixels;
            int width = pfm.width;
            int height = pfm.height;
            int totalSize = pfm.pixels.length;

            //com.badlogic.gdx.graphics.glutils.FloatTextureData td = new com.badlogic.gdx.graphics.glutils.FloatTextureData(width, height, GL30.GL_RGB16F, GL30.GL_RGB, GL30.GL_FLOAT, false);
            //td.prepare();
            //td.getBuffer().put(floatData);

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
                    f = (MathUtilsd.clamp(f, 0f, 1f) * 2f) * 255f;
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

}
