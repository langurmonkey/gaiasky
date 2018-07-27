package gaia.cu9.ari.gaiaorbit.scenegraph;

import java.time.Instant;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.assets.OrbitDataLoader;
import gaia.cu9.ari.gaiaorbit.data.orbit.IOrbitDataProvider;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.OrbitComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class Orbit extends Polyline {

    /** Threshold angle **/
    protected static final float ANGLE_LIMIT = (float) Math.toRadians(1.5);
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

    /** Orbital elements in gpu, in case there is no body **/
    public boolean elemsInGpu = false;

    private float distUp, distDown;

    public Orbit() {
        super();
        localTransform = new Matrix4();
        localTransformD = new Matrix4d();
        prev = new Vector3d();
        curr = new Vector3d();
    }

    @Override
    public void initialize() {
        if (!onlybody)
            try {
                providerClass = (Class<? extends IOrbitDataProvider>) ClassReflection.forName(provider);
                // Orbit data
                IOrbitDataProvider provider;
                try {
                    provider = ClassReflection.newInstance(providerClass);
                    provider.load(oc.source, new OrbitDataLoader.OrbitDataLoaderParameter(name, providerClass, oc, multiplier, 100), newmethod);
                    polylineData = provider.getData();
                } catch (Exception e) {
                    Logger.error(e, getClass().getSimpleName());
                }
            } catch (ReflectionException e) {
                Logger.error(e, getClass().getSimpleName());
            }
    }

    @Override
    public void doneLoading(AssetManager manager) {
        alpha = cc[3];
        if (!onlybody) {
            int last = polylineData.getNumPoints() - 1;
            Vector3d v = new Vector3d(polylineData.x.get(last), polylineData.y.get(last), polylineData.z.get(last));
            this.size = (float) v.len() * 5;
        } else {

        }
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);
        if (!onlybody)
            updateLocalTransform(time.getTime());
    }

    protected void updateLocalTransform(Instant date) {
        transform.getMatrix(localTransformD);
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
            float angleLimit = ANGLE_LIMIT * camera.getFovFactor();
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
                } else if (body != null && body.distToCamera > distDown) {
                    // Body, disappear slowly
                    if (body.distToCamera < distUp)
                        this.alpha *= MathUtilsd.lint(body.distToCamera, distDown, distUp, 0, 1);
                    addToRender(this, rg);
                }
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

            // Make origin Gaia
            Vector3d parentPos = null;
            if (parent instanceof Gaia) {
                parentPos = ((Gaia) parent).unrotatedPos;
            }

            // This is so that the shape renderer does not mess up the z-buffer
            for (int i = 1; i < polylineData.getNumPoints(); i++) {
                polylineData.loadPoint(prev, i - 1);
                polylineData.loadPoint(curr, i);

                if (parentPos != null) {
                    prev.sub(parentPos);
                    curr.sub(parentPos);
                }

                prev.mul(localTransformD);
                curr.mul(localTransformD);

                renderer.addLine(this, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], alpha * cc[3]);

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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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
                Logger.error(e);
            }

        }
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    public void setBody(CelestialBody body) {
        this.body = body;
        this.distUp = (float) Math.max(this.body.getRadius() * 200, 1000 * Constants.KM_TO_U);
        this.distDown = (float) Math.max(this.body.getRadius() * 50, 100 * Constants.KM_TO_U);
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


}
