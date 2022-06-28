package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;

public class GridRecursiveRadio extends EntityRadio {

    public GridRecursiveRadio(Entity entity) {
        super(entity);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.TOGGLE_VISIBILITY_CMD) {
            ComponentType ct = ComponentType.getFromKey((String) data[0]);
            if (ct != null && Settings.settings.scene.visibility.get(ct.toString())) {
                var body = Mapper.body.get(entity);
                var model = Mapper.model.get(entity);
                var transform = Mapper.transform.get(entity);
                var gr = Mapper.gridRec.get(entity);


                if (ct.equals(ComponentType.Equatorial)) {
                    // Activate equatorial
                    transform.setTransformName(null);
                    body.color = gr.ccEq;
                    body.labelColor = gr.ccEq;
                } else if (ct.equals(ComponentType.Ecliptic)) {
                    // Activate ecliptic
                    transform.setTransformName("eclipticToEquatorial");
                    body.color = gr.ccEcl;
                    body.labelColor = gr.ccEcl;
                } else if (ct.equals(ComponentType.Galactic)) {
                    // Activate galactic
                    transform.setTransformName("galacticToEquatorial");
                    body.color = gr.ccGal;
                    body.labelColor = gr.ccGal;
                }
                model.model.setColorAttribute(ColorAttribute.Diffuse, body.color);
                model.model.setColorAttribute(ColorAttribute.Emissive, ColorUtils.getRgbaComplimentary(body.color));
            }
        }

    }
}
