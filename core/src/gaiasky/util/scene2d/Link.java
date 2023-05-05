/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Link extends Label {

    private String linkURL;

    public Link(CharSequence text, LabelStyle style, String linkURL) {
        super(text, style);
        this.linkURL = linkURL;
        initialize();
    }

    public Link(CharSequence text, Skin skin, String fontName, Color color, String linkto) {
        super(text, skin, fontName, color);
        this.linkURL = linkto;
        initialize();
    }

    public Link(CharSequence text, Skin skin, String fontName, String colorName, String linkto) {
        super(text, skin, fontName, colorName);
        this.linkURL = linkto;
        initialize();
    }

    public Link(CharSequence text, Skin skin, String styleName, String linkto) {
        super(text, skin, styleName);
        this.linkURL = linkto;
        initialize();
    }

    public Link(CharSequence text, Skin skin, String linkto) {
        super(text, skin, "link");
        this.linkURL = linkto;
        initialize();
    }

    private void initialize() {
        // Fix touchUp issue
        this.addListener(new ClickListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return super.touchDown(event, x, y, pointer, button);
            }

            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Buttons.LEFT && linkURL != null && !linkURL.isEmpty())
                    Gdx.net.openURI(linkURL);

                // Bubble up
                super.touchUp(event, x, y, pointer, button);
            }
        });
        this.addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                if (type == Type.enter) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });
    }

    public String getLinkURL() {
        return linkURL;
    }

    public void setLinkURL(String linkURL) {
        this.linkURL = linkURL;
    }

}
