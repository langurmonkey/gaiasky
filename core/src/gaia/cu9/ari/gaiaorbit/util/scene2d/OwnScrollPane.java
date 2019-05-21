/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;

/**
 * Small overriding that returns the user set size as preferred size.
 *
 * @author Toni Sagrista
 */
public class OwnScrollPane extends ScrollPane {
    private float ownwidth = 0f, ownheight = 0f;
    // Whether to expand pane if possible
    private boolean expand = false;

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

        // FOCUS_MODE listener
        addListener((e) -> {
            if (e instanceof InputEvent) {
                InputEvent ie = (InputEvent) e;
                e.setBubbles(false);
                if (ie.getType() == InputEvent.Type.enter && this.getStage() != null) {
                    return this.getStage().setScrollFocus(this);
                } else if (ie.getType() == InputEvent.Type.exit && this.getStage() != null) {
                    return this.getStage().setScrollFocus(null);
                }
            }
            return true;
        });
    }

    public void setExpand(boolean expand){
        this.expand = expand;
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
            if (expand && getActor() instanceof Layout)
                return Math.max(ownwidth, super.getPrefWidth());
            else
                return ownwidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight() {
        if (ownheight != 0) {
            if (expand && getActor() instanceof Layout)
                return Math.max(ownheight, super.getPrefHeight());
            else
                return ownheight;
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
