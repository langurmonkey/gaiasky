package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;

/**
 * Small overriding that returns the user set size as preferred size.
 * @author Toni Sagrista
 *
 */
public class OwnScrollPane extends ScrollPane {
    private float ownwidth = 0f, ownheight = 0f;

    /** @param widget May be null. */
    public OwnScrollPane(Actor widget, Skin skin) {
        this(widget, skin.get(ScrollPaneStyle.class));
    }

    /** @param widget May be null. */
    public OwnScrollPane(Actor widget, Skin skin, String styleName) {
        this(widget, skin.get(styleName, ScrollPaneStyle.class));
    }

    /** @param widget May be null. */
    public OwnScrollPane(Actor widget, ScrollPaneStyle style) {
        super(widget, style);
        //Remove capture listeners
        for (EventListener cl : getListeners()) {
            if (cl instanceof ActorGestureListener) {
                removeListener(cl);
            }
        }
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

    /** Returns the amount to scroll vertically when the mouse wheel is scrolled. */
    protected float getMouseWheelY() {
        return 30;
    }

}
