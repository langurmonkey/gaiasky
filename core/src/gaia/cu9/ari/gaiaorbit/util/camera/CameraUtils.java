package gaia.cu9.ari.gaiaorbit.util.camera;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import gaia.cu9.ari.gaiaorbit.scenegraph.Planet;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Contains camera utilities/
 * 
 * @author tsagrista
 *
 */
public class CameraUtils {

    /**
     * Checks if the planet p is hit by the screen position x and y.
     * 
     * @param p
     *            The planet
     * @return Whether an intersection has occurred
     */
    public static boolean intersectScreenSphere(Planet p, ICamera camera, int sx, int sy, Vector3 v0, Vector3 v1, Vector3 vec, Vector3 intersection) {
        p.translation.put(vec);
        v0.set(sx, sy, 0f);
        v1.set(sx, sy, 0.5f);
        camera.getCamera().unproject(v0);
        camera.getCamera().unproject(v1);
        Ray ray = new Ray(v0, v1.sub(v0).nor());
        return Intersector.intersectRaySphere(ray, vec, (float) p.getRadius(), intersection);
    }

    public static boolean projectLonLat(Planet p, ICamera camera, double lon, double lat, Vector3 point, Vector3 pos, Vector3d in, Vector3d out, Matrix4 localTransform, Vector2 xy) {
        lon = Math.toRadians(lon - 90);
        lat = Math.toRadians(lat);
        in.set(lon, lat, p.getRadius());
        Coordinates.sphericalToCartesian(in, out);
        out.put(point);

        p.setToLocalTransform(1, localTransform, false);

        point.mul(localTransform);
        p.translation.put(pos);
        // Here we get the absolute position of [lon|lat] in cartesian
        // coordinates
        point.add(pos);

        camera.getCamera().project(point);

        xy.x = point.x;
        xy.y = point.y;
        return true;
    }

    public static boolean getLonLat(Planet p, ICamera camera, int sx, int sy, Vector3 v0, Vector3 v1, Vector3 vec, Vector3 intersection, Vector3d in, Vector3d out, Matrix4 localTransformInv, double[] lonlat) {

        boolean inter = intersectScreenSphere(p, camera, sx, sy, v0, v1, vec, intersection);

        if (inter) {
            // We found an intersection point
            p.setToLocalTransform(1, localTransformInv, false);
            localTransformInv.inv();
            intersection.mul(localTransformInv);

            in.set(intersection);
            Coordinates.cartesianToSpherical(in, out);

            lonlat[0] = (Math.toDegrees(out.x) + 90) % 360;
            lonlat[1] = Math.toDegrees(out.y);
            return true;
        } else {
            return false;
        }
    }

}
