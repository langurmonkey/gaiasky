/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Null;

public class OwnSelectBox<T> extends SelectBox<T> {

    private float ownWidth = 0f, ownHeight = 0f;

    public OwnSelectBox(OwnSelectBoxStyle style) {
        super(style);
    }

    public OwnSelectBox(Skin skin, String styleName) {
        super(skin.get(styleName, OwnSelectBoxStyle.class));
    }

    public OwnSelectBox(Skin skin) {
        super(skin.get(OwnSelectBoxStyle.class));
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

    protected @Null Drawable getBackgroundDrawable() {
        Drawable bg = super.getBackgroundDrawable();
        if (hasKeyboardFocus() && !isDisabled()) {
            bg = ((OwnSelectBoxStyle) getStyle()).backgroundFocused;
        }
        return bg;
    }

    static public class OwnSelectBoxStyle extends SelectBoxStyle {

        public @Null Drawable backgroundFocused;

        public OwnSelectBoxStyle() {
        }

        public OwnSelectBoxStyle(BitmapFont font, Color fontColor, @Null Drawable background, ScrollPaneStyle scrollStyle,
                ListStyle listStyle) {
            super(font, fontColor, background, scrollStyle, listStyle);
        }

        public OwnSelectBoxStyle(OwnSelectBoxStyle style) {
            super(style);
            backgroundFocused = style.backgroundFocused;
        }

    }

}
