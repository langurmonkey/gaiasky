package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class OwnImage extends Image {
    private float ownwidth = 0f, ownheight = 0f;

    public OwnImage(Drawable drawable) {
        super(drawable);
    }

    @Override
    public void setWidth(float width) {
        ownwidth = width;
        super.setWidth(width);
    }

    @Override
    public void setHeight(float height) {
        ownheight = height;
        super.setHeight(height);
    }

    @Override
    public void setSize(float width, float height) {
        ownwidth = width;
        ownheight = height;
        super.setSize(width, height);
    }

    @Override
    public float getPrefWidth() {
        if (ownwidth != 0) {
            return ownwidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight() {
        if (ownheight != 0) {
            return ownheight;
        } else {
            return super.getPrefHeight();
        }
    }

}
