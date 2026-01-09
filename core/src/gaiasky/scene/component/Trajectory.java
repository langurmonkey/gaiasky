/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.scene.record.OrbitComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

import java.util.Locale;

public class Trajectory implements Component {
    public static final Log logger = Logger.getLogger(Trajectory.class);

    /**
     * The entity that employs this trajectory/orbit. This is **not** the orbit object, but the
     * actual planet/moon/spacecraft object.
     **/
    public Entity body;

    public Vector3D curr;
    public double alpha;
    public Matrix4D localTransformD = new Matrix4D();
    public String provider;
    public Double multiplier = 1.0d;
    public Class<? extends IOrbitDataProvider> providerClass;
    public IOrbitDataProvider providerInstance;
    /** Orbit component object. **/
    public OrbitComponent oc;

    /**
     * Control the body/trajectory representation for this object. Note that the body can only be represented
     * when using orbital elements.
     */
    public OrbitBodyRepresentation bodyRepresentation = OrbitBodyRepresentation.BODY_AND_ORBIT;

    /**
     * Changes the way in which transformations are applied to the orbit objects.
     * Asteroids have this set to true.
     */
    public boolean newMethod = false;
    /** Current orbit completion -- current delta from t0. **/
    public double coord;
    /** The orientation model. **/
    public OrbitOrientationModel model = OrbitOrientationModel.DEFAULT;
    public boolean isInOrbitalElementsGroup = false;
    /**
     * Refreshing state
     */
    public boolean refreshing = false;
    /**
     * Number of samples for the orbit data provider.
     **/
    public int numSamples = 200;

    /**
     * Sampling strategy for the orbit component.
     */
    public enum OrbitSamplingStrategy {
        /** Orbit sampling is done uniformly in time. **/
        TIME,
        /** Orbit sampling is done uniformly in nu (true anomaly). **/
        NU
    }

    /** Strategy to use to sample the orbit. **/
    public OrbitSamplingStrategy sampling = OrbitSamplingStrategy.TIME;

    public long orbitStartMs, orbitEndMs;
    /**
     * Whether the orbit must be refreshed when out of bounds
     */
    public boolean mustRefresh;

    /**
     * Whether to close the trajectory (connect end point to start point) or not
     **/
    public boolean closedLoop = true;

    /**
     * Whether to show the orbit as a trail or not.
     * A trail fades the orbit line as it gets further away from the object.
     */
    public boolean orbitTrail = true;

    /**
     * The bottom mapping position for the trail. The orbit trail assigns
     * an opacity value to each point of the orbit, where 1 is the location of
     * the object and 0 is the other end. This mapping parameter defines the location
     * in the orbit (in [0,1]) where we map the opacity value of 0.
     * Set to 0 to have a full trail. Set to 0.5 to have a trail that spans half the orbit.
     * Set to 1 to have no orbit at all.
     */
    public float trailMap = 0.0f;

    /**
     * Minimum opacity value of the trail. Set this > 0 to raise the global opacity level
     * of the orbit. Effectively, the opacity of the orbit will be mapped from this value
     * to 1.
     */
    public float trailMinOpacity = 0.0f;

    public OrbitDataLoaderParameters params;
    /**
     * Body color. Color to use to represent the body in orbital elements trajectories, when the
     * {@link #bodyRepresentation}
     * attribute enables the representation of the body for this trajectory.
     **/
    public float[] bodyColor;
    /**
     * Point size
     **/
    public float pointSize = 1f;

    /**
     * Orbits with a body fade out as the camera get closer to the body.
     * This is the far distance, in body radius units, where the orbit starts the fade (mapped to 1).
     * This attribute only has effect if this trajectory has a body.
     **/
    public float distUp = 200;

    /**
     * Orbits with a body fade out as the camera get closer to the body.
     * This is the near distance, in body radius units, where the orbit ends the fade (mapped to 0).
     * This attribute only has effect if this trajectory has a body.
     **/
    public float distDown = 20;

    /**
     * For orbits that need to be refreshed (i.e. not implemented as orbital elements, but via samples),
     * this is the orbit refresh rate, in [0,1]. Set to 0 to recompute only every period, and set to 1 to recompute as often
     * as possible. Set to negative to use the default re-computation heuristic.
     * This can help reduce the seams between the trajectory lines computed in the past cycle and the current cycle in
     * orbits which are very open.
     */
    public double refreshRate = -1.0;

    /**
     * Sets the orientation model as a string.
     *
     * @param model The orientation model.
     */
    public void setOrientationModel(String model) {
        model = model.toUpperCase().trim();
        try {
            this.model = OrbitOrientationModel.valueOf(model);
        } catch (IllegalArgumentException e) {
            logger.error(I18n.msg("notif.error", e.getLocalizedMessage()));
        }
    }

    public void setModel(String model) {
        setOrientationModel(model);
    }

    public void setPointSize(Long pointSize) {
        this.pointSize = pointSize;
    }

    public void setPointsize(Long pointSize) {
        setPointSize(pointSize);
    }

    public void setPointSize(Double pointSize) {
        this.pointSize = pointSize.floatValue();
    }

    public void setPointsize(Double pointSize) {
        setPointSize(pointSize);
    }

    public void setBodyColor(double[] color) {
        bodyColor = GlobalResources.toFloatArray(color);
    }

    public void setPointColor(double[] color) {
        setBodyColor(color);
    }

