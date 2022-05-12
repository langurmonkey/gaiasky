package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.data.OrbitRefresher;
import gaiasky.data.orbit.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

import java.time.Instant;
import java.util.Date;

public class Trajectory implements Component {
    public static final Log logger = Logger.getLogger(Trajectory.class);

    public static OrbitRefresher orbitRefresher;

    public enum OrbitOrientationModel {
        DEFAULT,
        EXTRASOLAR_SYSTEM
    }

    /**
     * Special overlap factor
     */
    public static final float SHADER_MODEL_OVERLAP_FACTOR = 20f;

    public Entity body;
    public Vector3d prev, curr;
    public double alpha;
    public Matrix4d localTransformD = new Matrix4d();
    public Matrix4d auxMat;
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

    public void setOrbittrail(Boolean trail) {
        this.orbitTrail = trail;
    }

    public void setBody(Entity entity, double radius) {
        this.body = entity;
        this.distUp = (float) Math.max(radius * 200, 500 * Constants.KM_TO_U);
        this.distDown = (float) Math.max(radius * 20, 50 * Constants.KM_TO_U);
    }

    /**
     * TODO move this function to a system or another more suitable location.
     * Queues a trajectory refresh task with the refresher for this trajectory.
     *
     * @param verts The verts object containing the data.
     * @param force Whether to force the refresh.
     */
    public void refreshOrbit(Verts verts, boolean force) {
        if ((force && params != null) || (mustRefresh && !EntityUtils.isCoordinatesTimeOverflow(body))) {
            Instant currentTime = GaiaSky.instance.time.getTime();
            long currentMs = currentTime.toEpochMilli();
            if (verts.pointCloudData == null || currentMs < orbitStartMs || currentMs > orbitEndMs) {
                // Schedule for refresh
                // Work out sample initial date
                Date iniTime;
                if (GaiaSky.instance.time.getWarpFactor() < 0) {
                    // From (now - period) forward (reverse)
                    iniTime = Date.from(Instant.from(currentTime).minusMillis((long) (oc.period * 80000000L)));
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
}
