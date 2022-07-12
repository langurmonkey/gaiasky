package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import gaiasky.util.coord.IBodyCoordinates;

public class Coordinates implements Component, ICopy {

    /**
     * Coordinates provider. Provides position coordinates depending on time.
     **/
    public IBodyCoordinates coordinates;

    /**
     * Whether the current time puts the coordinates are out of time range.
     */
    public boolean timeOverflow;

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.coordinates = coordinates;
        copy.timeOverflow = timeOverflow;
        return copy;
    }
}
