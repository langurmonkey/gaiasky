package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.Settings;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;

public class Body extends Component {
    /**
     * Position of this entity in the local reference system. The units are
     * {@link gaiasky.util.Constants#U_TO_KM} by default.
     */
    public Vector3b pos;

    /**
     * Position in the equatorial system; ra, dec.
     */
    public Vector2d posSph;

    /**
     * Size factor in internal units.
     */
    public float size;

    /**
     * The distance to the camera from the focus center.
     */
    public double distToCamera;

    /**
     * The view angle, in radians.
     */
    public double viewAngle;

    /**
     * The view angle corrected with the field of view angle, in radians.
     */
    public double viewAngleApparent;

    /**
     * Base RGB color
     */
    public float[] cc;
    public float[] labelcolor = Settings.settings.program.ui.isUINightMode() ? new float[] { 1, 0, 0, 1 } : new float[] { 1, 1, 1, 1 };
}
