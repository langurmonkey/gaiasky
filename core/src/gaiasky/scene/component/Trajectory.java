package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.data.orbit.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

public class Trajectory implements Component {
    public static final Log logger = Logger.getLogger(Trajectory.class);

    public enum OrbitOrientationModel {
        DEFAULT,
        EXTRASOLAR_SYSTEM;

        public boolean isDefault() {
            return this.equals(DEFAULT);
        }

        public boolean isExtrasolar() {
            return this.equals(EXTRASOLAR_SYSTEM);
        }
    }

    public Entity body;
    public Vector3d curr;
    public double alpha;
    public Matrix4d localTransformD = new Matrix4d();
    public String provider;
    public Double multiplier = 1.0d;
    public Class<? extends IOrbitDataProvider> providerClass;
    public OrbitComponent oc;
    // Only adds the body, not the orbit
    public boolean onlyBody = false;
    // Use new method for orbital elements
    public boolean newMethod = false;
    // Current orbit completion -- current delta from t0
    public double coord;
    // The orientation model
    public OrbitOrientationModel model = OrbitOrientationModel.DEFAULT;

    public boolean isInOrbitalElementsGroup = false;

    /**
     * Refreshing state
     */
    public boolean refreshing = false;

    /** Number of samples for the orbit data provider. **/
    public int numSamples = 100;

    public long orbitStartMs, orbitEndMs;

    /**
     * Whether the orbit must be refreshed when out of bounds
     */
    public boolean mustRefresh;
    /**
     * Whether to show the orbit as a trail or not.
     * A trail fades the orbit line as it gets further away from the object.
     */
    public boolean orbitTrail;
    /**
     * The bottom mapping position for the trail. The orbit trail assigns
     * an opacity value to each point of the orbit, where 1 is the location of
     * the object and 0 is the other end. This mapping parameter defines the location
     * in the orbit (in [0,1]) where we map the opacity value of 0.
     * Set to 0 to have a full trail. Set to 0.5 to have a trail that spans half the orbit.
     * Set to 1 to have no orbit at all.
     */
    public float trailMap = 0.0f;

    public OrbitDataLoaderParameters params;

    /**
     * Point color
     **/
    public float[] pointColor;

    /**
     * Point size
     **/
    public float pointSize = 1f;

    public float distUp, distDown;

    /**
     * Sets the orientation model as a string.
     *
     * @param model The orientation model.
     */
    public void setModel(String model) {
        model = model.toUpperCase().trim();
        try {
            this.model = OrbitOrientationModel.valueOf(model);
        } catch (IllegalArgumentException e) {
            logger.error(I18n.msg("notif.error", e.getLocalizedMessage()));
        }
    }

    public void setPointsize(Long pointsize) {
        this.pointSize = pointsize;
    }

    public void setPointsize(Double pointsize) {
        this.pointSize = pointsize.floatValue();
    }

    public void setPointcolor(double[] color) {
        pointColor = GlobalResources.toFloatArray(color);
    }

    public void setProvider(String provider) {
        this.provider = provider.replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
    }

    public void setOrbit(OrbitComponent oc) {
        this.oc = oc;
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    public void setOnlybody(Boolean onlyBody) {
        this.onlyBody = onlyBody;
    }

    public void setNewmethod(Boolean newMethod) {
        this.newMethod = newMethod;
    }

    public void setTrail(Boolean trail) {
        this.orbitTrail = trail;
    }

    public void setTrailMap(Double trailMap) {
        this.trailMap = MathUtils.clamp(trailMap.floatValue(), 0f, 1f);
    }

    public void setOrbittrail(Boolean trail) {
        this.orbitTrail = trail;
    }

    public void setNumSamples(Long numSamples) {
        this.numSamples = Math.toIntExact(numSamples);
    }

    public void setBody(Entity entity, double radius) {
        this.body = entity;
        this.distUp = (float) Math.max(radius * 200, 500 * Constants.KM_TO_U);
        this.distDown = (float) Math.max(radius * 20, 50 * Constants.KM_TO_U);
    }

}
