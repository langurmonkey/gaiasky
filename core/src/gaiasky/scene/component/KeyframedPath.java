package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.camera.rec.Keyframe;

public class KeyframedPath implements Component {

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
    public Verts path;
    /**
     * The segments joining the knots.
     **/
    public Verts segments;
    /**
     * The knots, or keyframe positions.
     **/
    public Verts knots;
    /**
     * Knots which are also seams.
     **/
    public Verts knotsSeam;
    /**
     * Selected knot.
     **/
    public Verts selectedKnot;

    /**
     * High-lighted knot.
     */
    public Verts highlightedKnot;

    /**
     * Contains pairs of {direction, up} representing the orientation at each knot.
     **/
    public Array<Verts> orientations;

    /**
     * Objects.
     **/
    public Array<Verts> objects;

    /**
     * Invisible focus for camera.
     */
    public Entity focus;

    /**
     * Multiplier to primitive size
     **/
    public final float ss = 1f;
}
