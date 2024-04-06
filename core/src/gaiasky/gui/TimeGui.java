/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class TimeGui extends AbstractGui {
    private TimeGuiInterface time;
    private Container<Actor> t;

    public TimeGui(Skin skin, Graphics graphics, Float unitsPerPixel) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        this.stage = new Stage(vp, sb);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        float pad = 26f;

        // Time - BOTTOM RIGHT
        time = new TimeGuiInterface(skin);
        time.right().bottom();
        t = new Container<>(time);
        t.setFillParent(true);
        t.right().bottom();
        t.pad(0, 0, pad, pad);

        rebuildGui();
    }

    @Override
    protected void rebuildGui() {
        if (stage != null) {
            stage.clear();
            if (time != null && t != null)
                stage.addActor(t);
        }
    }

    @Override
    public boolean cancelTouchFocus() {
        return false;
    }
}
