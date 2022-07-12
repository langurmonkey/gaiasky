/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.update.ModelUpdater;
import gaiasky.scene.view.FocusView;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.Planet;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Nature;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3d;

/**
 * Contains camera utilities
 */
public class CameraUtils {

    private static ModelUpdater updater = new ModelUpdater(null, 0);

    /**
     * Checks if the entity e is hit by the screen position x and y.
     *
     * @param e The entity.
     * @return Whether an intersection has occurred
     */
    public static boolean intersectScreenSphere(IFocus f, Entity e, ICamera camera, int sx, int sy, Vector3 v0, Vector3 v1, Vector3 vec, Vector3 intersection) {
        var graph = Mapper.graph.get(e);
        graph.translation.put(vec);
        v0.set(sx, sy, 0f);
        v1.set(sx, sy, 0.5f);
        camera.getCamera().unproject(v0);
        camera.getCamera().unproject(v1);
        Ray ray = new Ray(v0, v1.sub(v0).nor());
        return Intersector.intersectRaySphere(ray, vec, (float) f.getRadius(), intersection);
    }

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

    public static boolean getLonLat(FocusView f, Entity e, ICamera camera, int sx, int sy, Vector3 v0, Vector3 v1, Vector3 vec, Vector3 intersection, Vector3d in, Vector3d out, Matrix4 localTransformInv, double[] lonlat) {

        boolean inter = intersectScreenSphere(f, e, camera, sx, sy, v0, v1, vec, intersection);

        if (inter) {
            // We found an intersection point
            updater.setToLocalTransform(e, f.getBody(), f.getGraph(), 1, localTransformInv, false);
            localTransformInv.inv();
            intersection.mul(localTransformInv);

            in.set(intersection);
            Coordinates.cartesianToSpherical(in, out);

            lonlat[0] = (Nature.TO_DEG * out.x + 90) % 360;
            lonlat[1] = Nature.TO_DEG * out.y;
            return true;
        } else {
            return false;
        }
    }

}
