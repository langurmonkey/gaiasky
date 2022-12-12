/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class LinkButton extends OwnImageButton {

    private final String linkURL;

    public LinkButton(String linkURL, Skin skin) {
        super(skin, "link");
        this.linkURL = linkURL;
        initialize(skin);
    }

    private void initialize(Skin skin) {
        // Fix touchUp issue
        this.addListener(new ClickListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return super.touchDown(event, x, y, pointer, button);
            }

            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (event.getButton() == Input.Buttons.LEFT && linkURL != null && !linkURL.isEmpty())
                    Gdx.net.openURI(linkURL);

                // Bubble up
                super.touchUp(event, x, y, pointer, button);
            }
        });
        this.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent.Type type = ((InputEvent) event).getType();
                // Click
                if (type == InputEvent.Type.touchUp && ((InputEvent) event).getButton() == Input.Buttons.LEFT) {
                    Gdx.net.openURI(linkURL);
                } else if (type == InputEvent.Type.enter) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == InputEvent.Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });

        this.addListener(new OwnTextTooltip(linkURL, skin, 10));

    }
}
