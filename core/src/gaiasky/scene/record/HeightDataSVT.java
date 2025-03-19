/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import gaiasky.scene.record.BilinearInterpolator.GridModel;
import gaiasky.util.svt.SVTQuadtree;
import net.jafama.FastMath;

import java.nio.file.Path;

public final class HeightDataSVT implements IHeightData {

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
            v = 1.0 - v;
            for (int level = svt.depth; level >= 0; level--) {
                int[] cr = svt.getColRow(level, u, v);
                if (svt.contains(level, cr[0], cr[1])) {
                    // Hit! Query this, if loaded!
                    var tile = svt.getTile(level, cr[0], cr[1]);
                    if (manager.contains(tile.object.toString())) {
                        Pixmap pm = manager.get(tile.object.toString());
                        double[] tileUV = tile.getUV();
                        double tilesPerLevel = FastMath.pow(2.0, level);
                        double tileU = (u - tileUV[0]) * tilesPerLevel * svt.root.length;
                        double tileV = (v - tileUV[1]) * tilesPerLevel;
                        model.setPixmap(pm);
                        return BilinearInterpolator.interpolate(tileU, tileV, model, false, false);
                    }
                }
            }
        }
        return 0;
    }

    private static class PixmapGridModel implements GridModel {
        private Pixmap pixmap;
        private final Color color;

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
