package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Coordinates;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.RefSysTransform;
import gaiasky.util.math.Intersectord;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

/**
 * Contains methods that act on trajectory entities.
 */
public class TrajectoryUtils {

    private Vector3b B31, B32;
    private Vector3d D31, D32, D33;

    public TrajectoryUtils() {
        B31 = new Vector3b();
        B32 = new Vector3b();
        D31 = new Vector3d();
        D32 = new Vector3d();
        D33 = new Vector3d();
    }

    public void computeExtrasolarSystemTransformMatrix(GraphNode graph, RefSysTransform transform) {
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
