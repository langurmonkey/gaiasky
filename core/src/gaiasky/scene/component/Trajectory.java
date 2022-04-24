package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import gaiasky.data.orbit.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

public class Trajectory implements Component {
    private static final Log logger = Logger.getLogger(Trajectory.class);

    private enum OrientationModel {
        DEFAULT,
        EXTRASOLAR_SYSTEM
    }

    /**
     * Special overlap factor
     */
    protected static final float SHADER_MODEL_OVERLAP_FACTOR = 20f;

    protected CelestialBody body;
    protected Vector3d prev, curr;
    public double alpha;
    public Matrix4d localTransformD, auxMat;
    protected String provider;
    protected Double multiplier = 1.0d;
    protected Class<? extends IOrbitDataProvider> providerClass;
    public OrbitComponent oc;
    // Only adds the body, not the orbit
    protected boolean onlyBody = false;
    // Use new method for orbital elements
    public boolean newMethod = false;
    // Current orbit completion -- current delta from t0
    public double coord;
    // The orientation model
    public OrientationModel model = OrientationModel.DEFAULT;

    public boolean isInOrbitalElementsGroup = false;

    /**
     * Refreshing state
     */
    public boolean refreshing = false;

    public long orbitStartMs, orbitEndMs;

    /**
     * Whether the orbit must be refreshed when out of bounds
     */
    public boolean mustRefresh;
    /**
     * Whether to show the orbit as a trail or not
     */
    public boolean orbitTrail;
    public OrbitDataLoaderParameter params;

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
            this.model = OrientationModel.valueOf(model);
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

    public void setOrbittrail(Boolean trail) {
        this.orbitTrail = trail;
    }
}
