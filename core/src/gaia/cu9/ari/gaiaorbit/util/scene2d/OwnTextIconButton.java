package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

import gaia.cu9.ari.gaiaorbit.util.GlobalConf;

/**
 * OwnTextButton with an icon. Also, the cursor changes when the mouse rolls
 * over. It also fixes the size issue.
 * 
 * @author Toni Sagrista
 *
 */
public class OwnTextIconButton extends OwnTextButton {

    private Image icon;

    public OwnTextIconButton(String text, Image icon, Skin skin) {
        super(text, skin);
        setIcon(icon);
    }

    public OwnTextIconButton(String text, Image icon, Skin skin, String styleName) {
        super(text, skin, styleName);
        setIcon(icon);
    }

    public OwnTextIconButton(String text, Image icon, TextButtonStyle style) {
        super(text, style);
        setIcon(icon);
    }

    public void setIcon(Image icon) {
        this.icon = icon;
        clearChildren();
        this.align(Align.left);
            add(this.icon).left().padLeft(GlobalConf.SCALE_FACTOR).padRight((getLabel().getText().length > 0 ? 5 : 1) * GlobalConf.SCALE_FACTOR);
            add(getLabel()).left();
    }

}
