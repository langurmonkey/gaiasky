package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;

public class RulerRadio extends EntityRadio {

    public RulerRadio(Entity entity) {
        super(entity);
        EventManager.instance.subscribe(this, Event.RULER_ATTACH_0, Event.RULER_ATTACH_1, Event.RULER_CLEAR);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        var ruler = Mapper.ruler.get(entity);

        switch (event) {
        case RULER_ATTACH_0:
            String name = (String) data[0];
            ruler.setName0(name);
            break;
        case RULER_ATTACH_1:
            name = (String) data[0];
            ruler.setName1(name);
            break;
        case RULER_CLEAR:
            ruler.setName0(null);
            ruler.setName1(null);
            break;
        default:
            break;
        }

    }
}
