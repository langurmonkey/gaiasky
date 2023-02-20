/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

/**
 * A UI widget that shows the contents of a frame buffer or a texture.
 */
public class TextureWidget extends Widget {

    private final Texture texture;
    private float width;
    private float height;

    private boolean flipX, flipY;
    private float scaleX = 1F, scaleY = 1F;

    public TextureWidget(FrameBuffer fb) {
        super();
        this.texture = fb.getColorBufferTexture();
        this.width = fb.getWidth();
        this.height = fb.getHeight();
    }

    public TextureWidget(Texture texture) {
        super();
        this.texture = texture;
        this.width = texture.getWidth();
        this.height = texture.getHeight();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (texture != null) {
            if (flipX || flipY || scaleX != 1F || scaleY != 1F) {
                batch.draw(texture, getX(), getY(), 0F, 0F, texture.getWidth(), texture.getHeight(), scaleX, scaleY, 0F, 0, 0, texture.getWidth(), texture.getHeight(), flipX, flipY);
            } else {
                batch.draw(texture, getX(), getY(), width, height);
            }
        }
    }

    /**
     * Flip the texture.
     *
     * @param x Flip horizontally.
     * @param y Flip vertically.
     */
    public void setFlip(boolean x, boolean y) {
        this.flipX = x;
        this.flipY = y;
    }

    /**
     * Set the scale factor of the texture.
     *
     * @param scale The scale factor in both x and y dimensions.
     */
    public void setScale(float scale) {
        this.setScale(scale, scale);
    }

    /**
     * Set the scale factor of the texture.
     *
     * @param scaleX The scale factor in x.
     * @param scaleY The scale factor in y.
     */
    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.width *= scaleX;
        this.height *= scaleY;
    }

    @Override
    public float getMinWidth() {
        return width;
    }

    @Override
    public float getMinHeight() {
        return height;
    }

    @Override
    public float getPrefWidth() {
        return width;
    }

    @Override
    public float getPrefHeight() {
        return height;
    }

    @Override
    public float getMaxWidth() {
        return width;
    }

    @Override
    public float getMaxHeight() {
        return height;
    }

}
