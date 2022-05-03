package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

/**
 * Processes entities in a scene graph, which have a {@link GraphRoot}
 * component. Generally, this should be a single entity unless
 * we have more than one scene graph.
 */
public class SceneGraphUpdateSystem extends IteratingSystem {

    private ICamera camera;
    private final ITimeFrameProvider time;

    public SceneGraphUpdateSystem(Family family, int priority, ITimeFrameProvider time) {
        super(family, priority);
        this.time = time;
    }

    public void setCamera(ICamera camera) {
       synchronized (camera) {
           this.camera = camera;
       }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        // This runs the root node
        GraphNode root = Mapper.graph.get(entity);

        synchronized (camera) {
           root.translation.set(camera.getInversePos());
        }
        update(entity, time, null, 1);
    }

    private void update(Entity entity, ITimeFrameProvider time, final Vector3b parentTransform, float opacity) {
        GraphNode graph = Mapper.graph.get(entity);
        Base base = Mapper.base.get(entity);
        Body body = Mapper.body.get(entity);
        Coordinates coordinates = Mapper.coordinates.get(entity);

        base.opacity = opacity;
        graph.translation.set(parentTransform);

        // Update local position here
        if(time.getHdiff() != 0 && coordinates != null) {
            // Load this object's equatorial cartesian coordinates into pos
            coordinates.timeOverflow = coordinates.coordinates.getEquatorialCartesianCoordinates(time.getTime(), body.pos) == null;
        }

        // Update translation and rest of attributes
        graph.translation.add(body.pos);
        base.opacity *= getVisibilityOpacityFactor(base);

        body.distToCamera = graph.translation.lend();
        body.viewAngle = FastMath.atan(body.size / body.distToCamera);
        body.viewAngleApparent = body.viewAngle / camera.getFovFactor();

        // Go down a level
        if (graph.children != null) {
            for (int i = 0; i < graph.children.size; i++) {
                Entity child = graph.children.get(i);
                // Update
                update(child, time, graph.translation, base.opacity);
            }
        }

    }

    protected float getVisibilityOpacityFactor(Base base) {
        long msSinceStateChange = msSinceStateChange(base);

        // Fast track
        if (msSinceStateChange > Settings.settings.scene.fadeMs)
            return base.visible ? 1 : 0;

        // Fading
        float fadeOpacity = MathUtilsd.lint(msSinceStateChange, 0, Settings.settings.scene.fadeMs, 0, 1);
        if (!base.visible) {
            fadeOpacity = 1 - fadeOpacity;
        }
        return fadeOpacity;
    }

    protected long msSinceStateChange(Base base) {
        return (long) (GaiaSky.instance.getT() * 1000f) - base.lastStateChangeTimeMs;
    }
}
