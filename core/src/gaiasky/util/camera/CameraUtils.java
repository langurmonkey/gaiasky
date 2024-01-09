/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.update.ModelUpdater;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Nature;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3d;

public class CameraUtils {

    private static final ModelUpdater updater = new ModelUpdater(null, 0);

    /**
     * Checks if the entity e is hit by the screen position x and y.
     *
     * @param e The entity.
     *
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

    public static boolean getLonLat(FocusView f, Entity e, ICamera camera, int sx, int sy, Vector3 v0, Vector3 v1, Vector3 vec, Vector3 intersection, Vector3d in, Vector3d out, Matrix4 localTransformInv, double[] lonlat) {

        boolean inter = intersectScreenSphere(f, e, camera, sx, sy, v0, v1, vec, intersection);

        if (inter) {
            // We found an intersection point.
            updater.setToLocalTransform(e, f.getBody(), f.getGraph(), 1, localTransformInv, false);
            localTransformInv.inv();
            // We use v0 because we need the unmodified intersection position in the camera manager.
            v0.set(intersection);
            v0.mul(localTransformInv);

            in.set(v0);
            Coordinates.cartesianToSpherical(in, out);

            lonlat[0] = (Nature.TO_DEG * out.x + 90) % 360;
            lonlat[1] = Nature.TO_DEG * out.y;
            return true;
        } else {
            return false;
        }
    }

    public static void unproject(Camera camera, Vector3 screenCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight, int screenHeight) {
        float x = screenCoords.x - viewportX, y = screenHeight - screenCoords.y - viewportY;
        screenCoords.x = (2 * x) / viewportWidth - 1;
        screenCoords.y = (2 * y) / viewportHeight - 1;
        screenCoords.z = 2 * screenCoords.z - 1;
        screenCoords.prj(camera.invProjectionView);
    }

    public static void unproject(Camera camera, Vector3 screenCoords, int screenWidth, int screenHeight) {
        unproject(camera, screenCoords, 0, 0, screenWidth, screenHeight, screenHeight);
    }

}
