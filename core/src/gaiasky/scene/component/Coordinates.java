package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.coord.IBodyCoordinates;

public class Coordinates implements Component {

    /**
     * Coordinates provider. Provides position coordinates depending on time.
     **/
    public IBodyCoordinates coordinates;

    /**
     * Whether the current time puts the coordinates are out of time range.
     */
    public boolean timeOverflow;
}
