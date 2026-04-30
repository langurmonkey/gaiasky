/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.Objects;

public class OwnImageButton extends ImageButton implements ProgrammaticButton {
    private final OwnImageButton me;
    private SystemCursor cursor;
    private float ownWidth = 0f, ownHeight = 0f;
    private final Color baseColor = new Color(1, 1, 1, 1);

    public OwnImageButton(Skin skin) {
        super(skin);
        this.me = this;
        initialize();
    }

    public OwnImageButton(Skin skin, String styleName) {
        super(skin, styleName);
        this.me = this;
        initialize();
    }

    public OwnImageButton(ImageButtonStyle style) {
        super(style);
        this.me = this;
        initialize();
    }

    public void setCheckedNoFire(boolean isChecked) {
        this.setProgrammaticChangeEvents(false);
        this.setChecked(isChecked);
        this.setProgrammaticChangeEvents(true);
    }

    private void initialize() {
        baseColor.set(getColor());
        cursor = SystemCursor.Hand;
        this.addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                if (type == Type.enter) {
                    if (!me.isDisabled())
                        Gdx.graphics.setSystemCursor(cursor);
                    return true;
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                    return true;
                }
            }
            return false;
        });
    }

    protected void updateImage() {
        getImage().setDrawable(getImageDrawable());
        Color theme;
        try {
            theme = getSkin().getColor("highlight");
        } catch (Exception e) {
            theme = null;
        }

        var c = baseColor != null ? baseColor : Color.WHITE;
        if (isOver()) {
            getImage().setColor(Objects.requireNonNullElse(theme, c));
        } else {
            getImage().setColor(c);
        }
    }

    public void draw(Batch batch, float parentAlpha) {
        updateImage();
        super.draw(batch, parentAlpha);
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

    @Override
    public void setColor(Color color) {
        super.setColor(color);
        baseColor.set(color);
        if (getImage() != null)
            getImage().setColor(color);
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        super.setColor(r, g, b, a);
        baseColor.set(r, g, b, a);
        if (getImage() != null)
            getImage().setColor(r, g, b, a);
    }

}
