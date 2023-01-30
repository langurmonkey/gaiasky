package gaiasky.scene.record;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import gaiasky.GaiaSky;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public class HeightDataPixmap implements IHeightData {
    private static final Log logger = Logger.getLogger(HeightDataPixmap.class);

    private final Pixmap heightPixmap;
    private final Color color;

    public HeightDataPixmap(Pixmap heightPixmap, Runnable finished) {
        this.heightPixmap = heightPixmap;
        color = new Color();

        if (finished != null) {
            GaiaSky.postRunnable(finished);
        }
    }

    public HeightDataPixmap(String heightTexturePacked, Runnable finished) {
        this(new Pixmap(new FileHandle(GlobalResources.unpackAssetPath(heightTexturePacked))), finished);
    }

    public HeightDataPixmap(Texture texture, Runnable finished) {
        color = new Color();
        if (texture != null && texture.getTextureData() instanceof PixmapTextureData) {
            heightPixmap = texture.getTextureData().consumePixmap();
        } else {
            heightPixmap = null;
        }
        if (finished != null) {
            GaiaSky.postRunnable(finished);
        }

    }

    @Override
    public double getNormalizedHeight(double u, double v) {
        if (heightPixmap == null) {
            return 0;
        }

        // Bi-linear interpolation
        int W = heightPixmap.getWidth();
        int H = heightPixmap.getHeight();

        // In pixmaps, Y points downwards, and [0,0] is top-left
        // In textures, V points upwards, and [0,0] is bottom-left
        v = 1.0 - v;

        /*
         * Weighted bi-linear interpolation of p.
         *
         *     i1j2-----------i2j2
         *      |   |          |
         *      |---p----------|
         *      |   |          |
         *      |   |          |
         *      |   |          |
         *     i1j1----------i2j1
         *
         */

        double dx = 1.0 / W;
        double dy = 1.0 / H;
        double dx2 = dx / 2.0;
        double dy2 = dy / 2.0;

        // The texels are sampled at the center of the area they cover,
        // so we need to shift the UV by half a pixel!
        u -= dx2;
        v -= dy2;

        int i1 = (int) Math.floor(W * u);
        if (i1 < 0) {
            i1 = W - 1;
        }
        int i2 = (i1 + 1) % W;
        int j1 = (int) Math.floor(H * v);
        if (j1 < 0) {
            j1 = H - 1;
        }
        int j2 = (j1 + 1) % H;

        double x;
        if (u < 0) {
            // In this special case, we are at the wrapping point (i1 is the last and i2 is the first).
            x = 1 + u + dx2;
        } else {
            // Regular case. Remember to add half a pixel to x_i and j_i to use central values.
            x = u + dx2;
        }
        double y = v + dx2;
        double x1 = ((double) i1 / (double) W) + dx2;
        double x2 = x1 + dx;
        double y1 = ((double) j1 / (double) H) + dy2;
        double y2 = y1 + dy;

        double q11 = color.set(heightPixmap.getPixel(i1, j1)).r;
        double q21 = color.set(heightPixmap.getPixel(i2, j1)).r;
        double q12 = color.set(heightPixmap.getPixel(i1, j2)).r;
        double q22 = color.set(heightPixmap.getPixel(i2, j2)).r;

        double r1 = ((x2 - x) / (x2 - x1)) * q11 + ((x - x1) / (x2 - x1)) * q21;
        double r2 = ((x2 - x) / (x2 - x1)) * q12 + ((x - x1) / (x2 - x1)) * q22;
        double p = ((y2 - y) / (y2 - y1)) * r1 + ((y - y1) / (y2 - y1)) * r2;
        return p;
    }
}
