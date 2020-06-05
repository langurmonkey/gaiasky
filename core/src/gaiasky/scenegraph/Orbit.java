/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.GaiaSky;
import gaiasky.assets.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.data.OrbitRefresher;
import gaiasky.data.orbit.IOrbitDataProvider;
import gaiasky.data.orbit.OrbitFileDataProvider;
import gaiasky.data.orbit.OrbitalParametersProvider;
import gaiasky.data.util.PointCloudData;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.time.Instant;
import java.util.Date;

public class Orbit extends Polyline {
    private static final Log logger = Logger.getLogger(Orbit.class);

    private static OrbitRefresher orbitRefresher;

    private static void initRefresher() {
        if (orbitRefresher == null) {
            orbitRefresher = new OrbitRefresher();
        }
    }

    /**
     * Threshold solid angle
     **/
    protected static float SOLID_ANGLE_THRESHOLD = (float) Math.toRadians(1.5);
    public static void setSolidAngleThreshold(float angleDeg){
        SOLID_ANGLE_THRESHOLD = (float) Math.toRadians(angleDeg);
    }

    /**
     * Special overlap factor
     */
    protected static final float SHADER_MODEL_OVERLAP_FACTOR = 20f;

    protected CelestialBody body;
    protected Vector3d prev, curr;
    public double alpha;
    public Matrix4d localTransformD, transformFunction;
    protected String provider;
    protected Double multiplier = 1.0d;
    protected Class<? extends IOrbitDataProvider> providerClass;
    public OrbitComponent oc;
    // Only adds the body, not the orbit
    protected boolean onlybody = false;
    // Use new method for orbital elements
    public boolean newmethod = false;

    /**
     * Refreshing state
     */
    public boolean refreshing = false;

    private long orbitStartMs, orbitEndMs;

    /**
     * Whether the orbit must be refreshed when out of bounds
     */
    private boolean mustRefresh;
    /**
     * Whether to show the orbit as a trail or not
     */
    private boolean orbitTrail;
    private OrbitDataLoaderParameter params;

    /**
     * Orbital elements in gpu, in case there is no body
     **/
    public boolean elemsInGpu = false;

    /**
     * Point color
     **/
    public float[] pointColor;

    /**
     * Point size
     **/
    public float pointSize = 1f;

    private float distUp, distDown;

    public Orbit() {
        super();
        pointColor = new float[]{0.8f, 0.8f, 0.8f, 1f};
        localTransform = new Matrix4();
        localTransformD = new Matrix4d();
        prev = new Vector3d();
        curr = new Vector3d();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() {
        if (!onlybody)
            try {
                providerClass = (Class<? extends IOrbitDataProvider>) ClassReflection.forName(provider);
                // Orbit data
                IOrbitDataProvider provider;
                try {
                    provider = ClassReflection.newInstance(providerClass);
                    provider.load(oc.source, new OrbitDataLoaderParameter(names[0], providerClass, oc, multiplier, 100), newmethod);
                    pointCloudData = provider.getData();
                } catch (Exception e) {
                    logger.error(e);
                }
            } catch (ReflectionException e) {
                logger.error(e);
            }

        initRefresher();
    }

    @Override
    public void doneLoading(AssetManager manager) {
        alpha = cc[3];
        initOrbitMetadata();
        primitiveSize = 1.1f;

        if (body != null) {
            params = new OrbitDataLoaderParameter(body.names[0], null, oc.period, 500);
            params.orbit = this;
        }
    }

    public void setPointCloudData(PointCloudData pcd) {
        super.setPointCloudData(pcd);
    }

    public void initOrbitMetadata() {
        if (pointCloudData != null) {
            orbitStartMs = pointCloudData.getDate(0).toEpochMilli();
            orbitEndMs = pointCloudData.getDate(pointCloudData.getNumPoints() - 1).toEpochMilli();
            if (!onlybody) {
                int last = pointCloudData.getNumPoints() - 1;
                Vector3d v = new Vector3d(pointCloudData.x.get(last), pointCloudData.y.get(last), pointCloudData.z.get(last));
                this.size = (float) v.len() * 5;
            }
        }
        mustRefresh = providerClass != null && providerClass.equals(OrbitFileDataProvider.class) && body != null && body instanceof Planet && oc.period > 0;
        orbitTrail = mustRefresh | (providerClass != null && providerClass.equals(OrbitalParametersProvider.class));
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);
        if (!onlybody)
            updateLocalTransform(time.getTime());

    }

