package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.data.orbit.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scene.system.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import org.lwjgl.opengl.GL20;

/**
 * Initializes entities with a {@link Trajectory} component.
 */
public class TrajectoryInitializer extends AbstractInitSystem {

    /**
     * The trajectory utils instance.
     */
    private final TrajectoryUtils utils;

    public TrajectoryInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        utils = new TrajectoryUtils();
        TrajectoryUtils.initRefresher();
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var trajectory = Mapper.trajectory.get(entity);
        var verts = Mapper.verts.get(entity);
        var line = Mapper.line.get(entity);

        if (!trajectory.onlyBody) {
            try {
                trajectory.providerClass = (Class<? extends IOrbitDataProvider>) ClassReflection.forName(trajectory.provider);
                // Orbit data
                IOrbitDataProvider provider;
                try {
                    provider = ClassReflection.newInstance(trajectory.providerClass);
                    provider.load(trajectory.oc.source, new OrbitDataLoaderParameters(base.names[0], trajectory.providerClass, trajectory.oc, trajectory.multiplier, trajectory.numSamples), trajectory.newMethod);
                    verts.pointCloudData = provider.getData();
                } catch (Exception e) {
                    logger.error(e);
                }
            } catch (ReflectionException e) {
                logger.error(e);
            }
        }
        // All trajectories have the same primitive and render group.
        verts.glPrimitive = GL20.GL_LINE_STRIP;
        verts.renderGroup = RenderGroup.LINE_GPU;

        line.renderConsumer = LineEntityRenderSystem::renderTrajectory;

        // Initialize default colors if needed
        if (body.color == null) {
            body.color = new float[] { 0.8f, 0.8f, 0.8f, 1f };
        }
        if (trajectory.pointColor == null) {
            trajectory.pointColor = new float[] { 0.8f, 0.8f, 0.8f, 1f };
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var transform = Mapper.transform.get(entity);
        var trajectory = Mapper.trajectory.get(entity);
        var verts = Mapper.verts.get(entity);

        trajectory.alpha = body.color[3];
        utils.initOrbitMetadata(body, trajectory, verts);
        verts.primitiveSize = 1.1f;

        if (trajectory.body != null) {
            var bodyBase = Mapper.base.get(trajectory.body);
            trajectory.params = new OrbitDataLoaderParameters(bodyBase.names[0], null, trajectory.oc.period, 500);
            trajectory.params.entity = entity;
        }

        utils.initializeTransformMatrix(trajectory, graph, transform);

        trajectory.isInOrbitalElementsGroup = graph.parent != null && Mapper.orbitElementsSet.has(graph.parent);
    }
}
