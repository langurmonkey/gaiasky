/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

public class TextureWidget extends Widget {

    private final FrameBuffer fb;
    private final float width;
    private final float height;

    public TextureWidget(FrameBuffer fb) {
        super();
        this.fb = fb;
        this.width = fb.getWidth();
        this.height = fb.getHeight();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (fb != null) {
            batch.draw(fb.getColorBufferTexture(), getX(), getY(), width, height);
        }
    }

    @Override
    public float getMinWidth() { //-V6032
        return width;
    }

    @Override
    public float getMinHeight() { //-V6032
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