    protected void updateLocalTransform(Instant date) {
        translation.getMatrix(localTransformD);
        if (newmethod) {
            if (transformFunction != null) {
                localTransformD.mul(transformFunction).rotate(0, 1, 0, 90);
            }
            if (parent.getOrientation() != null) {
                localTransformD.mul(parent.getOrientation()).rotate(0, 1, 0, 90);
            }
        } else {
            if (transformFunction == null && parent.orientation != null)
                localTransformD.mul(parent.orientation);
            if (transformFunction != null)
                localTransformD.mul(transformFunction);

            localTransformD.rotate(0, 1, 0, oc.argofpericenter);
            localTransformD.rotate(0, 0, 1, oc.i);
            localTransformD.rotate(0, 1, 0, oc.ascendingnode);
        }
        localTransformD.putIn(localTransform);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (!onlybody && GaiaSky.instance.isOn(ct)) {
            // If overflow, return
            if (body != null && body.coordinatesTimeOverflow)
                return;

            boolean added = false;
            float angleLimit = SOLID_ANGLE_THRESHOLD * camera.getFovFactor();
            if (viewAngle > angleLimit) {
                if (viewAngle < angleLimit * SHADER_MODEL_OVERLAP_FACTOR) {
                    double alpha = MathUtilsd.lint(viewAngle, angleLimit, angleLimit * SHADER_MODEL_OVERLAP_FACTOR, 0, cc[3]);
                    this.alpha = alpha;
                } else {
                    this.alpha = cc[3];
                }

                RenderGroup rg = GlobalConf.scene.ORBIT_RENDERER == 1 ? RenderGroup.LINE_GPU : RenderGroup.LINE;

                if (body == null) {
                    // No body, always render
                    addToRender(this, rg);
                    added = true;
                } else if (body.distToCamera > distDown) {
                    // Body, disappear slowly
                    if (body.distToCamera < distUp)
                        this.alpha *= MathUtilsd.lint(body.distToCamera, distDown / camera.getFovFactor(), distUp / camera.getFovFactor(), 0, 1);
                    addToRender(this, rg);
                    added = true;
                }
            }

            if (pointCloudData == null || added) {
                refreshOrbit(false);
            }
        }
        // Orbital elements renderer
        if (body == null && oc != null && opacity > 0 && ct.get(ComponentType.Asteroids.ordinal()) && GaiaSky.instance.isOn(ComponentType.Asteroids)) {
            addToRender(this, RenderGroup.PARTICLE_ORBIT_ELEMENTS);
        }

    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        if (!onlybody) {
            alpha *= this.alpha;

            int last = 1;
            Vector3d parentPos = null;
            if (parent instanceof AbstractPositionEntity) {
                parentPos = ((AbstractPositionEntity) parent).getUnrotatedPos();
                last = parentPos != null ? 2 : 1;
            }

            float dAlpha = 0f;
            int stIdx = 0;
            int nPoints = pointCloudData.getNumPoints();

            boolean reverse = GaiaSky.instance.time.getWarpFactor() < 0;

            Vector3d bodyPos = aux3d1.get().setZero();
            if (orbitTrail) {
                float top = alpha * 1f;
                float bottom = alpha * -0f;
                dAlpha = (top - bottom) / nPoints;
                Instant currentTime = GaiaSky.instance.time.getTime();
                long wrapTime = pointCloudData.getWrapTimeMs(currentTime);
                stIdx = pointCloudData.getIndex(wrapTime);

                if (body != null) {
                    bodyPos.set(body.translation);
                } else if (oc != null) {
                    oc.loadDataPoint(bodyPos, currentTime);
                    bodyPos.mul(localTransformD);
                }

                if (!reverse) {
                    alpha = bottom;
                    dAlpha = -dAlpha;
                }

            }

            // This is so that the shape renderer does not mess up the z-buffer
            int n = 0;
            int i = wrap(stIdx + 2, nPoints);
            while (n < nPoints - last) {
                // i minus one
                int im = wrap(i - 1, nPoints);

                pointCloudData.loadPoint(prev, im);
                pointCloudData.loadPoint(curr, i);

                if (parentPos != null) {
                    prev.sub(parentPos);
                    curr.sub(parentPos);
                }

                prev.mul(localTransformD);
                curr.mul(localTransformD);

                float calpha = MathUtils.clamp(alpha, 0f, 1f);
                if (orbitTrail && !reverse && n == nPoints - 2) {
                    renderer.addLine(this, (float) curr.x, (float) curr.y, (float) curr.z, (float) bodyPos.x, (float) bodyPos.y, (float) bodyPos.z, cc[0], cc[1], cc[2], calpha * cc[3]);
                } else if (orbitTrail && reverse && n == 0) {
                    renderer.addLine(this, (float) curr.x, (float) curr.y, (float) curr.z, (float) bodyPos.x, (float) bodyPos.y, (float) bodyPos.z, cc[0], cc[1], cc[2], calpha * cc[3]);
                }
                renderer.addLine(this, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], calpha * cc[3]);


                alpha -= dAlpha;

                // advance
                i = wrap(i + 1, nPoints);
                n++;
            }
        }
    }

    private int wrap(int idx, int n) {
        return (((idx % n) + n) % n);
    }

    public void refreshOrbit(boolean force) {
        if ((force && params != null) || (mustRefresh && !body.coordinatesTimeOverflow)) {
            Instant currentTime = GaiaSky.instance.time.getTime();
            long currentMs = currentTime.toEpochMilli();
            if (pointCloudData == null || currentMs < orbitStartMs || currentMs > orbitEndMs) {
                // Schedule for refresh

                // Work out sample initial date
                Date iniTime;
                if (GaiaSky.instance.time.getWarpFactor() >= 0) {
                    // From (now - period) forward (reverse)
                    iniTime = Date.from(Instant.from(currentTime).minusMillis((long) (oc.period * 8640000l)));
                } else {
                    // From now forward
                    iniTime = Date.from(currentTime);
                }
                params.setIni(iniTime);

                // Add to queue
                if (!refreshing) {
                    orbitRefresher.queue(params);
                }
            }
        }
    }

    /**
     * Sets the absolute size of this entity
     *
     * @param size
     */
    public void setSize(Float size) {
        this.size = size * (float) Constants.KM_TO_U;
    }

    public void setPointsize(Long pointsize) {
        this.pointSize = pointsize;
    }

    public void setPointsize(Double pointsize) {
        this.pointSize = pointsize.floatValue();
    }

    public void setPointcolor(double[] color) {
        float[] f = GlobalResources.toFloatArray(color);
        pointColor[0] = f[0];
        pointColor[1] = f[1];
        pointColor[2] = f[2];
        pointColor[3] = f[3];
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider.replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
    }

    public void setOrbit(OrbitComponent oc) {
        this.oc = oc;
    }

    public void setTransformFunction(String transformFunction) {
        if (transformFunction != null && !transformFunction.isEmpty()) {
            try {
                Method m = ClassReflection.getMethod(Coordinates.class, transformFunction);
                this.transformFunction = (Matrix4d) m.invoke(null);
            } catch (Exception e) {
                logger.error(e);
            }

        }
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    public void setBody(CelestialBody body) {
        this.body = body;
        this.distUp = (float) Math.max(this.body.getRadius() * 200, 500 * Constants.KM_TO_U);
        this.distDown = (float) Math.max(this.body.getRadius() * 20, 50 * Constants.KM_TO_U);
    }

    public void setOnlybody(Boolean onlybody) {
        this.onlybody = onlybody;
    }

    public void setNewmethod(Boolean newmethod) {
        this.newmethod = newmethod;
    }

    @Override
    public double getAlpha() {
        return alpha;
    }

    @Override
    public boolean mustAddToIndex() {
        return true;
    }

}
