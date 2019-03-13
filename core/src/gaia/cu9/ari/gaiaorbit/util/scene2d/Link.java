package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

/**
 * Link widget.
 * 
 * @author Toni Sagrista
 *
 */
public class Link extends Label {

    private String linkURL;

    public Link(CharSequence text, LabelStyle style, String linkURL) {
        super(text, style);
        this.linkURL = linkURL;
        initialize();
    }

    public Link(CharSequence text, Skin skin, String fontName, Color color, String linkto) {
        super(text, skin, fontName, color);
        this.linkURL = linkto;
        initialize();
    }

    public Link(CharSequence text, Skin skin, String fontName, String colorName, String linkto) {
        super(text, skin, fontName, colorName);
        this.linkURL = linkto;
        initialize();
    }

    public Link(CharSequence text, Skin skin, String styleName, String linkto) {
        super(text, skin, styleName);
        this.linkURL = linkto;
        initialize();
    }

    public Link(CharSequence text, Skin skin, String linkto) {
        super(text, skin);
        this.linkURL = linkto;
        initialize();
    }

    private void initialize() {
        this.addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                // Click
                if (type == Type.touchUp && ((InputEvent) event).getButton() == Buttons.LEFT) {
                    Gdx.net.openURI(linkURL);
                } else if (type == Type.enter) {
                    Gdx.graphics.setCursor(GlobalResources.linkCursor);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });
    }

    public String getLinkURL() {
        return linkURL;
    }

    public void setLinkURL(String linkURL) {
        this.linkURL = linkURL;
    }

}
