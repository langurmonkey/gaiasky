package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.RulerRadio;

public class RulerInitializer extends InitSystem {
    public RulerInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var text = Mapper.text.get(entity);
        var line = Mapper.line.get(entity);

        text.textScale = 0.2f;
        text.labelFactor = 0.0005f;
        text.labelMax = 1f;

        line.lineWidth = 1f;

        EventManager.instance.subscribe(new RulerRadio(entity), Event.RULER_ATTACH_0, Event.RULER_ATTACH_1, Event.RULER_CLEAR);
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
