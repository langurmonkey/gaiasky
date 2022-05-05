package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.GaiaSky;
import gaiasky.data.OrbitRefresher;
import gaiasky.data.orbit.IOrbitDataProvider;
import gaiasky.data.orbit.OrbitFileDataProvider;
import gaiasky.data.orbit.OrbitalParametersProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.component.Trajectory.OrbitOrientationModel;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.util.math.Intersectord;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

/**
 * Initializes entities with a {@link Trajectory} component.
 */
public class TrajectoryInitializer extends InitSystem {

    private Vector3b B31, B32;
    private Vector3d D31, D32, D33;

    public TrajectoryInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        B31 = new Vector3b();
        B32 = new Vector3b();
        D31 = new Vector3d();
        D32 = new Vector3d();
        D33 = new Vector3d();
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
        if (body.cc == null) {
            body.cc = new float[] { 0.8f, 0.8f, 0.8f, 1f };
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

        trajectory.alpha = body.cc[3];
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
            computeExtrasolarSystemTransformMatrix(graph, transform);
        }
    }

    private void computeExtrasolarSystemTransformMatrix(GraphNode graph, RefSysTransform transform) {
        Entity parent = graph.parent;
        Coordinates coord = Mapper.coordinates.get(parent);
        // Compute new transform function from the orbit's parent position
        Vector3b barycenter = B31;
        if (coord != null && coord.coordinates != null) {
            coord.coordinates.getEquatorialCartesianCoordinates(GaiaSky.instance.time.getTime(), barycenter);
        } else {
            EntityUtils.getAbsolutePosition(parent, barycenter);
        }

        // Up
        Vector3b y = B32.set(barycenter).scl(1).nor();
        Vector3d yd = y.put(D31);
        // Towards north - intersect y with plane
        Vector3d zd = D32;
        Intersectord.lineIntersection(barycenter.put(new Vector3d()), (new Vector3d(yd)), new Vector3d(0, 0, 0), new Vector3d(0, 1, 0), zd);
        zd.sub(barycenter).nor();
        //zd.set(yd).crs(0, 1, 0).nor();

        // Orthogonal to ZY, right-hand system
        Vector3d xd = D33.set(yd).crs(zd);

        transform.matrix = Matrix4d.changeOfBasis(zd, yd, xd);
    }

}
