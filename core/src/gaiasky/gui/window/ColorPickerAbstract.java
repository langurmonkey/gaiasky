/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public abstract class ColorPickerAbstract extends Image {
    protected Stage stage;
    protected Skin skin;
    protected Runnable newColorRunnable, newColormapRunnable;
    protected float[] color;
    protected String name;

    protected ColorPickerAbstract(String name, Stage stage, Skin skin) {
        super(skin.getDrawable("white"));
        this.name = name;
        this.skin = skin;
        this.stage = stage;
    }

    protected abstract void initialize();

    protected void initColor() {
        if (color == null || color.length != 4) {
            color = new float[4];
        }
    }

    public float[] getPickedColor() {
        return color;
    }

    public void setPickedColor(float[] color) {
        initColor();
        if (color.length >= 4) {
            System.arraycopy(color, 0, this.color, 0, color.length);
            super.setColor(color[0], color[1], color[2], color[3]);
        } else if (color.length == 3) {
            System.arraycopy(color, 0, this.color, 0, color.length);
            this.color[3] = 1f;
            super.setColor(color[0], color[1], color[2], 1f);
        }
    }

    public double[] getPickedColorDouble() {
        double[] c = new double[color.length];
        for (int i = 0; i < color.length; i++)
            c[i] = color[i];
        return c;
    }

    public void setPickedColor(float r, float g, float b, float a) {
        initColor();
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
        super.setColor(r, g, b, a);
    }
}
