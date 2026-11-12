/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.record.OrbitComponent;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import net.jafama.FastMath;

import java.time.Instant;

/**
 * Resolves the reference frame of a set of orbital elements into a rotation matrix that takes the
 * cartesian position produced by {@link KeplerianElements} into the equatorial frame of the scene.
 * <p>
 * The frame is defined by a pole and a reference direction. The reference direction is always the
 * ascending node of the plane on the equator of J2000, which is the convention of the JPL satellite
 * element tables, so their longitudes of the ascending node can be used as they are. Only the pole
 * has to be chosen, and that is what {@link OrbitReference} selects.
 * <p>
 * Gaia Sky's internal axes put the fundamental plane in ZX, with +Y towards the north equatorial
 * pole and +Z towards the vernal equinox. A frame with pole <i>p</i> and node <i>n</i> is therefore
 * the matrix whose columns are (n, p x n, p): it maps the classic reference axes onto the scene.
 * For the identity frame that is exactly the permutation that {@link KeplerianElements} applies.
 */
public final class OrbitFrames {
    private static final Log logger = Logger.getLogger(OrbitFrames.class);

    private OrbitFrames() {
    }

    /**
     * Resolves the reference frame for the given elements.
     *
     * @param oc     The orbit component, which carries the reference and the optional explicit pole.
     * @param parent The parent entity, whose equator or orbital plane may be the reference. May be
     *               null, in which case the equatorial plane is used.
     * @param date   The date at which to sample the plane, for the references that are derived from a
     *               trajectory. Poles and planes are constant over the timescales of the scene, so
     *               any date will do; this is only here because evaluating coordinates needs one.
     * @param out    The matrix to write the result into.
     *
     * @return The output matrix, for chaining.
     */
    public static Matrix4D resolve(OrbitComponent oc, Entity parent, Instant date, Matrix4D out) {
        if (oc == null || oc.reference == null || oc.reference == OrbitReference.INHERITED) {
            return out.idt();
        }

        // An explicit pole overrides whatever the reference would derive, so that a plane that the
        // parent cannot supply - the Laplace plane of a satellite, above all - can be given directly.
        if (oc.referencePole != null && oc.referencePole.length >= 2) {
            return fromPole(oc.referencePole[0], oc.referencePole[1], out);
        }

        return switch (oc.reference) {
            case ECLIPTIC -> out.set(Coordinates.eclToEq());
            case PARENT_EQUATOR -> fromPole(parentEquatorPole(parent), out);
            case PARENT_ORBITAL_PLANE -> fromPole(parentOrbitalPole(parent, date), out);
            case LAPLACE -> {
                logger.warn("Orbit uses the LAPLACE reference without a referencePole; "
                                    + "falling back to the equator of the parent. Give the right ascension "
                                    + "and declination of the precession pole to get the plane right.");
                yield fromPole(parentEquatorPole(parent), out);
            }
            case INHERITED -> out.idt();
        };
    }

    /**
     * Builds the frame matrix of a plane, from the right ascension and declination of its pole.
     *
     * @param poleRaDeg  Right ascension of the pole, in degrees.
     * @param poleDecDeg Declination of the pole, in degrees.
     * @param out        The matrix to write the result into.
     *
     * @return The output matrix.
     */
    public static Matrix4D fromPole(double poleRaDeg, double poleDecDeg, Matrix4D out) {
        var pole = Coordinates.sphericalToCartesian(FastMath.toRadians(poleRaDeg), FastMath.toRadians(poleDecDeg), 1.0, new Vector3D());
        return fromPole(pole, out);
    }

    /**
     * Builds the frame matrix of a plane, from its pole.
     *
     * @param pole The unit pole of the plane, in equatorial scene coordinates.
     * @param out  The matrix to write the result into.
     *
     * @return The output matrix.
     */
    public static Matrix4D fromPole(Vector3D pole, Matrix4D out) {
        var p = new Vector3D(pole).nor();
        // Ascending node of the plane on the equator: the direction that is both in the plane and in
        // the equator, crossed from the equatorial pole.
        var node = new Vector3D(0, 1, 0).crs(p);
        if (node.len2() < 1e-18) {
            // The plane is the equator itself, whose node on the equator is undefined: the reference
            // direction is then the vernal equinox, which is the identity.
            return out.idt();
        }
        node.nor();
        var y = new Vector3D(p).crs(node).nor();
        // The keplerian conversion produces (reg y, reg z, reg x) in scene axes: the third regular
        // axis, the pole, lands on +Y, and the first, the node, on +Z. So the columns of this matrix
        // are the in-plane axis, the pole and the node, in that order.
        return out.set(Matrix4D.changeOfBasis(y, p, node));
    }

    /**
     * Pole of the equatorial plane of the parent body, taken from its rotation model. This is the
     * same construction the model updater uses to orient the body, so the orbits end up flattened
     * onto the equator the body is actually drawn with.
     */
    private static Vector3D parentEquatorPole(Entity parent) {
        var pole = new Vector3D(0, 1, 0);
        if (parent == null) {
            return pole;
        }
        var orientation = Mapper.orientation.get(parent);
        var rc = orientation != null ? orientation.rotationComponent : null;
        if (rc != null) {
            var scaffolding = Mapper.modelScaffolding.get(parent);
            var refPlane = Coordinates.getTransformD(scaffolding != null ? scaffolding.refPlaneTransform : null);
            var m = new Matrix4D().idt()
                    .rotate(0, 1, 0, (float) rc.ascendingNode)
                    .mul(refPlane)
                    .rotate(0, 0, 1, (float) (rc.inclination + rc.axialTilt));
            return pole.set(0, 1, 0).mul(m).nor();
        }
        // No rotation model: fall back to the orientation of the parent, which for a body without
        // rotation only carries its reference plane.
        var graph = Mapper.graph.get(parent);
        if (graph != null && graph.orientation != null) {
            return pole.set(0, 1, 0).mul(graph.orientation).nor();
        }
        return pole;
    }

    /**
     * Pole of the orbital plane of the parent body about its own parent, as the normal of its
     * heliocentric (or barycentric) position sampled twice. Sampling the coordinates rather than the
     * elements is deliberate: most bodies of the solar system get their position from a series or a
     * file, and have no elements to read a plane from.
     */
    private static Vector3D parentOrbitalPole(Entity parent, Instant date) {
        var coordinates = parent != null ? Mapper.coordinates.get(parent) : null;
        if (coordinates == null || coordinates.coordinates == null) {
            return parentEquatorPole(parent);
        }
        var period = 0.0;
        var trajectory = Mapper.trajectory.get(parent);
        if (trajectory != null && trajectory.oc != null) {
            period = trajectory.oc.period;
        }
        // A hundredth of a period is a small fraction of the orbit in angle and still large enough
        // that the cross product of the two positions is not dominated by their rounding.
        var dtDays = period > 0 ? Math.min(1.0, period / 100.0) : 1.0;
        dtDays = Math.max(dtDays, 1e-5);

        var q0 = new Vector3Q();
        var q1 = new Vector3Q();
        coordinates.coordinates.getEquatorialCartesianCoordinates(date, q0);
        coordinates.coordinates.getEquatorialCartesianCoordinates(date.plusSeconds((long) (dtDays * 86400.0)), q1);
        var p0 = new Vector3D().set(q0);
        var p1 = new Vector3D().set(q1);
        var pole = new Vector3D(p0).crs(p1);
        if (pole.len2() < 1e-18) {
            return parentEquatorPole(parent);
        }
        return pole.nor();
    }
}
