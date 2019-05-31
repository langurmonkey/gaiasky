/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

public class LinkButton  extends OwnImageButton {

    private String linkURL;

    public LinkButton(String linkURL, Skin skin){
        super(skin, "link");
        this.linkURL = linkURL;
        initialize(skin);
    }

    private void initialize(Skin skin) {
        this.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent.Type type = ((InputEvent) event).getType();
                // Click
                if (type == InputEvent.Type.touchUp && ((InputEvent) event).getButton() == Input.Buttons.LEFT) {
                    Gdx.net.openURI(linkURL);
                } else if (type == InputEvent.Type.enter) {
                    Gdx.graphics.setCursor(GlobalResources.linkCursor);
                } else if (type == InputEvent.Type.exit) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });

        this.addListener(new OwnTextTooltip(linkURL, skin, 10));
    }
}
