/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.elements;

import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;

/**
 * Represent and manipulate osculating elements of the elliptic two-body
 * problem. The class supports the representation in elliptical elements, or in
 * position-velocity. Methods perform the transformation in both directions,
 * solve the Kepler equation, propagate the motion to another date. <br> By
 * default the central body is the Sun and the units are {@code AU},
 * <code>AU day<sup>-1</sup></code>. The default {@code GM} (gravitational
 * constant times mass) can be overridden by using the constructors taking
 * {@code mu} as an argument.
 *
 * @author <a href="mailto:francois.mignard@obs-nice.fr">Francois Mignard</a>
 * @author <a href="mailto:gilles.sadowski@ulb.ac.be">Gilles Sadowski (GS)</a>
 * @version $Id$
 */
public class OsculatingElements {
    public static final double GAUSS_CONSTANT = 0.01720209895;
    /** GM for the Sun (<code>k<sup>2</sup> = gaia.cu1.params.GaiaParam.Nature#GAUSS_CONSTANT</code>). */
    private static final double MU_SUN = (GAUSS_CONSTANT * GAUSS_CONSTANT);

    /** The Constant TWO_PI. */
    private static final double TWO_PI = 2 * Math.PI;

    /** The Constant PI_TWO. */
    private static final double PI_TWO = Math.PI / 2;

    /** Eccentricity below which the orbit is considered circular. */
    private static final double E_CIRCLE_LIMIT = 1e-6;

    /** For solving Kepler equation. */
    private static final KeplerSolver KEPLER = new KeplerSolver();

    /** Semi-major axis. Unit determined by the choice of {@code mu = GM}. */
    private final double semiaxis;

    /** Eccentricity. */
    private final double eccent;

    /** Inclination in radians. */
    private final double gamma;

    /** Longitude of node in radians (bomega = Big Omega). */
    private final double bomega;

    /** Argument of pericenter in radians (somega = Small Omega). */
    private final double somega;

    /** Mean anomaly in radians. */
    private final double anom;

    /** Gravitational constant * mass = GM. */
    private final double mu;

    /** Position vector at pericenter. */
    private Vector3d posperi;

    /** Velocity vector at pericenter. */
    private Vector3d velperi;

    /**
     * The Constructor.
     *
     * @param semiaxis Semi-major axis.
     * @param eccent   Eccentricity.
     * @param gamma    Inclination (units: {@code rad}).
     * @param bomega   Longitude of node (units: {@code rad}).
     * @param somega   Argument of periastron (units: {@code rad}).
     * @param anom     Mean anomaly (units: {@code rad}).
     * @param mu       {@code GM} for the central body.
     */
    public OsculatingElements(final double semiaxis,
                              final double eccent,
                              final double gamma,
                              final double bomega,
                              final double somega,
                              final double anom,
                              final double mu) {
        this.semiaxis = semiaxis;
        this.eccent = eccent;
        this.gamma = gamma;
        this.bomega = bomega;
        this.somega = somega;
        this.anom = anom;
        this.mu = mu;

        // Compute pericenter position and velocity
        this.toPericenter();
    }

    /**
     * The Constructor.
     *
     * @param elliptic Six elliptic elements, in the following order: a, e, i, omega,
     *                 pi, M.
     * @param mu       {@code GM} for the central body.
     *
     * @throws IllegalArgumentException if the length of {@code} is different from 6.
     */
    public OsculatingElements(final double[] elliptic,
                              final double mu) {
        this(elliptic[0], elliptic[1], elliptic[2], elliptic[3], elliptic[4],
             elliptic[5], mu);

        if (elliptic.length != 6) {
            throw new IllegalArgumentException(
                    "Wrong number of elements: " + elliptic.length);
        }
    }

    /**
     * Assuming the Sun is the central body.
     *
     * @param elliptic Six elliptic elements, in the following order: a, e, i, omega,
     *                 pi, M.
     */
    public OsculatingElements(final double[] elliptic) {
        this(elliptic, OsculatingElements.MU_SUN);
    }

    /**
     * Assuming the Sun is the central body.
     *
     * @param semiaxis Semi-major axis.
     * @param eccent   Eccentricity.
     * @param gamma    Inclination (units: {@code rad}).
     * @param bomega   Longitude of node (units: {@code rad}).
     * @param somega   Argument of periastron (units: {@code rad}).
     * @param anom     Mean anomaly (units: {@code rad}).
     */
    public OsculatingElements(final double semiaxis,
                              final double eccent,
                              final double gamma,
                              final double bomega,
                              final double somega,
                              final double anom) {
        this(semiaxis, eccent, gamma, bomega, somega, anom,
             OsculatingElements.MU_SUN);
    }

    /**
     * Create elliptic elements from position-velocity, assuming the Sun as the
     * central body.
     *
     * @param pos Position.
     * @param vel Velocity.
     */
    public OsculatingElements(final Vector3d pos,
                              final Vector3d vel) {
        this(OsculatingElements.MU_SUN, pos, vel);
    }

