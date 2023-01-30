package gaiasky.scene.record;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import gaiasky.GaiaSky;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public class HeightDataArray implements IHeightData {
    private static final Log logger = Logger.getLogger(HeightDataArray.class);

    private final float[][] heightData;

    public HeightDataArray(String heightTexture, Runnable finished) {
        String heightUnpacked = GlobalResources.unpackAssetPath(heightTexture);
        GaiaSky.postRunnable(() -> logger.info("Constructing elevation data from texture: " + heightUnpacked));
        Pixmap heightPixmap = new Pixmap(new FileHandle(heightUnpacked));
        Color color = new Color();
        float[][] partialData = new float[heightPixmap.getWidth()][heightPixmap.getHeight()];
        for (int i = 0; i < heightPixmap.getWidth(); i++) {
            for (int j = 0; j < heightPixmap.getHeight(); j++) {
                partialData[i][j] = color.set(heightPixmap.getPixel(i, j)).r;
            }
        }
        heightData = partialData;

        if (finished != null) {
            GaiaSky.postRunnable(finished);
        }
    }

    @Override
    public double getNormalizedHeight(double u, double v) {
        if(heightData == null) {
            return 0;
        }

        // Bi-linear interpolation
        int W = heightData.length;
        int H = heightData[0].length;

        // In our array, Y points downwards, and [0,0] is top-left
        // In textures, V points upwards, and [0,0] is bottom-left
        v = 1.0 - v;

        int i1 = (int) (W * u);
        int i2 = (i1 + 1) % W;
        int j1 = (int) (H * v);
        int j2 = (j1 + 1) % H;

        double dx = 1.0 / W;
        double dy = 1.0 / H;
        double x1 = (double) i1 / (double) W;
        double x2 = (x1 + dx) % 1.0;
        double y1 = (double) j1 / (double) H;
        double y2 = (y1 + dy) % 1.0;

        double f11 = heightData[i1][j1];
        double f21 = heightData[i2][j1];
        double f12 = heightData[i1][j2];
        double f22 = heightData[i2][j2];

        double denominator = (x2 - x1) * (y2 - y1);
        return (((x2 - u) * (y2 - v)) / denominator) * f11 + ((u - x1) * (y2 - v) / denominator) * f21 + ((x2 - u) * (v - y1) / denominator) * f12 + ((u - x1) * (v - y1) / denominator) * f22;
    }
}
