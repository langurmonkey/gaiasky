package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import gaiasky.scene.record.BilinearInterpolator.GridModel;
import gaiasky.util.svt.SVTQuadtree;

import java.nio.file.Path;

public class HeightDataSVT implements IHeightData {

    private final SVTQuadtree<Path> svt;
    private final AssetManager manager;
    private final PixmapGridModel model;

    public HeightDataSVT(SVTQuadtree<Path> svt, AssetManager manager) {
        this.svt = svt;
        this.manager = manager;
        this.model = new PixmapGridModel();
    }

    @Override
    public double getNormalizedHeight(double u, double v) {
        if (svt != null) {
            for (int level = svt.depth; level >= 0; level--) {
                int[] cr = svt.getColRow(level, u, v);
                if (svt.contains(level, cr[0], cr[1])) {
                    // Hit! Query this, if loaded!
                    var tile = svt.getTile(level, cr[0], cr[1]);
                    if (manager.contains(tile.object.toString())) {
                        Pixmap pm = manager.get(tile.object.toString());
                        double[] tileUV = tile.getUV();
                        double tileU = u - tileUV[0];
                        double tileV = v - tileUV[1];
                        model.setPixmap(pm);
                        return BilinearInterpolator.interpolate(tileU, tileV, model, false, false);
                    }
                }
            }
        }
        return 0;
    }

    private class PixmapGridModel implements GridModel {
        private Pixmap pixmap;
        private Color color;

        public PixmapGridModel() {
            this.color = new Color();
        }

        public void setPixmap(Pixmap pixmap) {
            this.pixmap = pixmap;
        }
        @Override
        public int getWidth() {
            return pixmap.getWidth();
        }

        @Override
        public int getHeight() {
            return pixmap.getHeight();
        }

        @Override
        public double getValue(int x, int y) {
            return color.set(pixmap.getPixel(x, y)).r;
        }
    }
}
