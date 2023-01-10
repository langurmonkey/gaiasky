/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

/**
 * A UI widget that shows the contents of a frame buffer.
 */
public class FrameBufferWidget extends Widget {

    private final FrameBuffer fb;
    private final float width;
    private final float height;

    private boolean flipX, flipY;

    public FrameBufferWidget(FrameBuffer fb) {
        super();
        this.fb = fb;
        this.width = fb.getWidth();
        this.height = fb.getHeight();
    }

    public void setFlip(boolean x, boolean y) {
        this.flipX = x;
        this.flipY = y;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (fb != null) {
            if (flipX || flipY) {
                batch.draw(fb.getColorBufferTexture(), getX(), getY(), 0F, 0F, width, height, 1F, 1F, 0F, 0, 0, (int) width, (int) height, flipX, flipY);
            } else {
                batch.draw(fb.getColorBufferTexture(), getX(), getY(), width, height);
            }
        }
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
