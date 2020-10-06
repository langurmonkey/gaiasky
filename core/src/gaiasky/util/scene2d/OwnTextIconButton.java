/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import gaiasky.util.GlobalConf;

/**
 * OwnTextButton with an icon. Also, the cursor changes when the mouse rolls
 * over. It also fixes the size issue.
 *
 * @author Toni Sagrista
 */
public class OwnTextIconButton extends OwnTextButton {

    private Skin skin;
    private Image icon;
    private TextIconButtonStyle style;
    private float pad = 2f;
    private int align = Align.left;

    public OwnTextIconButton(String text, Skin skin, String styleName) {
        super(text, skin);
        this.skin = skin;
        setStyle(skin.get(styleName, TextIconButtonStyle.class), "default");
    }

    public OwnTextIconButton(String text, int align, Skin skin, String styleName) {
        super(text, skin);
        this.skin = skin;
        this.align = align;
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
        this.align = align;
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

    public void setPad(float pad) {
        this.pad = pad;
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

    protected void updateImage() {
        if (style != null) {
            Drawable drawable = style.imageUp;
            if (isChecked() && style.imageDown != null)
                drawable = style.imageDown;
            icon.setDrawable(drawable);
        }
    }

    public void setIcon(Image icon) {
        this.icon = icon;
        clearChildren();
        if (Align.isRight(align)) {
            this.align(align);
            add(getLabel()).align(align).padRight((getLabel().getText().length > 0 ? 8f : 1f) * GlobalConf.UI_SCALE_FACTOR);
            add(this.icon).align(align).pad(pad).padRight(pad);
        } else {
            this.align(align);
            add(this.icon).align(align).pad(pad).padRight((getLabel().getText().length > 0 ? 8f : 1f) * GlobalConf.UI_SCALE_FACTOR);
            add(getLabel()).align(align).padRight(pad);
        }
    }

    public void draw(Batch batch, float parentAlpha) {
        updateImage();
        super.draw(batch, parentAlpha);
    }

    static public class TextIconButtonStyle extends TextButtonStyle {
        Drawable imageUp, imageDown;

        public TextIconButtonStyle() {
        }

        public TextIconButtonStyle(TextButtonStyle def, TextIconButtonStyle style) {
            super(def);
            Drawable imageUp = style.imageUp;
            Drawable imageDown = style.imageDown;
            this.imageUp = imageUp;
            this.imageDown = imageDown;
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