    /**
     * Create elliptic elements from position-velocity.
     *
     * @param mu  GM.
     * @param pos Position.
     * @param vel Velocity.
     */
    public OsculatingElements(final double mu,
                              final Vector3d pos,
                              final Vector3d vel) {
        this(OsculatingElements.toElliptic(mu, pos, vel), mu);
    }

    /**
     * Compute elliptic elements corresponding to the given Cartesian
     * representation.
     *
     * @param mu  GM.
     * @param pos Position.
     * @param vel Velocity.
     *
     * @return the six elliptic elements in an array (order: a, e, i, node,
     * pericenter, mean anomaly).
     */
    public static double[] toElliptic(final double mu,
                                      final Vector3d pos,
                                      final Vector3d vel) {
        final double v2 = vel.len2();
        final double r = pos.len();

        // Angular momentum.
        final Vector3d sigma = Vector3d.cross(pos, vel);
        final double c = sigma.len();

        // Runge vector.
        final Vector3d runge = Vector3d.cross(vel, sigma).scaleAdd(-mu / r,
                                                                   pos);

        // Semi-major axis and eccentricity.
        final double semiaxis = mu / (((2 * mu) / r) - v2);
        double eccent = Math.sqrt(Math.abs(1.0 - ((c * c) / (mu * semiaxis))));

        // Node and inclination.
        double bomega = sigma.getLongitude();
        double gamma = sigma.getLatitude();

        bomega += OsculatingElements.PI_TWO;
        bomega %= OsculatingElements.TWO_PI;
        gamma = OsculatingElements.PI_TWO - gamma;

        if (gamma == 0.0) { // zero inclination orbit.
            // conventional longitude of node for zero inclination.
            bomega = 0.0;
        }

        // Argument of periastron.
        double somega;

        if (gamma == 0) { // zero inclination => longitude of periastron.
            somega = Math.atan2(runge.y(), runge.x());
        } else {
            somega = Math.atan2(
                    runge.z() * c,
                    (runge.y() * sigma.x()) - (runge.x() * sigma.y()));
        }

        if (eccent < OsculatingElements.E_CIRCLE_LIMIT) { // circular orbit.
            somega = 0.0; // conventional periastron.
        }

        somega %= OsculatingElements.TWO_PI;

        // Anomalies
        final double anomv = Math.atan2(
                -c / mu * vel.dot(runge),
                pos.dot(runge) / r);
        final double anome = Anomalies.true2ecc(anomv, eccent);
        double anom = (anome - (eccent * Math.sin(anome))) % OsculatingElements.TWO_PI;

        if (eccent < OsculatingElements.E_CIRCLE_LIMIT) { // circular orbit:
            // somega = 0.

            final double com = Math.cos(bomega);
            final double som = Math.sin(bomega);
            eccent = 0.0;
            anom = Math.atan2(
                    ((pos.y() * com) - (pos.x() * som)) / sigma.z() * c,
                    (pos.x() * com) + (pos.y() * som));
        }

        return new double[] { semiaxis, eccent, gamma, bomega, somega, anom };
    }

    /**
     * Transform elliptic element into position and velocity vector.
     * <p>
     * Units are set by the choice of {@code GM}. By default
     * <code>GM = k<sup>2</sup></code>; hence, the units are {@code AU},
     * and <code>AU day<sup>-1</sup></code> ({@code k = GAUSS_CONSTANT}).
     * Another choice can be <code>GM = 4 pi<sup>2</sup></code>; hence, the
     * units will be {@code AU}, <code>AU year<sup>-1</sup></code>. For an
     * Earth satellite: {@code GM = EARTH_GM} with units {@code m} and
     * <code>m s<sup>-1</sup></code>.
     *
     * @return a Vector3d[2] with position in [0] and velocity in [1].
     */
    public Vector3d[] toCartesian() {
        // Kepler equation.
        final double xk = Math.sqrt(this.mu);
        final double xn = xk / (this.semiaxis * Math.sqrt(this.semiaxis)); // radians
        // per
        // unit
        // of
        // time.

        final double anex = Anomalies.mean2ecc(
                OsculatingElements.KEPLER,
                this.anom,
                this.eccent);

        // Auxiliary data.
        final double ce = Math.cos(anex);
        final double se = Math.sin(anex);

        final double eccentce = this.eccent * ce;
        final double rsura = 1.0 - eccentce;
        final double ca = (ce - this.eccent) / (1 - eccentce);
        final double sqrt = Math.sqrt(
                (1.0 - this.eccent) * (1.0 + this.eccent));
        final double sa = (sqrt * se) / (1.0 - eccentce);

        // Position and velocity vectors in the orbit.
        final double semirsura = this.semiaxis * rsura;
        final double xnsemi = xn * this.semiaxis;
        final Vector3d pos = new Vector3d(semirsura * ca, semirsura * sa, 0.0);
        final Vector3d vel = new Vector3d(-xnsemi / rsura * se,
                                          xnsemi / rsura * sqrt * ce, 0.0);

        // Position and velocity vector in the reference axes.
        pos.rotateRad(this.somega, 0, 0, 1);
        pos.rotateRad(this.gamma, 1, 0, 0);
        pos.rotateRad(this.bomega, 0, 0, 1);

        vel.rotateRad(this.somega, 0, 0, 1);
        vel.rotateRad(this.gamma, 1, 0, 0);
        vel.rotateRad(this.bomega, 0, 0, 1);

        return new Vector3d[] { pos, vel };
    }

