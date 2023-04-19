/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.gui.vr.FixedScreenViewport;

public class FixedStage extends Stage {

    private final int width;
    private final int height;

    public FixedStage(int width, int height) {
        this(new FixedScreenViewport(width, height), width, height);
    }

    public FixedStage(Viewport vp, int width, int height) {
        super(vp);
        this.width = width;
        this.height = height;
    }

    public FixedStage(Viewport vp, SpriteBatch batch, int width, int height) {
        super(vp, batch);
        this.width = width;
        this.height = height;
    }

    @Override
    public Vector2 stageToScreenCoordinates (Vector2 stageCoords) {
        getViewport().project(stageCoords);
        stageCoords.y = height - stageCoords.y;
        return stageCoords;
    }

    @Override
    protected boolean isInsideViewport (int screenX, int screenY) {
        Viewport viewport = getViewport();
        int x0 = viewport.getScreenX();
        int x1 = x0 + viewport.getScreenWidth();
        int y0 = viewport.getScreenY();
        int y1 = y0 + viewport.getScreenHeight();
        screenY = height - 1 - screenY;
        return screenX >= x0 && screenX < x1 && screenY >= y0 && screenY < y1;
    }
}
