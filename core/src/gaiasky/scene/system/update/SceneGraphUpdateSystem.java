package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

/**
 * Processes entities in a scene graph, which have a {@link GraphRoot}
 * component. Generally, this should be a single entity unless
 * we have more than one scene graph.
 */
public class SceneGraphUpdateSystem extends EntitySystem {
    private static Log logger = Logger.getLogger(SceneGraphUpdateSystem.class);

    private ICamera camera;
    private final ITimeFrameProvider time;
    private Family family;
    private ImmutableArray<Entity> entities;
    private Vector3d D31;

    /**
     * Instantiates a system that will iterate over the entities described by the Family.
     *
     * @param family The family of entities iterated over in this System. In this case, it should be just one ({@link GraphRoot}.
     */
    public SceneGraphUpdateSystem(Family family, int priority, ITimeFrameProvider time) {
        super(priority);
        this.family = family;
        this.time = time;
        this.D31 = new Vector3d();
    }

    public void setCamera(ICamera camera) {
        synchronized (camera) {
            this.camera = camera;
        }
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(family);
        if (entities.size() > 1) {
            logger.error("The scene graph update system should only update one entity, the root.");
        }
    }

    @Override
    public void removedFromEngine(Engine engine) {
        entities = null;
    }

    @Override
    public void update(float deltaTime) {
        for (int i = 0; i < entities.size(); ++i) {
            processEntity(entities.get(i), deltaTime);
        }
    }

    /**
     * @return set of entities processed by the system
     */
    public ImmutableArray<Entity> getEntities() {
        return entities;
    }

    /**
     * @return the Family used when the system was created
     */
    public Family getFamily() {
        return family;
    }

    protected void processEntity(Entity entity, float deltaTime) {
        // This runs the root node
        GraphNode root = entity.getComponent(GraphNode.class);

        synchronized (camera) {
            root.translation.set(camera.getInversePos());
        }
        update(entity, time, null, 1);
    }

    private void update(Entity entity, ITimeFrameProvider time, final Vector3b parentTransform, float opacity) {
        var graph = Mapper.graph.get(entity);
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var coordinates = Mapper.coordinates.get(entity);
        var rotation = Mapper.rotation.get(entity);

        // Update local position here
        if (time.getHdiff() != 0 && coordinates != null && coordinates.coordinates != null) {
            // Load this object's equatorial cartesian coordinates into pos
            coordinates.timeOverflow = coordinates.coordinates.getEquatorialCartesianCoordinates(time.getTime(), body.pos) == null;

            // Update the spherical position
            gaiasky.util.coord.Coordinates.cartesianToSpherical(body.pos, D31);
            body.posSph.set((float) (Nature.TO_DEG * D31.x), (float) (Nature.TO_DEG * D31.y));

            // Update angle
            if (rotation != null && rotation.rc != null)
                rotation.rc.update(time);
        }

        // Update translation
        graph.translation.set(parentTransform).add(body.pos);

        // Update opacity
        base.opacity = opacity * getVisibilityOpacityFactor(base);

        // Apply proper motion if needed
        if (Mapper.pm.has(entity)) {
            var pm = Mapper.pm.get(entity);
            Vector3d pmv = D31.set(pm.pm).scl(AstroUtils.getMsSince(time.getTime(), AstroUtils.JD_J2015_5) * Nature.MS_TO_Y);
            graph.translation.add(pmv);
        }

        // Update supporting attributes
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
