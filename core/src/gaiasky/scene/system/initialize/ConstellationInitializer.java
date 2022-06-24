package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.ConstellationRadio;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;

public class ConstellationInitializer extends InitSystem {

    public ConstellationInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var constel = Mapper.constel.get(entity);

        constel.posd = new Vector3d();
        constel.lineWidth = 1;
        constel.alpha = 0.4f;

        if (body.color == null) {
            body.color = new float[] { .5f, 1f, .5f, constel.alpha };
        }

        EventManager.instance.subscribe(new ConstellationRadio(entity), Event.CONSTELLATION_UPDATE_CMD);

    }

    @Override
    public void setUpEntity(Entity entity) {
        var constel = Mapper.constel.get(entity);

        int nPairs = constel.ids.size;
        if (constel.lines == null) {
            constel.lines = new IPosition[nPairs][];
        }
    }
}
