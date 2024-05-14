/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.DoubleArray;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.CascadeShadowMapAttribute;
import gaiasky.util.gdx.model.gltf.scene3d.lights.DirectionalShadowLight;
import gaiasky.util.math.BoundingBoxDouble;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

public class CascadeShadowMap implements Disposable {

    public final Array<DirectionalShadowLight> lights;
    public final CascadeShadowMapAttribute attribute;

    protected final int cascadeCount;

    protected final DoubleArray splitRates;
    private final Vector3d[] splitPoints;
    private final Matrix4d lightMatrix = new Matrix4d();
    private final BoundingBoxDouble box = new BoundingBoxDouble();
    private final Vector3d center = new Vector3d();
    private final Vector3d a = new Vector3d();
    private final Vector3d b = new Vector3d();
    private final Vector3d dir = new Vector3d();
    private final Vector3d up = new Vector3d();


    /**
     * @param cascadeCount How many extra cascades.
     */
    public CascadeShadowMap(int cascadeCount) {
        this.cascadeCount = cascadeCount;
        attribute = new CascadeShadowMapAttribute(this);
        lights = new Array<>(cascadeCount);
        splitRates = new DoubleArray(cascadeCount + 2);
        splitPoints = new Vector3d[8];
        for (int i = 0; i < splitPoints.length; i++) {
            splitPoints[i] = new Vector3d();
        }
    }

    @Override
    public void dispose() {
        for (DirectionalShadowLight light : lights) {
            light.dispose();
        }
        lights.clear();
    }

    /**
     * Setup base light and extra cascades based on scene camera frustum. With automatic split rates.
     *
     * @param sceneCamera   The camera used to render the scene (frustum should be up-to-date).
     * @param base          The default shadow light, used for far shadows.
     * @param minLightDepth Minimum shadow box depth, depends on the scene, big value means more objects cast but less
     *                      precision.
     *                      A zero value restricts shadow box depth to the frustum (only visible objects by the scene
     *                      camera).
     * @param splitDivisor  Describe how to split scene camera frustum. . With a value of 4, far cascade covers the
     *                      range: 1/4 to 1/1, next cascade, the range 1/16 to 1/4, and so on. The closest one covers
     *                      the remaining starting
     *                      from 0. When used with 2 extra cascades (3 areas), split points are: 0.0, 1/16, 1/4, 1.0.
     */
    public void setCascades(ICamera sceneCamera, DirectionalShadowLight base, double minLightDepth, double splitDivisor) {
        splitRates.clear();
        double rate = 1.0;
        for (int i = 0; i < cascadeCount + 1; i++) {
            splitRates.add(rate);
            rate /= splitDivisor;
        }
        splitRates.add(0);
        splitRates.reverse();

        setCascades(sceneCamera, base, minLightDepth, splitRates);
    }

    /**
     * Setup base light and extra cascades based on scene camera frustum. With user defined split rates.
     *
     * @param sceneCamera   The camera used to render the scene (frustum should be up to date).
     * @param base          The default shadow light, used for far shadows.
     * @param minLightDepth Minimum shadow box depth, depends on the scene, big value means more objects casted but
     *                      less precision.
     *                      A zero value restricts shadow box depth to the frustum (only visible objects by the scene
     *                      camera).
     * @param splitRates    Describe how to split scene camera frustum. The first 2 values define near and far rate for
     *                      the closest cascade,
     *                      Second and third value define near and far rate for the second cascade, and so on.
     *                      When used with 2 extra cascades (3 areas), 4 split rates are expected. Eg: [0.0, 0.1, 0.3,
     *                      1.0].
     */
    public void setCascades(ICamera sceneCamera, DirectionalShadowLight base, double minLightDepth, DoubleArray splitRates) {
        if (splitRates.size != cascadeCount + 2) {
            throw new IllegalArgumentException("Invalid splitRates, expected " + (cascadeCount + 2) + " items.");
        }

        syncExtraCascades(base);

        for (int i = 0; i < cascadeCount + 1; i++) {
            double splitNear = splitRates.get(i);
            double splitFar = splitRates.get(i + 1);
            DirectionalShadowLight light = i < cascadeCount ? lights.get(i) : base;
            if (light != base) {
                light.direction.set(base.direction);
                light.getCamera().up.set(base.getCamera().up);
            }
            setCascades(light, sceneCamera, splitNear, splitFar, minLightDepth);
        }
    }

    private void setCascades(DirectionalShadowLight shadowLight, ICamera cam, double splitNear, double splitFar, double minLightDepth) {

        for (int i = 0; i < 4; i++) {
            a.set(cam.getFrustum().planePoints[i]);
            b.set(cam.getFrustum().planePoints[i + 4]);

            splitPoints[i].set(a).lerp(b, splitNear);
            splitPoints[i + 4].set(a).lerp(b, splitFar);
        }

        dir.set(shadowLight.direction);
        up.set(shadowLight.getCamera().up);

        lightMatrix.setToLookAt(dir, up);
        box.inf();
        for (Vector3d splitPoint : splitPoints) {
            Vector3d v = splitPoint.mul(lightMatrix);
            box.ext(v);
        }
        double halfFrustumDepth = box.getDepth() / 2;

        double lightDepth = Math.max(minLightDepth, box.getDepth());

        box.getCenter(center);
        center.mul(lightMatrix.tra());
        center.mulAdd(dir, halfFrustumDepth - lightDepth / 2);

        shadowLight.setCenter(center);
        shadowLight.setViewport(box.getWidth(), box.getHeight(), 0, lightDepth);
    }

    /**
     * Create or recreate, if necessary ,extra cascades with same resolution as the default shadow light.
     *
     * @param base The default shadow light.
     */
    protected void syncExtraCascades(DirectionalShadowLight base) {
        int w = base.getFrameBuffer().getWidth();
        int h = base.getFrameBuffer().getHeight();
        for (int i = 0; i < cascadeCount; i++) {
            DirectionalShadowLight light;
            if (i < lights.size) {
                light = lights.get(i);
                if (light.getFrameBuffer().getWidth() != w ||
                        light.getFrameBuffer().getHeight() != h) {
                    light.dispose();
                    lights.set(i, light = createLight(w, h));
                }
            } else {
                lights.add(light = createLight(w, h));
            }
            light.direction.set(base.direction);
            light.getCamera().up.set(base.getCamera().up);
        }
    }

    /**
     * Allow subclass to use their own shadow light implementation.
     *
     * @return a new directional shadow light.
     */
    protected DirectionalShadowLight createLight(int width, int height) {
        return new DirectionalShadowLight(width, height);
    }
}
