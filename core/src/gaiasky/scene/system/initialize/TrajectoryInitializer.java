package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.data.OrbitRefresher;
import gaiasky.data.orbit.IOrbitDataProvider;
import gaiasky.data.orbit.OrbitFileDataProvider;
import gaiasky.data.orbit.OrbitalParametersProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.component.Trajectory.OrbitOrientationModel;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.util.math.Vector3d;

/**
 * Initializes entities with a {@link Trajectory} component.
 */
public class TrajectoryInitializer extends InitSystem {

    /**
     * The trajectory utils instance.
     */
    private final TrajectoryUtils utils;

    public TrajectoryInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        utils = new TrajectoryUtils();
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var trajectory = Mapper.trajectory.get(entity);
        var verts = Mapper.verts.get(entity);

        if (!trajectory.onlyBody)
            try {
                trajectory.providerClass = (Class<? extends IOrbitDataProvider>) ClassReflection.forName(trajectory.provider);
                // Orbit data
                IOrbitDataProvider provider;
                try {
                    provider = ClassReflection.newInstance(trajectory.providerClass);
                    provider.load(trajectory.oc.source, new OrbitDataLoaderParameter(base.names[0], trajectory.providerClass, trajectory.oc, trajectory.multiplier, 100), trajectory.newMethod);
                    verts.pointCloudData = provider.getData();
                } catch (Exception e) {
                    logger.error(e);
                }
            } catch (ReflectionException e) {
                logger.error(e);
            }

        // Initialize default colors if needed
        if (body.color == null) {
            body.color = new float[] { 0.8f, 0.8f, 0.8f, 1f };
        }
        if (trajectory.pointColor == null) {
            trajectory.pointColor = new float[] { 0.8f, 0.8f, 0.8f, 1f };
        }

        initRefresher();

    }

    @Override
    public void setUpEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var transform = Mapper.transform.get(entity);
        var trajectory = Mapper.trajectory.get(entity);
        var verts = Mapper.verts.get(entity);

        trajectory.alpha = body.color[3];
        initOrbitMetadata(body, trajectory, verts);
        verts.primitiveSize = 1.1f;

        if (body != null) {
            trajectory.params = new OrbitDataLoaderParameter(base.names[0], null, trajectory.oc.period, 500);
            trajectory.params.entity = entity;
        }

        initializeTransformMatrix(trajectory, graph, transform);

        trajectory.isInOrbitalElementsGroup = graph.parent != null && Mapper.orbitElementsSet.has(graph.parent);
    }

    private void initRefresher() {
        if (Trajectory.orbitRefresher == null) {
            Trajectory.orbitRefresher = new OrbitRefresher();
        }
    }

    private void initOrbitMetadata(Body body, Trajectory trajectory, Verts verts) {
        if (verts.pointCloudData != null) {
            trajectory.orbitStartMs = verts.pointCloudData.getDate(0).toEpochMilli();
            trajectory.orbitEndMs = verts.pointCloudData.getDate(verts.pointCloudData.getNumPoints() - 1).toEpochMilli();
            if (!trajectory.onlyBody) {
                int last = verts.pointCloudData.getNumPoints() - 1;
                Vector3d v = new Vector3d(verts.pointCloudData.x.get(last), verts.pointCloudData.y.get(last), verts.pointCloudData.z.get(last));
                body.size = (float) v.len() * 5;
            }
        }
        trajectory.mustRefresh = trajectory.providerClass != null
                && trajectory.providerClass.equals(OrbitFileDataProvider.class)
                && trajectory.body != null
                // body instanceof Planet
                && Mapper.atmosphere.has(trajectory.body)
                && trajectory.oc.period > 0;
        trajectory.orbitTrail = trajectory.orbitTrail | trajectory.mustRefresh | (trajectory.providerClass != null && trajectory.providerClass.equals(OrbitalParametersProvider.class));
    }

    private void initializeTransformMatrix(Trajectory trajectory, GraphNode graph, RefSysTransform transform) {
        if (trajectory.model == OrbitOrientationModel.EXTRASOLAR_SYSTEM && transform.matrix == null && graph.parent != null) {
            utils.computeExtrasolarSystemTransformMatrix(graph, transform);
        }
    }

}
