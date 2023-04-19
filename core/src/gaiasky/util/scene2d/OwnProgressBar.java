/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class OwnProgressBar extends ProgressBar {

    private final boolean vertical;
    private float prefWidth = 0;
    private float prefHeight = 0;
    private OwnLabel titleLabel;

    public OwnProgressBar(float min, float max, float stepSize, boolean vertical, ProgressBarStyle style) {
        super(min, max, stepSize, vertical, style);
        this.vertical = vertical;
    }

    public OwnProgressBar(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        super(min, max, stepSize, vertical, skin, styleName);
        this.vertical = vertical;
    }

    public OwnProgressBar(float min, float max, float stepSize, boolean vertical, Skin skin) {
        super(min, max, stepSize, vertical, skin);
        this.vertical = vertical;
    }

    public void setTitle(String title, Skin skin) {
        this.titleLabel = new OwnLabel(title, skin, "small");
    }

    public float getPrefWidth() {
        if (!vertical && prefWidth > 0) {
            return prefWidth;
        } else {
            return super.getPrefWidth();
        }

    }

    public void setPrefWidth(float prefWidth) {
        this.prefWidth = prefWidth;
    }

    public float getPrefHeight() {
        if (vertical && prefHeight > 0) {
            return prefHeight;
        } else {
            return super.getPrefHeight();
        }
    }

    public void setPrefHeight(float prefHeight) {
        this.prefHeight = prefHeight;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (titleLabel != null) {
            float padY = 2f;
            float padX = 10.8f;
            titleLabel.setPosition(getX() + padX, getY() + getHeight() - titleLabel.getHeight() - padY);
            titleLabel.draw(batch, parentAlpha);
        }
    }
}
