package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;

public class OrbitElementsSet implements Component {
    public Array<Entity> alwaysUpdate;
    public boolean initialUpdate = true;

    public void markForUpdate(Render render) {
        EventManager.publish(Event.GPU_DISPOSE_ORBITAL_ELEMENTS, render);
    }
}
