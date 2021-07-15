/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import gaiasky.util.GlobalResources;

/**
 * TextButton in which the cursor changes when the mouse rolls over. It also
 * fixes the size issue.
 */
public class OwnTextButton extends TextButton {

    private float ownwidth = 0f, ownheight = 0f;
    OwnTextButton me;

    public OwnTextButton(String text, Skin skin) {
        super(text, skin);
        this.me = this;
        initialize();
    }

    public OwnTextButton(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
        this.me = this;
        initialize();
    }

    public OwnTextButton(String text, TextButtonStyle style) {
        super(text, style);
        this.me = this;
        initialize();
    }

    private void initialize() {
        this.addListener((event) -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                if (type == Type.enter) {
                    if (!me.isDisabled())
                        Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }

            }
            return false;
        });
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
