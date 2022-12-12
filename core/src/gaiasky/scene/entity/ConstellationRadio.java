package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.util.tree.IPosition;

import java.util.Map;

/**
 * This radio is in charge of updating constellations when required.
 */
public class ConstellationRadio extends EntityRadio {

    public ConstellationRadio(Entity entity) {
        super(entity);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.CONSTELLATION_UPDATE_CMD) {
            Scene scene = (Scene) data[0];
            setUp(scene);
        }
    }

    public void setUp(Scene scene) {
        var constel = Mapper.constel.get(entity);
        synchronized (constel) {
            if (!constel.allLoaded) {
                int nPairs = constel.ids.size;
                Map<Integer, IPosition> hipMap = scene.index().getHipMap();
                constel.allLoaded = true;
                for (int i = 0; i < nPairs; i++) {
                    int[] pair = constel.ids.get(i);
                    IPosition s1, s2;
                    s1 = hipMap.get(pair[0]);
                    s2 = hipMap.get(pair[1]);
                    if (constel.lines[i] == null && s1 != null && s2 != null) {
                        constel.lines[i] = new IPosition[] { s1, s2 };
                    } else {
                        constel.allLoaded = false;
                    }
                }
            }
        }
    }
}
