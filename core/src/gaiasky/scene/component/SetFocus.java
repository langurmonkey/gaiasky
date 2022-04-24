package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public class SetFocus implements Component {

    /**
     * Reference to the current focus.
     */
    public IParticleRecord focus;
    /**
     * Index of the particle acting as focus. Negative if we have no focus here.
     */
    public int focusIndex;

    /**
     * Candidate to focus.
     */
    public int candidateFocusIndex;

    /**
     * Position of the current focus
     */
    public Vector3d focusPosition;

    /**
     * Position in equatorial coordinates of the current focus in radians
     */
    public Vector2d focusPositionSph;

    /**
     * FOCUS_MODE attributes
     */
    public double focusDistToCamera, focusViewAngle, focusViewAngleApparent, focusSize;
}
