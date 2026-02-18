/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * An image that keeps track of the given width and height.
 * It also can, optionally, include a hyperlink.
 */
public class OwnImage extends Image {
    private float ownWidth = 0f, ownHeight = 0f;
    private String linkURL;
    private boolean applyAccentColor = true;

    /**
     * Create an image.
     *
     * @param drawable The drawable.
     */
    public OwnImage(Drawable drawable) {
        this(drawable, null);
    }

    public OwnImage(Drawable drawable, boolean applyAccentColor) {
        this(drawable, null);
        this.applyAccentColor = applyAccentColor;
    }

    /**
     * Create an image.
     *
     * @param tex The texture.
     * @param applyAccentColor Whether to apply the accent color for this image.
     */
    public OwnImage(Texture tex, boolean applyAccentColor) {
        super(tex);
        this.applyAccentColor = applyAccentColor;
    }

    /**
     * Creates a new image with a link.
     *
     * @param drawable The drawable.
     * @param url      The URL of the link.
     */
    public OwnImage(Drawable drawable, String url) {
        super(drawable);
        if (url != null && !url.isBlank()) {
            this.linkURL = url;
            initialize();
        }
    }

    private void initialize() {
        // Fix touchUp issue
        this.addListener(new ClickListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return super.touchDown(event, x, y, pointer, button);
            }

            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.LEFT && linkURL != null && !linkURL.isEmpty())
                    Gdx.net.openURI(linkURL);

                // Bubble up
                super.touchUp(event, x, y, pointer, button);
            }
        });
        this.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent.Type type = ((InputEvent) event).getType();
                if (type == InputEvent.Type.enter) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
                } else if (type == InputEvent.Type.exit) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });
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

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!applyAccentColor) {
           batch.getShader().setUniformi("u_applyAccent", 0);
        }
        super.draw(batch, parentAlpha);
        if (!applyAccentColor) {
            batch.flush();
            batch.getShader().setUniformi("u_applyAccent", 1);
        }
    }
}
