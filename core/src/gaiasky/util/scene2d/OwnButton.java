/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class OwnButton extends Button {
    OwnButton me;
    SystemCursor cursor;
    private boolean changeCursor = true;

    public OwnButton(Skin skin) {
        super(skin);
        this.me = this;
        initialize();
    }

    public OwnButton(Skin skin, String styleName) {
        super(skin, styleName);
        this.me = this;
        initialize();
    }

    public OwnButton(Skin skin, String styleName, boolean changeCursor) {
        super(skin, styleName);
        this.me = this;
        this.changeCursor = changeCursor;
        initialize();
    }

    public OwnButton(Actor child, Skin skin, String styleName, boolean changeCursor) {
        super(child, skin, styleName);
        this.me = this;
        this.changeCursor = changeCursor;
        initialize();
    }

    public void setCheckedNoFire(boolean isChecked) {
        this.setProgrammaticChangeEvents(false);
        this.setChecked(isChecked);
        this.setProgrammaticChangeEvents(true);
    }

    private void initialize() {
        cursor = SystemCursor.Hand;
        this.addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                if (changeCursor && type == Type.enter) {
                    if (!me.isDisabled())
                        Gdx.graphics.setSystemCursor(cursor);
                    return true;
                } else if (changeCursor && type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                    return true;
                } else if (type == Type.touchDown) {
                    // Focus
                    this.getStage().setKeyboardFocus(this);
                }
            }
            return false;
        });
    }
}
