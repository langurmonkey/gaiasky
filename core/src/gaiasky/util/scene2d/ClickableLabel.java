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
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * A simple label that executes a {@link Runnable} when it is clicked.
 */
public class ClickableLabel extends OwnLabel {

    private Runnable clickAction;

    public ClickableLabel(CharSequence text,
                          LabelStyle style,
                          Runnable clickAction) {
        super(text, style);
        this.clickAction = clickAction;
        initialize();
    }

    public ClickableLabel(CharSequence text,
                          Skin skin,
                          String fontName,
                          Color color,
                          Runnable clickAction) {
        super(text, skin, fontName, color);
        this.clickAction = clickAction;
        initialize();
    }

    public ClickableLabel(CharSequence text,
                          Skin skin,
                          String fontName,
                          String colorName,
                          Runnable clickAction) {
        super(text, skin, fontName, colorName);
        this.clickAction = clickAction;
        initialize();
    }

    public ClickableLabel(CharSequence text,
                          Skin skin,
                          String styleName,
                          Runnable clickAction) {
        super(text, skin, styleName);
        this.clickAction = clickAction;
        initialize();
    }

    public ClickableLabel(CharSequence text,
                          Skin skin,
                          Runnable clickAction) {
        super(text, skin, "link");
        this.clickAction = clickAction;
        initialize();
    }

    private void initialize() {
        // Fix touchUp issue
        this.addListener(new ClickListener() {
            public boolean touchDown(InputEvent event,
                                     float x,
                                     float y,
                                     int pointer,
                                     int button) {
                return super.touchDown(event, x, y, pointer, button);
            }

            public void touchUp(InputEvent event,
                                float x,
                                float y,
                                int pointer,
                                int button) {
                if (button == Buttons.LEFT && clickAction != null) {
                    clickAction.run();
                }

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

    public void setClickAction(Runnable clickAction) {
        this.clickAction = clickAction;
    }

}
