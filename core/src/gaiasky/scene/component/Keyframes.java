package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.view.VertsView;
import gaiasky.scenegraph.Invisible;
import gaiasky.scenegraph.Polyline;
import gaiasky.scenegraph.VertsObject;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.camera.rec.CameraKeyframeManager;
import gaiasky.util.camera.rec.Keyframe;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.Vector3d;

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

    /** Reference to the scene. **/
    public Scene scene;

    /**
     * Multiplier to primitive size
     **/
    public final float ss = 1f;

    public Keyframes() {
    }


    public void clearOrientations() {
        for (Entity vo : orientations)
            objects.removeValue(vo, true);
        orientations.clear();
    }
}
