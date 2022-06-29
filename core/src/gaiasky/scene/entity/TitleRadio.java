package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.event.Event;
import gaiasky.scene.Mapper;

public class TitleRadio extends EntityRadio {

    public TitleRadio(Entity entity) {
        super(entity);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        var body = Mapper.body.get(entity);

        switch (event) {
        case UI_THEME_RELOAD_INFO:
            Skin skin = (Skin) data[0];
            // Get new theme color and put it in the label colour
            LabelStyle headerStyle = skin.get("header", LabelStyle.class);
            body.labelColor[0] = headerStyle.fontColor.r;
            body.labelColor[1] = headerStyle.fontColor.g;
            body.labelColor[2] = headerStyle.fontColor.b;
            break;
        default:
            break;
        }

    }
}
