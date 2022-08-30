package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.RulerRadio;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;

public class RulerInitializer extends AbstractInitSystem {
    public RulerInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var label = Mapper.label.get(entity);
        var line = Mapper.line.get(entity);

        label.label = true;
        label.textScale = 0.2f;
        label.labelFactor = 0.0005f;
        label.labelMax = 1f;
        label.renderConsumer = LabelEntityRenderSystem::renderRuler;
        label.renderFunction = LabelView::renderTextRuler;

        // Lines.
        line.lineWidth = 1f;
        line.renderConsumer = LineEntityRenderSystem::renderRuler;

        EventManager.instance.subscribe(new RulerRadio(entity), Event.RULER_ATTACH_0, Event.RULER_ATTACH_1, Event.RULER_CLEAR);
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
