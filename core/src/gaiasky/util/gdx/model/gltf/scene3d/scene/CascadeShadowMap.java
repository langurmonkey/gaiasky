/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.DoubleArray;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.CascadeShadowMapAttribute;
import gaiasky.util.gdx.model.gltf.scene3d.lights.DirectionalShadowLight;
import gaiasky.util.math.BoundingBoxDouble;
import gaiasky.util.math.FrustumDouble;
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
    private final Vector3d tmp = new Vector3d();
    private final Vector3d tmp2 = new Vector3d();

    /** Attributes used for the CSM camera, which does not cover the same range as the full camera. **/
    protected Matrix4d projection, view, combined;
    /** Inverse projection view matrix. **/
    private final Matrix4d invProjectionView = new Matrix4d();
    protected FrustumDouble frustum;
    /** Camera near value for the cascaded shadow maps. **/
    public double CAM_NEAR_CSM;
    /** Camera far value for the cascaded shadow maps. **/
    public double CAM_FAR_CSM;

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

        // Camera attributes.
        projection = new Matrix4d();
        view = new Matrix4d();
        combined = new Matrix4d();
        frustum = new FrustumDouble();
        CAM_NEAR_CSM = 1000.0 * Constants.M_TO_U;
        CAM_FAR_CSM = 0.01 * Constants.AU_TO_U;
    }

    @Override
    public void dispose() {
        for (DirectionalShadowLight light : lights) {
            light.dispose();
        }
        lights.clear();
    }

    public double getSplitDistance(int layer) {
        if (layer < 0 || layer > cascadeCount) {
            return 1.0;
        }

        return splitRates.get(layer + 1);
    }

    /**
     * Setup base light and extra cascades based on scene camera frustum. With automatic split rates.
     *
     * @param sceneCamera      The camera used to render the scene (frustum should be up-to-date).
     * @param baseLight        The default shadow light, used for far shadows.
     * @param lightDepthFactor Shadow box depth factor, depends on the scene. Must be >= 1. Greater than 1 means more
     *                         objects cast shadows but less precision.
     *                         A 1 value restricts shadow box depth to the frustum (only visible objects by the scene
     *                         camera).
     * @param splitDivisor     Describe how to split scene camera frustum. . With a value of 4, far cascade covers the
     *                         range: 1/4 to 1/1, next cascade, the range 1/16 to 1/4, and so on. The closest one covers
     *                         the remaining starting
     *                         from 0. When used with 2 extra cascades (3 areas), split points are: 0.0, 1/16, 1/4, 1.0.
     */
    public void setCascades(ICamera sceneCamera, DirectionalShadowLight baseLight, double lightDepthFactor, double splitDivisor) {
        splitRates.clear();
        double rate = 1.0;
        for (int i = 0; i < cascadeCount + 1; i++) {
            splitRates.add(rate);
            rate /= splitDivisor;
        }
        splitRates.add(0);
        splitRates.reverse();

        setCascades(sceneCamera, baseLight, lightDepthFactor, splitRates);
    }

    /**
     * Setup base light and extra cascades based on scene camera frustum. With user defined split rates.
     *
     * @param sceneCamera      The camera used to render the scene (frustum should be up-to-date).
     * @param base             The default shadow light, used for far shadows.
     * @param lightDepthFactor Shadow box depth factor, depends on the scene. Must be >= 1. Greater than 1 means more
     *                         objects cast shadows but less precision.
     *                         A 1 value restricts shadow box depth to the frustum (only visible objects by the scene
     *                         camera).
     * @param splitRates       Describe how to split scene camera frustum. The first 2 values define near and far rate
     *                         for
     *                         the closest cascade,
     *                         Second and third value define near and far rate for the second cascade, and so on.
     *                         When used with 2 extra cascades (3 areas), 4 split rates are expected. Eg: [0.0, 0.1,
     *                         0.3,
     *                         1.0].
     */
    public void setCascades(ICamera sceneCamera, DirectionalShadowLight base, double lightDepthFactor, DoubleArray splitRates) {
        if (splitRates.size != cascadeCount + 2) {
            throw new IllegalArgumentException("Invalid splitRates, expected " + (cascadeCount + 2) + " items.");
        }

        // Generate the matrices for the CSM camera using the modified near and far planes. True camera stays at origin, hence (0 0 0).
        updateCSM(sceneCamera.getCamera(), tmp2.set(0, 0, 0), sceneCamera.getDirection(), sceneCamera.getUp());

        syncExtraCascades(base);

        for (int i = 0; i < cascadeCount + 1; i++) {
            double splitNear = splitRates.get(i);
            double splitFar = splitRates.get(i + 1);
            DirectionalShadowLight light = i < cascadeCount ? lights.get(i) : base;
            if (light != base) {
                light.direction.set(base.direction);
                light.getCamera().up.set(base.getCamera().up);
            }
            setCascades(light, splitNear, splitFar, lightDepthFactor);
        }
    }

    private void setCascades(DirectionalShadowLight shadowLight, double splitNear, double splitFar, double lightDepthFactor) {

        for (int i = 0; i < 4; i++) {
            a.set(frustum.planePoints[i]);
            b.set(frustum.planePoints[i + 4]);

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

        double lightDepth = Math.max(box.getDepth(), box.getDepth() * lightDepthFactor);

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

    public void updateCSM(PerspectiveCamera cam, Vector3d position, Vector3d direction, Vector3d up) {
        double aspect = cam.viewportWidth / cam.viewportHeight;
        projection.setToProjection(CAM_NEAR_CSM, CAM_FAR_CSM, cam.fieldOfView, aspect);
        view.setToLookAt(position, tmp.set(position).add(direction), up);
        combined.set(projection);
        Matrix4d.mul(combined.val, view.val);

        invProjectionView.set(combined);
        Matrix4d.inv(invProjectionView.val);
        frustum.update(invProjectionView);
    }
}
