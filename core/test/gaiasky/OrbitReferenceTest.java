/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.scene.record.OrbitComponent;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.OrbitFrames;
import gaiasky.util.coord.OrbitReference;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

/**
 * Tests the resolution of the reference frame of the orbital elements: that the explicit references
 * reproduce the planes they name, that the node is where the JPL tables measure it from, and that the
 * inherited reference stays the identity so that nothing that does not opt in changes.
 */
public class OrbitReferenceTest {

    private static final Instant EPOCH = Instant.ofEpochMilli(946728000000L);

    /** The pole of the ecliptic of J2000, in equatorial scene coordinates. */
    private static Vector3D eclipticPole() {
        double obliquity = Math.toRadians(Coordinates.OBLIQUITY_DEG_J2000);
        return new Vector3D(-Math.sin(obliquity), Math.cos(obliquity), 0.0);
    }

    private static double maxDifference(Matrix4D a, Matrix4D b) {
        double max = 0;
        for (int i = 0; i < 16; i++) {
            max = Math.max(max, Math.abs(a.val[i] - b.val[i]));
        }
        return max;
    }

    @Test
    public void testEclipticMatchesTheTransformFunction() {
        // The ecliptic reference must produce exactly what transformFunction: eclipticToEquatorial
        // produced before the property existed, or the two ways of saying the same thing would
        // disagree.
        var oc = new OrbitComponent();
        oc.setReference("ECLIPTIC");
        var m = OrbitFrames.resolve(oc, null, EPOCH, new Matrix4D());
        Assert.assertEquals(0.0, maxDifference(m, Coordinates.eclToEq()), 1e-12);
    }

    @Test
    public void testInheritedIsTheIdentity() {
        var oc = new OrbitComponent();
        Assert.assertEquals(OrbitReference.INHERITED, oc.reference);
        var m = OrbitFrames.resolve(oc, null, EPOCH, new Matrix4D());
        Assert.assertEquals(0.0, maxDifference(m, new Matrix4D().idt()), 1e-14);
        Assert.assertFalse(oc.hasExplicitReference());
    }

    @Test
    public void testPoleAndNodeLandWhereTheKeplerianFrameExpectsThem() {
        // The keplerian conversion returns (reg y, reg z, reg x) in scene axes, so the pole of the
        // plane is carried by +Y and the direction the node is measured from by +Z. Those are the
        // two axes this matrix has to get right, or a plane's tilt would be applied as a rotation
        // within the plane.
        double ra = 268.057;
        double dec = 64.495;
        var pole = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), 1.0, new Vector3D());
        var m = OrbitFrames.fromPole(ra, dec, new Matrix4D());

        var poleOut = new Vector3D(0, 1, 0).mul(m);
        Assert.assertEquals(pole.x, poleOut.x, 1e-12);
        Assert.assertEquals(pole.y, poleOut.y, 1e-12);
        Assert.assertEquals(pole.z, poleOut.z, 1e-12);

        var nodeOut = new Vector3D(0, 0, 1).mul(m);
        var expectedNode = new Vector3D(0, 1, 0).crs(pole).nor();
        Assert.assertEquals(expectedNode.x, nodeOut.x, 1e-12);
        Assert.assertEquals(expectedNode.y, nodeOut.y, 1e-12);
        Assert.assertEquals(expectedNode.z, nodeOut.z, 1e-12);
        // The node has to be on the equator, and at right angles to the pole.
        Assert.assertEquals(0.0, nodeOut.y, 1e-12);
        Assert.assertEquals(0.0, nodeOut.dot(poleOut), 1e-12);
    }

    @Test
    public void testTheEclipticPoleProducesTheEclipticTransform() {
        // The same frame built from the pole of the ecliptic must reproduce the transform function
        // that has always been used for ecliptic elements. This is the check that pins the layout of
        // the matrix: a cyclic permutation of its columns would still be a proper rotation, and would
        // still put the pole somewhere sensible, but it would tilt the plane by the wrong angle.
        var pole = eclipticPole();
        var ra = Math.toDegrees(Math.atan2(pole.x, pole.z));
        var dec = Math.toDegrees(Math.asin(pole.y));
        var m = OrbitFrames.fromPole(ra, dec, new Matrix4D());
        Assert.assertEquals(0.0, maxDifference(m, Coordinates.eclToEq()), 1e-12);
    }

    @Test
    public void testEquatorialPoleIsTheIdentity() {
        // A plane that is the equator has no node on the equator, and the reference direction falls
        // back to the equinox, which is the identity frame.
        var m = OrbitFrames.fromPole(0.0, 90.0, new Matrix4D());
        Assert.assertEquals(0.0, maxDifference(m, new Matrix4D().idt()), 1e-12);
    }

    @Test
    public void testFramesAreProperRotations() {
        double[][] poles = {{268.057, 64.495}, {79.0, 88.0}, {0.0, -89.0}, {180.0, 0.0}, {40.0, 60.0}};
        for (var p : poles) {
            var m = OrbitFrames.fromPole(p[0], p[1], new Matrix4D());
            // Orthonormal columns.
            for (int c = 0; c < 3; c++) {
                var col = new Vector3D(m.val[c * 4], m.val[c * 4 + 1], m.val[c * 4 + 2]);
                Assert.assertEquals(1.0, col.len(), 1e-12);
            }
            for (int c = 0; c < 3; c++) {
                var a = new Vector3D(m.val[c * 4], m.val[c * 4 + 1], m.val[c * 4 + 2]);
                int d = (c + 1) % 3;
                var b = new Vector3D(m.val[d * 4], m.val[d * 4 + 1], m.val[d * 4 + 2]);
                Assert.assertEquals(0.0, a.dot(b), 1e-12);
            }
            // Right-handed, so that an orbit does not come out mirrored.
            var x = new Vector3D(m.val[0], m.val[1], m.val[2]);
            var y = new Vector3D(m.val[4], m.val[5], m.val[6]);
            var z = new Vector3D(m.val[8], m.val[9], m.val[10]);
            Assert.assertEquals(1.0, new Vector3D(x).crs(y).dot(z), 1e-12);
        }
    }

    @Test
    public void testExplicitPoleOverridesTheReference() {
        var oc = new OrbitComponent();
        oc.setReference("LAPLACE");
        oc.setReferencePole(new double[]{268.1, 64.5});
        var expected = OrbitFrames.fromPole(268.1, 64.5, new Matrix4D());
        var m = OrbitFrames.resolve(oc, null, EPOCH, new Matrix4D());
        Assert.assertEquals(0.0, maxDifference(m, expected), 1e-12);
    }

    @Test
    public void testNamesAreParsedLeniently() {
        Assert.assertEquals(OrbitReference.INHERITED, OrbitReference.fromString("inherit"));
        Assert.assertEquals(OrbitReference.INHERITED, OrbitReference.fromString("INHERITED"));
        Assert.assertEquals(OrbitReference.PARENT_EQUATOR, OrbitReference.fromString("parent_equator"));
        Assert.assertEquals(OrbitReference.PARENT_ORBITAL_PLANE, OrbitReference.fromString("PARENT_ORBITAL_PLANE"));
        Assert.assertEquals(OrbitReference.ECLIPTIC, OrbitReference.fromString(" Ecliptic "));
        Assert.assertEquals(OrbitReference.LAPLACE, OrbitReference.fromString("laplace"));
        Assert.assertNull(OrbitReference.fromString("nonsense"));
        // An unknown name must not silently change the reference.
        var oc = new OrbitComponent();
        oc.setReference("nonsense");
        Assert.assertEquals(OrbitReference.INHERITED, oc.reference);
    }
}
