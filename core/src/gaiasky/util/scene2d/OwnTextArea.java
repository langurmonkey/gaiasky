/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;

public class OwnTextArea extends TextArea {
    private float ownWidth = 0f, ownHeight = 0f;

    public OwnTextArea(String text, Skin skin) {
        super(text, skin);
    }

    public OwnTextArea(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
    }

    public OwnTextArea(String text, TextFieldStyle style) {
        super(text, style);
    }

    @Override
    public void setWidth(float width) {
        ownWidth = width;
        super.setWidth(width);
    }

    @Override
    public void setHeight(float height) {
        ownHeight = height;
        super.setHeight(height);
    }

    @Override
    public void setSize(float width, float height) {
        ownWidth = width;
        ownHeight = height;
        super.setSize(width, height);
    }

    @Override
    public float getPrefWidth() {
        if (ownWidth != 0) {
            return ownWidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight() {
        if (ownHeight != 0) {
            return ownHeight;
        } else {
            return super.getPrefHeight();
        }
    }

    public void offsets() {
        super.calculateOffsets();
    }
}
