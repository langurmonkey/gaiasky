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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import gaiasky.GaiaSky;
import gaiasky.scene.record.BilinearInterpolator.GridModel;
import gaiasky.util.GlobalResources;
import gaiasky.util.SysUtils;

public class HeightDataPixmap implements IHeightData {

    private final Pixmap heightPixmap;
    private GridModel model;
    private final float heightScale;

    public HeightDataPixmap(Pixmap heightPixmap, Runnable finished, float heightScale) {
        this.heightScale = heightScale;
        this.heightPixmap = heightPixmap;

        if (finished != null) {
            GaiaSky.postRunnable(finished);
        }
        initModel();
    }

    public HeightDataPixmap(String heightTexturePacked, Runnable finished, float heightScale) {
        this(new Pixmap(new FileHandle(GlobalResources.unpackAssetPath(heightTexturePacked))), finished, heightScale);
    }

    public HeightDataPixmap(Texture texture, Runnable finished, float heightScale) {
        this.heightScale = heightScale;
        if (texture != null && texture.getTextureData() instanceof PixmapTextureData) {
            // Directly get pixmap texture data.
            heightPixmap = texture.getTextureData().consumePixmap();
            initModel();
        } else if (texture != null && texture.getTextureData() instanceof FileTextureData fileTextureData) {
            // Load it.
            heightPixmap = new Pixmap(fileTextureData.getFileHandle());
            initModel();
        } else if (texture != null && texture.getTextureData() instanceof GLOnlyTextureData) {
            // GL data.
            heightPixmap = SysUtils.pixmapFromGLTexture(texture);
            initModel();
        } else {
            // Nothing.
            heightPixmap = null;
        }
        if (finished != null) {
            GaiaSky.postRunnable(finished);
        }
    }

    private void initModel(){
        this.model = new GridModel() {
            private final Color color = new Color();
            @Override
            public int getWidth() {
                assert heightPixmap != null;
                return heightPixmap.getWidth();
            }

            @Override
            public int getHeight() {
                assert heightPixmap != null;
                return heightPixmap.getHeight();
            }

            @Override
            public double getValue(int x, int y) {
                assert heightPixmap != null;
                return color.set(heightPixmap.getPixel(x, y)).r * heightScale;
            }
        };
    }

    @Override
    public double getNormalizedHeight(double u, double v) {
        if (heightPixmap == null) {
            return 0;
        }

        return BilinearInterpolator.interpolate(u, v, model, true, false);
    }
}
