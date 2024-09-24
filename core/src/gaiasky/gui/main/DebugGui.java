/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.gui.iface.DebugInterface;

public class DebugGui extends AbstractGui {
    protected DebugInterface debugInterface;
    private Container<Actor> di;

    public DebugGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel) {
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
        float pad = 16f;

        // DEBUG INFO - TOP RIGHT
        debugInterface = new DebugInterface(skin, lock);
        debugInterface.right().top();
        di = new Container<>(debugInterface);
        di.setFillParent(true);
        di.right().top();
        di.pad(pad, 0, 0, pad);

        rebuildGui();
    }

    @Override
    protected void rebuildGui() {
        if (stage != null) {
            stage.clear();
            if (debugInterface != null && di != null)
                stage.addActor(di);
        }
    }

    @Override
    public boolean cancelTouchFocus() {
        return false;
    }

}
