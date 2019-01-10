package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;

public class OwnLabel extends Label implements Disableable {
    private float ownwidth = 0f, ownheight = 0f;
    private Color regularColor;
    private boolean disabled = false;

    public OwnLabel(CharSequence text, Skin skin) {
        super(text, skin);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, LabelStyle style) {
        super(text, style);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, Skin skin, String fontName, Color color) {
        super(text, skin, fontName, color);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, Skin skin, String fontName, String colorName) {
        super(text, skin, fontName, colorName);
        this.regularColor = this.getColor().cpy();
    }

    public OwnLabel(CharSequence text, Skin skin, String styleName) {
        super(text, skin, styleName);
        this.regularColor = this.getColor().cpy();
    }

    public void receiveScrollEvents(){
            // Focus listener
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

    @Override
    public void setDisabled(boolean isDisabled) {
        if (isDisabled) {
            disabled = true;
            this.setColor(Color.GRAY);
        } else {
            disabled = false;
            this.setColor(regularColor);
        }
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

}
