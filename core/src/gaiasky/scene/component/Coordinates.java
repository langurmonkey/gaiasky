package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.coord.IBodyCoordinates;

public class Coordinates extends Component {

    /**
     * Coordinates provider. Provides position coordinates depending on time.
     **/
    protected IBodyCoordinates coordinates;

    /**
     * Whether the current time puts the coordinates are out of time range.
     */
    public boolean timeOverflow;
}