    public void setPointcolor(double[] color) {
        setPointColor(color);
    }

    public void setClosedLoop(Boolean closedLoop) {

    }

    public void setOrbitProvider(String provider) {
        this.provider = provider.replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
    }

    public void setProvider(String provider) {
        setOrbitProvider(provider);
    }


    public void setOrbit(OrbitComponent oc) {
        this.oc = oc;
    }

    public void setOrbitScaleFactor(Double scaleFactor) {
        this.multiplier = scaleFactor;
    }

    public void setMultiplier(Double scaleFactor) {
        this.setOrbitScaleFactor(scaleFactor);
    }

    /**
     * Mutes the orbit line in orbital elements trajectories.
     *
     * @param onlyBody Whether to display only the body for this trajectory.
     *
     * @deprecated Use {{@link #setBodyRepresentation(String)}} instead.
     */
    @Deprecated
    public void setOnlyBody(Boolean onlyBody) {
        if (onlyBody) {
            bodyRepresentation = OrbitBodyRepresentation.ONLY_BODY;
        } else {
            bodyRepresentation = OrbitBodyRepresentation.BODY_AND_ORBIT;
        }
    }

    /**
     * Alias method, @see {@link #setOnlyBody(Boolean)}.
     *
     * @deprecated Use {{@link #setBodyRepresentation(String)}} instead.
     */
    @Deprecated
    public void setOnlybody(Boolean onlyBody) {
        this.setOnlyBody(onlyBody);
    }

    public boolean isOnlyBody() {
        return bodyRepresentation.isOnlyBody();
    }

    /**
     * Sets the body representation for this trajectory.
     *
     * @param representation The body representation model. See {@link OrbitBodyRepresentation} for more information.
     */
    public void setBodyRepresentation(String representation) {
        representation = representation.toUpperCase().trim();
        try {
            this.bodyRepresentation = OrbitBodyRepresentation.valueOf(representation);
        } catch (IllegalArgumentException e) {
            logger.error(I18n.msg("notif.error", e.getLocalizedMessage()));
        }
    }

    public void setNewmethod(Boolean newMethod) {
        this.newMethod = newMethod;
    }

    public void setTrail(Boolean trail) {
        this.orbitTrail = trail;
    }

    public void setTrailMap(Double trailMap) {
        this.trailMap = MathUtils.clamp(trailMap.floatValue(), 0f, 1.0f);
    }

    public void setTrailMinOpacity(Double trailMin) {
        this.trailMinOpacity = MathUtils.clamp(trailMin.floatValue(), 0f, 1.0f);
    }

    public void setOrbittrail(Boolean trail) {
        this.orbitTrail = trail;
    }

    public void setNumSamples(Long numSamples) {
        this.numSamples = FastMath.toIntExact(numSamples);
    }

    public void setSampling(String sampling) {
        try {
            this.sampling = OrbitSamplingStrategy.valueOf(sampling.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.error("Unknown orbit sampling value: " + sampling, e);
        }
    }


    public void setFadeDistanceUp(Double distUp) {
        this.distUp = distUp.floatValue();
    }

    public void setFadeDistanceDown(Double distDown) {
        this.distDown = distDown.floatValue();
    }

    public void setRefreshRate(Double refhreshRate) {
        this.refreshRate = refhreshRate;
    }

    public void setBody(Entity entity, double radius) {
        setBody(entity, radius, 20, 200);
    }

    public void setBody(Entity entity, double radius, float distDown, float distUp) {
        this.body = entity;
        this.distUp = (float) FastMath.max(radius * distUp, 500 * Constants.KM_TO_U);
        this.distDown = (float) FastMath.max(radius * distDown, 50 * Constants.KM_TO_U);
    }

    /**
     * Orientation model for this orbit/trajectory.
     */
    public enum OrbitOrientationModel {
        /**
         * Equatorial plane is the reference plane, and aries (vernal equinox) is the reference direction.
         */
        DEFAULT,

        /**
         * Extrasolar systems are typically specified using elements in a special reference system. In this reference
         * system, the reference plane is the plane whose normal is the line of sight vector from the Sun to the planet
         * or
         * star for whom the orbit is defined. The reference direction is the direction from the object to the north
         * celestial pole projected on the reference plane.
         */
        EXTRASOLAR_SYSTEM;

        public boolean isDefault() {
            return this.equals(DEFAULT);
        }

        public boolean isExtrasolar() {
            return this.equals(EXTRASOLAR_SYSTEM);
        }
    }

    /**
     * The body representation type for this orbit/trajectory. This only works with orbits defined via orbital
     * elements.
     */
    public enum OrbitBodyRepresentation {
        /**
         * Body is not visually represented at all.
         */
        ONLY_ORBIT,

        /**
         * Only the body is visually represented. No orbit/trajectory line is present.
         */
        ONLY_BODY,

        /**
         * Both body and orbit/trajectory line are visually represented.
         */
        BODY_AND_ORBIT;

        public boolean isOnlyBody() {
            return this.equals(ONLY_BODY);
        }

        public boolean isOnlyOrbit() {
            return this.equals(ONLY_ORBIT);
        }

        public boolean isBodyAndOrbit() {
            return this.equals(BODY_AND_ORBIT);
        }

        public boolean isOrbit() {
            return isOnlyOrbit() || isBodyAndOrbit();
        }

        public boolean isBody() {
            return isOnlyBody() || isBodyAndOrbit();
        }
    }
}
