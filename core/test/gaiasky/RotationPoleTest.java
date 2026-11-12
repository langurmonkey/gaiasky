/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.scene.record.RotationComponent;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the explicit rotation pole of a body: that the body is oriented with its rotation axis on
 * the pole, that a pole gives the same orientation as the legacy triple of ascending node, reference
 * plane and tilt, and that a body without a pole is left exactly as it was.
 */
public class RotationPoleTest {

    /** IAU poles of the planets, as right ascension and declination in degrees. */
    private static final double[][] IAU_POLES = {
            {281.010, 61.414},   // Mercury
            {272.760, 67.160},   // Venus
            {0.000, 90.000},     // Earth
            {317.681, 52.887},   // Mars
            {268.057, 64.495},   // Jupiter
            {40.589, 83.537},    // Saturn
            {257.311, -15.175},  // Uranus
            {299.360, 43.460},   // Neptune
            {132.993, -6.163}    // Pluto
    };

    private static Vector3D poleVector(double ra, double dec) {
        return Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), 1.0, new Vector3D());
    }

    private static Vector3D yAxis(Matrix4D m) {
        // Column 1 of the matrix, which is where a vector passes through the rotation to land on
        // the Y axis of local space.
        return new Vector3D(m.val[4], m.val[5], m.val[6]);
    }

    private static double maxDifference(Matrix4D a, Matrix4D b) {
        double max = 0;
        for (int i = 0; i < 16; i++) {
            max = Math.max(max, Math.abs(a.val[i] - b.val[i]));
        }
        return max;
    }

    private static RotationComponent withPole(double ra, double dec) {
        var rc = new RotationComponent();
        rc.setPole(new double[]{ra, dec});
        return rc;
    }

    @Test
    public void testTheRotationAxisIsThePole() {
        for (var p : IAU_POLES) {
            var m = withPole(p[0], p[1]).getOrientation(null, new Matrix4D());
            var expected = poleVector(p[0], p[1]);
            var actual = yAxis(m);
            Assert.assertEquals(expected.x, actual.x, 1e-12);
            Assert.assertEquals(expected.y, actual.y, 1e-12);
            Assert.assertEquals(expected.z, actual.z, 1e-12);
            // And it is a proper rotation, so the body is not mirrored.
            var x = new Vector3D(m.val[0], m.val[1], m.val[2]);
            var y = new Vector3D(m.val[4], m.val[5], m.val[6]);
            var z = new Vector3D(m.val[8], m.val[9], m.val[10]);
            Assert.assertEquals(1.0, new Vector3D(x).crs(y).dot(z), 1e-12);
        }
    }

    @Test
    public void testThePoleAgreesWithTheLegacyTriple() {
        // The old representation could reach these poles too, with a node and a tilt solved for the
        // purpose. Both routes must land the body on the same axis, or switching the data over would
        // move the body.
        double[][] legacy = {
                {281.010, 61.414, 11.010, 5.146719},
                {40.589, 83.537, 130.589, -16.976281},
                {257.311, -15.175, 347.311, 81.735719},
                {299.360, 43.460, 29.360, 23.100719}
        };
        for (var l : legacy) {
            var rc = new RotationComponent();
            rc.setAscendingNode(l[2]);
            rc.setInclination(l[3]);
            var fromLegacy = yAxis(rc.getOrientation("ecltoeq", new Matrix4D()));
            var fromPole = yAxis(withPole(l[0], l[1]).getOrientation(null, new Matrix4D()));
            // The legacy chain applies its angles as floats, so this is as close as it gets.
            Assert.assertEquals(fromLegacy.x, fromPole.x, 1e-6);
            Assert.assertEquals(fromLegacy.y, fromPole.y, 1e-6);
            Assert.assertEquals(fromLegacy.z, fromPole.z, 1e-6);
        }
    }

    @Test
    public void testTheLegacyChainIsUnchangedWithoutAPole() {
        // A body that does not declare a pole must be oriented by the old chain, unchanged: node,
        // then the reference plane, then the inclination plus the axial tilt.
        var rc = new RotationComponent();
        rc.setAscendingNode(50.0);
        rc.setInclination(1.85);
        rc.setAxialTilt(10.0);
        Assert.assertFalse(rc.hasExplicitPole());

        var expected = new Matrix4D().idt()
                .rotate(0, 1, 0, 50.0)
                .mul(Coordinates.getTransformD("ecltoeq"))
                .rotate(0, 0, 1, 11.85);
        // The angles of the component are doubles and the legacy chain applies them as floats.
        Assert.assertEquals(0.0, maxDifference(rc.getOrientation("ecltoeq", new Matrix4D()), expected), 1e-6);
    }

    @Test
    public void testTheEquatorialPoleIsTheIdentity() {
        // A body whose axis is the pole of the equator needs no orientation at all, which is what the
        // reference plane and the obliquity used to add up to for the Earth.
        var m = withPole(0.0, 90.0).getOrientation("ecltoeq", new Matrix4D());
        Assert.assertEquals(0.0, maxDifference(m, new Matrix4D().idt()), 1e-12);
    }

    @Test
    public void testCopiesCarryThePole() {
        var rc = withPole(268.057, 64.495);
        rc.setPole(new double[]{268.057, 64.495});
        Assert.assertTrue(rc.copy().hasExplicitPole());
        Assert.assertEquals(268.057, rc.copy().pole[0], 1e-12);

        var other = new RotationComponent();
        other.copyFrom(rc);
        Assert.assertTrue(other.hasExplicitPole());
        Assert.assertEquals(64.495, other.pole[1], 1e-12);

        // A malformed pole is ignored rather than silently taken as a zero-ascension equator pole.
        var bad = new RotationComponent();
        bad.setPole(new double[]{10.0});
        Assert.assertFalse(bad.hasExplicitPole());
    }

    @Test
    public void testRetrogradeRotationSurvivesACopy() {
        // A retrograde body is a negative period about its north pole, and copying a component must
        // not quietly turn it prograde.
        var venus = withPole(272.76, 67.16);
        venus.setPeriod(-5832.443616);
        venus.setMeridianAngle(321.335);
        var copy = new RotationComponent();
        copy.copyFrom(venus);
        Assert.assertEquals(-5832.443616, copy.period, 0.0);
        Assert.assertTrue(copy.angularVelocity < 0);
    }
}
