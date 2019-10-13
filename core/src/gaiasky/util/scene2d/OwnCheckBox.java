/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class OwnCheckBox extends CheckBox {
    private Color regularColor;

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

}
