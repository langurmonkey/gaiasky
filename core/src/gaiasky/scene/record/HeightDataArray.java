/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import gaiasky.GaiaSky;
import gaiasky.scene.record.BilinearInterpolator.GridModel;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public class HeightDataArray implements IHeightData {
    private static final Log logger = Logger.getLogger(HeightDataArray.class);

    private final float[][] heightData;
    private final GridModel model;

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

        model = new GridModel() {

            @Override
            public int getWidth() {
                return heightData.length;
            }

            @Override
            public int getHeight() {
                return heightData[0].length;
            }

            @Override
            public double getValue(int x, int y) {
                return heightData[x][y];
            }
        };
    }

    @Override
    public double getNormalizedHeight(double u, double v) {
        if (heightData == null) {
            return 0;
        }

        return BilinearInterpolator.interpolate(u, v, model, true, false);
    }
}
