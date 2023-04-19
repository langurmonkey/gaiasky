/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class Separator extends Widget {
    private final SeparatorStyle style;

    /** New separator with default style */
    public Separator(Skin skin) {
        style = skin.get(SeparatorStyle.class);
    }

    public Separator(Skin skin, String styleName) {
        style = skin.get(styleName, SeparatorStyle.class);
    }

    public Separator(SeparatorStyle style) {
        this.style = style;
    }

    @Override
    public float getPrefHeight() { //-V6032
        return style.thickness;
    }

    @Override
    public float getPrefWidth() {
        return style.thickness;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color c = getColor();
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
        style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
    }

    public SeparatorStyle getStyle() {
        return style;
    }

    static public class SeparatorStyle {
        public Drawable background;
        public int thickness;

        public SeparatorStyle() {
        }

        public SeparatorStyle(SeparatorStyle style) {
            this.background = style.background;
            this.thickness = style.thickness;
        }

        public SeparatorStyle(Drawable bg, int thickness) {
            this.background = bg;
            this.thickness = thickness;
        }
    }
}
