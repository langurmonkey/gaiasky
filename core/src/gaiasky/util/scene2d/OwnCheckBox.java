/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;

public class OwnCheckBox extends CheckBox {
    private final Color regularColor;
    private float ownWidth = 0f, ownHeight = 0f;

    public OwnCheckBox(String text, Skin skin) {
        super(text, skin);
        this.regularColor = getLabel().getColor().cpy();
        this.getCells().get(0);
    }

    public OwnCheckBox(String text, Skin skin, float space) {
        super(text, skin);
        this.regularColor = getLabel().getColor().cpy();
        this.getCells().get(0).padRight(space);
    }

    public OwnCheckBox(String text, Skin skin, String styleName, float space) {
        super(text, skin, styleName);
        this.regularColor = getLabel().getColor().cpy();
        this.getCells().get(0).padRight(space);
    }

    @Override
    public void setDisabled(boolean isDisabled) {
        super.setDisabled(isDisabled);

        if (isDisabled) {
            getLabel().setColor(Color.GRAY);
        } else {
            getLabel().setColor(regularColor);
        }
    }

    @Override
    public void setWidth(float width) {
        ownWidth = width;
        super.setWidth(width);
    }

    public void setMinWidth(float width) {
        this.setWidth(Math.max(width, getWidth()));
    }

    @Override
    public void setHeight(float height) {
        ownHeight = height;
        super.setHeight(height);
    }

    public void setMinHeight(float height) {
        this.setHeight(Math.max(height, getHeight()));
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

    /**
     * Fix focused check boxes.
     *
     * @param batch       The batch.
     * @param parentAlpha The parent alpha, to be multiplied with this actor's alpha, allowing the parent's alpha to affect all
     *                    children.
     */
    public void draw(Batch batch, float parentAlpha) {
        Drawable checkbox = null;
        if (isDisabled()) {
            if (isChecked() && getStyle().checkboxOnDisabled != null)
                checkbox = getStyle().checkboxOnDisabled;
            else
                checkbox = getStyle().checkboxOffDisabled;
        }
        if (checkbox == null) {
            boolean over = isOver() && !isDisabled();
            boolean focused = hasKeyboardFocus() && !isDisabled();
            if (isChecked() && getStyle().checkboxOn != null)
                if (focused && getStyle().checkedFocused != null)
                    checkbox = getStyle().checkedFocused;
                else
                    checkbox = over && getStyle().checkboxOnOver != null ? getStyle().checkboxOnOver : getStyle().checkboxOn;
            else if (over && getStyle().checkboxOver != null)
                checkbox = getStyle().checkboxOver;
            else if (focused && getStyle().focused != null)
                checkbox = getStyle().focused;
            else
                checkbox = getStyle().checkboxOff;
        }
        getImage().setDrawable(checkbox);

        // From TextButton
        getLabel().getStyle().fontColor = getFontColor();

        // From Button
        validate();

        float offsetX = 0, offsetY = 0;
        if (isPressed() && !isDisabled()) {
            offsetX = getStyle().pressedOffsetX;
            offsetY = getStyle().pressedOffsetY;
        } else if (isChecked() && !isDisabled()) {
            offsetX = getStyle().checkedOffsetX;
            offsetY = getStyle().checkedOffsetY;
        } else {
            offsetX = getStyle().unpressedOffsetX;
            offsetY = getStyle().unpressedOffsetY;
        }
        boolean offset = offsetX != 0 || offsetY != 0;

        Array<Actor> children = getChildren();
        if (offset) {
            for (int i = 0; i < children.size; i++)
                children.get(i).moveBy(offsetX, offsetY);
        }

        // From Table
        validate();
        if (isTransform()) {
            applyTransform(batch, computeTransform());
            if (getClip()) {
                batch.flush();
                float padLeft = this.getPadLeftValue().get(this), padBottom = this.getPadBottomValue().get(this);
                if (clipBegin(padLeft, padBottom, getWidth() - padLeft - getPadBottomValue().get(this),
                        getHeight() - padBottom - getPadTopValue().get(this))) {
                    drawChildren(batch, parentAlpha);
                    batch.flush();
                    clipEnd();
                }
            } else
                drawChildren(batch, parentAlpha);
            resetTransform(batch);
        } else {
            // From WidgetGroup
            validate();
            // From Group
            if (isTransform()) applyTransform(batch, computeTransform());
            drawChildren(batch, parentAlpha);
            if (isTransform()) resetTransform(batch);
            // End Group
            // End WidgetGroup
        }
        // End Table


        if (offset) {
            for (int i = 0; i < children.size; i++)
                children.get(i).moveBy(-offsetX, -offsetY);
        }

        Stage stage = getStage();
        if (stage != null && stage.getActionsRequestRendering() && isPressed() != getClickListener().isPressed())
            Gdx.graphics.requestRendering();
        // End Button
        // End TextButton
    }
}
