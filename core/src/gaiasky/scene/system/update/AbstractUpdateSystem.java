package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.LocationMark;

public abstract class AbstractUpdateSystem extends IteratingSystem implements EntityUpdater {

    public AbstractUpdateSystem(Family family, int priority) {
        super(family, priority);
    }

}
