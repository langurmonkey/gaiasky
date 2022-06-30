package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

public abstract class AbstractUpdateSystem extends IteratingSystem implements EntityUpdater {

    public AbstractUpdateSystem(Family family, int priority) {
        super(family, priority);
    }
}
