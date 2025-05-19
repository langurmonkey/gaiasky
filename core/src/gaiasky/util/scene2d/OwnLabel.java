/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import gaiasky.util.TextUtils;

public class OwnLabel extends Label implements Disableable {
    private final Color regularColor;
    private float ownPrefWidth = 0f, ownPrefHeight = 0f;
    private boolean disabled = false;

    public OwnLabel(CharSequence text, Skin skin, float width) {
        super(text, skin);
        this.regularColor = this.getColor().cpy();
        this.setWidth(width);
    }

    public OwnLabel(CharSequence text, Skin skin, String styleName, int breakCharacters) {
        super(TextUtils.breakCharacters(text, breakCharacters), skin, styleName);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, Skin skin) {
        super(text, skin);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, LabelStyle style) {
        super(text, style);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, Skin skin, String fontName, Color color) {
        super(text, skin, fontName, color);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, Skin skin, String fontName, String colorName) {
        super(text, skin, fontName, colorName);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, Skin skin, String styleName) {
        super(text, skin, styleName);
        this.regularColor = this.getColor().cpy();
    }

    public void receiveScrollEvents() {
        // FOCUS_MODE listener
        addListener((e) -> {
            if (e instanceof InputEvent ie) {
                e.setBubbles(false);
                if (ie.getType() == InputEvent.Type.enter && this.getStage() != null) {
                    return this.getStage().setScrollFocus(this);
                } else if (ie.getType() == InputEvent.Type.exit && this.getStage() != null) {
                    return this.getStage().setScrollFocus(null);
                }
            }
            return true;
        });
    }

    @Override
    public void setWidth(float width) {
        ownPrefWidth = width;
        super.setWidth(width);
    }

    @Override
    public void setHeight(float height) {
        ownPrefHeight = height;
        super.setHeight(height);
    }

    @Override
    public void setSize(float width, float height) {
        ownPrefWidth = width;
        ownPrefHeight = height;
        super.setSize(width, height);
    }

    @Override
    public float getPrefWidth() {
        if (ownPrefWidth != 0) {
            return ownPrefWidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight() {
        if (ownPrefHeight != 0) {
            return ownPrefHeight;
        } else {
            return super.getPrefHeight();
        }
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(boolean isDisabled) {
        if (isDisabled) {
            disabled = true;
            this.setColor(Color.GRAY);
        } else {
            disabled = false;
            this.setColor(regularColor);
        }
    }

}
