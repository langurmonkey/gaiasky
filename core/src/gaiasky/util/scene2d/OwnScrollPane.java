/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import gaiasky.util.GuiUtils;

public class OwnScrollPane extends ScrollPane {
    private float ownWidth = 0f, ownHeight = 0f;
    // Whether to expand pane if possible
    private boolean expand = false;
    // Whether to bubble events
    private boolean bubbles = true;

    /**
     * @param widget May be null.
     */
    public OwnScrollPane(Actor widget, Skin skin) {
        this(widget, skin.get(ScrollPaneStyle.class));
    }

    /**
     * @param widget May be null.
     */
    public OwnScrollPane(Actor widget, Skin skin, String styleName) {
        this(widget, skin.get(styleName, ScrollPaneStyle.class));
    }

    /**
     * @param widget May be null.
     */
    public OwnScrollPane(Actor widget, ScrollPaneStyle style) {
        super(widget, style);
        //Remove capture listeners
        for (EventListener cl : getListeners()) {
            if (cl instanceof ActorGestureListener) {
                removeListener(cl);
            }
        }

        // Focus listener.
        setFocusModeListener();
    }

    private void setFocusModeListener() {
        // Focus listener.
        addListener((e) -> {
            if (e instanceof InputEvent) {
                InputEvent ie = (InputEvent) e;
                e.setBubbles(bubbles);
                if (ie.getType() == InputEvent.Type.enter && this.getStage() != null) {
                    return this.getStage().setScrollFocus(this);
                }
            }
            return false;
        });
    }

    public void setExpand(boolean expand) {
        this.expand = expand;
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
            if (expand && getActor() instanceof Layout)
                return Math.max(ownWidth, super.getPrefWidth());
            else
                return ownWidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public void layout() {
        super.layout();
        this.bubbles = !isScrollX() && !isScrollY();
    }

    @Override
    public float getPrefHeight() {
        if (ownHeight != 0) {
            if (expand && getActor() instanceof Layout)
                return Math.max(ownHeight, super.getPrefHeight());
            else
                return ownHeight;
        } else {
            return super.getPrefHeight();
        }
    }

    /**
     * Returns the amount to scroll vertically when the mouse wheel is scrolled.
     */
    protected float getMouseWheelY() {
        return 30;
    }

}
