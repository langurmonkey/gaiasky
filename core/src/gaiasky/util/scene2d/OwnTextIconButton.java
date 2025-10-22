/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;

public class OwnTextIconButton extends OwnTextButton {

    private final Skin skin;
    private Image icon;
    private TextIconButtonStyle style;
    private float pad = 2f;
    private float space = -1;
    private int contentAlign = Align.center;

    public OwnTextIconButton(String text, Skin skin, String styleName) {
        super(text, skin);
        this.skin = skin;
        setStyle(skin.get(styleName, TextIconButtonStyle.class), "default");
    }

    public OwnTextIconButton(String text, int contentAlign, Skin skin, String styleName) {
        super(text, skin);
        this.skin = skin;
        this.contentAlign = contentAlign;
        setStyle(skin.get(styleName, TextIconButtonStyle.class), "default");
    }

    public OwnTextIconButton(String text, Skin skin, String styleName, String textButtonStyle) {
        super(text, skin);
        this.skin = skin;
        setStyle(skin.get(styleName, TextIconButtonStyle.class), textButtonStyle);
    }

    public OwnTextIconButton(String text, int align, Skin skin, String styleName, String textButtonStyle) {
        super(text, skin);
        this.skin = skin;
        this.contentAlign = align;
        setStyle(skin.get(styleName, TextIconButtonStyle.class), textButtonStyle);
    }

    public OwnTextIconButton(String text, Image up, Skin skin) {
        super(text, skin);
        this.skin = skin;
        setIcon(up);
    }

    public OwnTextIconButton(String text, Image up, Skin skin, String styleName) {
        super(text, skin, styleName);
        this.skin = skin;
        setIcon(up);
    }
    public OwnTextIconButton(String text, int contentAlign, Image up, Skin skin, String styleName) {
        super(text, skin, styleName);
        this.skin = skin;
        this.contentAlign = contentAlign;
        setIcon(up);
    }

    public void setContentAlign(int align) {
        this.contentAlign = align;
    }

    public void setPad(float pad) {
        this.pad = pad;
        setIcon(this.icon);
    }

    public void setSpace(float space) {
        this.space = space;
        setIcon(this.icon);
    }

    public void setStyle(TextButtonStyle style, String defaultTextButtonStyle) {
        if (!(style instanceof TextIconButtonStyle))
            throw new IllegalArgumentException("style must be an ImageButtonStyle.");

        // Check default style
        if (style.font == null || style.fontColor == null) {
            TextButtonStyle toggle = skin.get(defaultTextButtonStyle, TextButtonStyle.class);
            // Overwrite style
            style = new TextIconButtonStyle(toggle, (TextIconButtonStyle) style);
        }

        super.setStyle(style);
        this.style = (TextIconButtonStyle) style;
        setIcon(new Image(((TextIconButtonStyle) style).imageUp));
        if (icon != null)
            updateImage();
    }

    @Override
    public void setDisabled(boolean isDisabled) {
        super.setDisabled(isDisabled);
        if (isDisabled) {
            if (this.style != null && this.style.disabledFontColor != null) {
                this.icon.setColor(this.style.disabledFontColor);
                this.setIcon(this.icon);
            }
        } else {
            if (this.style != null && this.style.fontColor != null) {
                this.icon.setColor(this.style.fontColor);
                this.setIcon(this.icon);
            }
        }

    }

    protected void updateImage() {
        if (style != null) {
            Drawable drawable = style.imageUp;
            if (isChecked() && style.imageDown != null)
                drawable = style.imageDown;
            else if (isOver() && style.imageOver != null)
                drawable = style.imageOver;
            icon.setDrawable(drawable);
        }
    }

    public void setIcon(Image icon) {
        this.icon = icon;
        clearChildren();
        if (Align.isRight(contentAlign)) {
            this.align(contentAlign);
            add(getLabel()).align(contentAlign).padRight(space <= 0 ? (getLabel().getText().size > 0 ? 12.8f : 1f) : space);
            add(this.icon).align(contentAlign).pad(pad);
        } else {
            this.align(contentAlign);
            add(this.icon).align(contentAlign).pad(pad).padRight(space <= 0 ? (getLabel().getText().size > 0 ? 12.8f : 1f) : space);
            add(getLabel()).align(contentAlign).padRight(pad);
        }
    }

    public void draw(Batch batch, float parentAlpha) {
        updateImage();
        super.draw(batch, parentAlpha);
    }

    static public class TextIconButtonStyle extends TextButtonStyle {
        Drawable imageUp, imageDown, imageOver;

        public TextIconButtonStyle() {
        }

        public TextIconButtonStyle(TextButtonStyle def, TextIconButtonStyle style) {
            super(def);
            Drawable imageUp = style.imageUp;
            Drawable imageDown = style.imageDown;
            Drawable imageHover = style.imageOver;
            this.imageUp = imageUp;
            this.imageDown = imageDown;
            this.imageOver = imageHover;
            if (style.up != null)
                this.up = style.up;
            if (style.down != null)
                this.down = style.down;
            if (style.font != null)
                this.font = style.font;
            if (style.fontColor != null)
                this.fontColor = style.fontColor;
            if (style.downFontColor != null)
                this.downFontColor = style.downFontColor;
            if (style.focused != null)
                this.focused = style.focused;
            if (style.checked != null)
                this.checked = style.checked;
            if (style.checkedFocused != null)
                this.checkedFocused = style.checkedFocused;
        }
    }
}
