/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class OwnCheckBox extends CheckBox {
    private final Color regularColor;
    private float ownwidth = 0f, ownheight = 0f;

    public OwnCheckBox(String text, Skin skin) {
        super(text, skin);
        this.regularColor = getLabel().getColor().cpy();
        this.getCells().get(0);
    }

    public OwnCheckBox(String text, Skin skin, float space) {
        super(text, skin);
        this.regularColor = getLabel().getColor().cpy();
        this.getCells().get(0).padRight(space);
    }

    public OwnCheckBox(String text, Skin skin, String styleName, float space) {
        super(text, skin, styleName);
        this.regularColor = getLabel().getColor().cpy();
        this.getCells().get(0).padRight(space);
    }

    @Override
    public void setDisabled(boolean isDisabled) {
        super.setDisabled(isDisabled);

        if (isDisabled) {
            getLabel().setColor(Color.GRAY);
        } else {
            getLabel().setColor(regularColor);
        }
    }
    @Override
    public void setWidth(float width) {
        ownwidth = width;
        super.setWidth(width);
    }

    public void setMinWidth(float width) {
        this.setWidth(Math.max(width, getWidth()));
    }

    @Override
    public void setHeight(float height) {
        ownheight = height;
        super.setHeight(height);
    }

    public void setMinHeight(float height) {
        this.setHeight(Math.max(height, getHeight()));
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
