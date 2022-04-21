package gaiasky.scene.component;

import com.artemis.Component;
import com.badlogic.gdx.math.Vector3;

public class ProperMotion extends Component {
    /**
     * Proper motion in cartesian coordinates [U/yr]
     **/
    public Vector3 pm;
    /**
     * MuAlpha [mas/yr], Mudelta [mas/yr], radvel [km/s]
     **/
    public Vector3 pmSph;
}
