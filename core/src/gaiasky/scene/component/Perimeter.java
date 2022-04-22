package gaiasky.scene.component;

import com.artemis.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Perimeter extends Component {

    public float[][][] loc2d, loc3d;

    /** Max latitude/longitude and min latitude/longitude **/
    public Vector2 maxlonlat;
    public Vector2 minlonlat;
    /** Cartesian points corresponding to maximum lonlat and minimum lonlat **/
    public Vector3 cart0;
}
