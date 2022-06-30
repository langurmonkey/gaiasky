package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.RulerRadio;
import gaiasky.scene.system.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;

public class RulerInitializer extends InitSystem {
    public RulerInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var label = Mapper.label.get(entity);
        var line = Mapper.line.get(entity);

        label.textScale = 0.2f;
        label.labelFactor = 0.0005f;
        label.labelMax = 1f;
        label.renderConsumer = (LabelEntityRenderSystem rs, LabelView l, ExtSpriteBatch b, ExtShaderProgram s, FontRenderSystem f, RenderingContext r, ICamera c)
                -> rs.renderRuler(l, b, s, f, r, c);

        // Lines.
        line.lineWidth = 1f;
        line.renderConsumer = (LineEntityRenderSystem rs, Entity e, LinePrimitiveRenderer r, ICamera c, Float a)
                -> rs.renderRuler(e, r, c, a);

        EventManager.instance.subscribe(new RulerRadio(entity), Event.RULER_ATTACH_0, Event.RULER_ATTACH_1, Event.RULER_CLEAR);
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
