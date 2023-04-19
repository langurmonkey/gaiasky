/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.Align;

public abstract class GuiComponent {

    protected Actor component;
    protected Skin skin;
    protected Stage stage;

    protected float pad20, pad12, pad9, pad8, pad6, pad4, pad3, pad1;

    public GuiComponent(Skin skin, Stage stage) {
        this.skin = skin;
        this.stage = stage;
        pad8 = 8f;
        pad20 = 20.0f;
        pad12 = 12.8f;
        pad9 = 9.6f;
        pad6 = 6.4f;
        pad4 = 4.8f;
        pad3 = 3.2f;
        pad1 = 1.6f;
    }

    /**
     * Initialises the component.
     */
    public abstract void initialize();

    public Actor getActor() {
        return component;
    }

    /**
     * Disposes the component.
     */
    public abstract void dispose();

    protected VerticalGroup group(Actor ac1, Actor ac2, float sp) {
        VerticalGroup vg = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        vg.space(sp);
        vg.addActor(ac1);
        vg.addActor(ac2);
        return vg;
    }
}
