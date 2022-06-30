package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.camera.rec.Keyframe;

public class Keyframes implements Component {

    /**
     * Keyframe objects.
     */
    public Array<Keyframe> keyframes;
    /**
     * Selected keyframe.
     **/
    public Keyframe selected = null;
    /**
     * Highlighted keyframe.
     */
    public Keyframe highlighted = null;

    /**
     * The actual path.
     **/
    public Entity path;
    /**
     * The segments joining the knots.
     **/
    public Entity segments;
    /**
     * The knots, or keyframe positions.
     **/
    public Entity knots;
    /**
     * Knots which are also seams.
     **/
    public Entity knotsSeam;
    /**
     * Selected knot.
     **/
    public Entity selectedKnot;

    /**
     * High-lighted knot.
     */
    public Entity highlightedKnot;

    /**
     * Contains pairs of {direction, up} representing the orientation at each knot.
     **/
    public Array<Entity> orientations;

    /**
     * Objects.
     **/
    public Array<Entity> objects;

    /**
     * Invisible focus for camera.
     */
    public Entity focus;

    /**
     * Multiplier to primitive size
     **/
    public final float ss = 1f;

    public void clearOrientations() {
        for (Entity vo : orientations)
            objects.removeValue(vo, true);
        orientations.clear();
    }
}