    /**
     * Advance a position-velocity vector at a new mean anomaly. It is optimized
     * for quick computation in the final reference frame. See
     * {@link #toCartesian() toCartesian} for a discussion on the units.
     *
     * @param xanom Mean anomaly at which the motion is propagated (units:
     *              {@code rad}).
     *
     * @return a Vector3d[2] with position in [0] and velocity in [1].
     */
    public Vector3d[] advanceTwoBody(final double xanom) {
        final double xk = Math.sqrt(this.mu);
        final double xn = xk / this.semiaxis / Math.sqrt(this.semiaxis); // radians
        // per
        // unit
        // of
        // time

        final double anex = Anomalies.mean2ecc(
                OsculatingElements.KEPLER,
                xanom,
                this.eccent);

        final double ume = 1.0 - this.eccent;
        final double ce = Math.cos(anex);
        final double se = Math.sin(anex);

        final double f = (ce - this.eccent) / ume;
        final double g = (ume * se) / xn;
        final Vector3d pos = Vector3d.scale(g, this.velperi).add(
                Vector3d.scale(f, this.posperi));

        final double fp = (-xn * se) / (1.0 - (this.eccent * ce)) / ume;
        final double gp = (ce * ume) / (1.0 - (this.eccent * ce));
        final Vector3d vel = Vector3d.scale(gp, this.velperi).add(
                Vector3d.scale(fp, this.posperi));

        return new Vector3d[] { pos, vel };
    }

    /**
     * Gets the elements.
     *
     * @return the six elliptic elements of this object in an array (order: a,
     * e, i, node, pericenter, mean anomaly).
     */
    public double[] getElements() {
        return new double[] {
                this.semiaxis, this.eccent, this.gamma, this.bomega, this.somega,
                this.anom
        };
    }

    /**
     * Gets the semi major axis.
     *
     * @return the semi-major axis.
     */
    public double getSemiMajorAxis() {
        return this.semiaxis;
    }

    /**
     * Gets the eccentricity.
     *
     * @return the eccentricity.
     */
    public double getEccentricity() {
        return this.eccent;
    }

    /**
     * Gets the inclination.
     *
     * @return the inclination (units: {@code rad}).
     */
    public double getInclination() {
        return this.gamma;
    }

    /**
     * Gets the node.
     *
     * @return the longitude of node (units: {@code rad}).
     */
    public double getNode() {
        return this.bomega;
    }

    /**
     * Gets the pericenter.
     *
     * @return the argument of pericenter (units: {@code rad}).
     */
    public double getPericenter() {
        return this.somega;
    }

    /**
     * Gets the mean anomaly.
     *
     * @return the mean anomaly (units: {@code rad}).
     */
    public double getMeanAnomaly() {
        return this.anom;
    }

    /**
     * Gets the mu.
     *
     * @return the {@code GM}.
     */
    public double getMu() {
        return this.mu;
    }

    /**
     * Gets the position at pericenter.
     *
     * @return the position at pericenter.
     */
    public Vector3d getPositionAtPericenter() {
        return this.posperi;
    }

    /**
     * Gets the velocity at pericenter.
     *
     * @return the velocity at pericenter.
     */
    public Vector3d getVelocityAtPericenter() {
        return this.velperi;
    }

    /**
     * Transform ellitic element into position and velocity vector at
     * pericenter. This must be called once before propagation with
     * {@link #advanceTwoBody(double) advanceTwoBody}. See
     * {@link #toCartesian() toCartesian} for a discussion on the units.
     */
    private void toPericenter() {
        // Kepler equation
        final double xk = Math.sqrt(this.mu);
        final double xn = xk / (this.semiaxis * Math.sqrt(this.semiaxis)); // radians
        // per
        // unit
        // of
        // time

        final double rsura = 1.0 - this.eccent;

        // position and velocity vectors in the orbit
        this.posperi = new Vector3d(this.semiaxis * rsura, 0.0, 0.0);

        final double y = xn * this.semiaxis * Math.sqrt(
                (1.0 + this.eccent) / (1.0 - this.eccent));
        this.velperi = new Vector3d(0.0, y, 0.0);

        // position and velocity vector in the reference axes
        this.posperi.rotateRad(this.somega, 0, 0, 1);
        this.posperi.rotateRad(this.gamma, 1, 0, 0);
        this.posperi.rotateRad(this.bomega, 0, 0, 1);
        this.velperi.rotateRad(this.somega, 0, 0, 1);
        this.velperi.rotateRad(this.gamma, 1, 0, 0);
        this.velperi.rotateRad(this.bomega, 0, 0, 1);
    }
}
