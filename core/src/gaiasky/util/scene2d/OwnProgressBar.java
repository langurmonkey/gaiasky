/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class OwnProgressBar extends ProgressBar {

    private float prefWidth = 0;
    private float prefHeight = 0;
    private final boolean vertical;
    private OwnLabel titleLabel;

    public OwnProgressBar(float min, float max, float stepSize, boolean vertical, ProgressBarStyle style) {
        super(min, max, stepSize, vertical, style);
        this.vertical = vertical;
    }

    public OwnProgressBar(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        super(min, max, stepSize, vertical, skin, styleName);
        this.vertical = vertical;
    }

    public OwnProgressBar(float min, float max, float stepSize, boolean vertical, Skin skin) {
        super(min, max, stepSize, vertical, skin);
        this.vertical = vertical;
    }

    public void setTitle(String title, Skin skin){
        this.titleLabel = new OwnLabel(title, skin, "small");
    }

    public void setPrefWidth(float prefWidth) {
        this.prefWidth = prefWidth;
    }

    public void setPrefHeight(float prefHeight) {
        this.prefHeight = prefHeight;
    }

    public float getPrefWidth() {
        if (!vertical && prefWidth > 0) {
            return prefWidth;
        } else {
            return super.getPrefWidth();
        }

    }

    public float getPrefHeight() {
        if (vertical && prefHeight > 0) {
            return prefHeight;
        } else {
            return super.getPrefHeight();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (titleLabel != null) {
            float padY = 2f;
            float padX = 10.8f;
            titleLabel.setPosition(getX() + padX, getY() + getHeight() - titleLabel.getHeight() - padY);
            titleLabel.draw(batch, parentAlpha);
        }
    }
}
