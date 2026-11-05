/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.camera;

import org.junit.Assert;
import org.junit.Test;

/**
 * The closest body is what the camera measures its speed against, so the two functions that pick and
 * measure it are pinned here. The bug this covers: a body could hold the closest slot forever, either
 * because it stopped being rendered (nothing ever reset the slot) or because its distance was NaN
 * (a NaN never compares as larger, so it could not be displaced). Either way the camera speed became
 * a constant that had nothing to do with where the camera was.
 */
public class ClosestBodyTest {

    @Test
    public void closerCandidateWins() {
        Assert.assertTrue(AbstractCamera.isCloser(5, 1, 10, 1));
        Assert.assertFalse(AbstractCamera.isCloser(15, 1, 10, 1));
    }

    @Test
    public void radiusCounts() {
        // A big body whose surface is nearer wins over a small one that is farther away.
        Assert.assertTrue(AbstractCamera.isCloser(30, 25, 10, 1));
        Assert.assertFalse(AbstractCamera.isCloser(10, 1, 30, 25));
    }

    @Test
    public void equalDistancesKeepTheIncumbent() {
        Assert.assertFalse(AbstractCamera.isCloser(10, 1, 10, 1));
    }

    @Test
    public void nonFiniteCandidateNeverWins() {
        Assert.assertFalse(AbstractCamera.isCloser(Double.NaN, 1, 10, 1));
        Assert.assertFalse(AbstractCamera.isCloser(Double.POSITIVE_INFINITY, 1, 10, 1));
        Assert.assertFalse(AbstractCamera.isCloser(10, Double.NaN, 10, 1));
    }

    @Test
    public void nonFiniteIncumbentIsAlwaysReplaced() {
        // An empty slot is passed as an infinite distance.
        Assert.assertTrue(AbstractCamera.isCloser(10, 1, Double.POSITIVE_INFINITY, 0));
        // A poisoned incumbent must not be able to keep the slot.
        Assert.assertTrue(AbstractCamera.isCloser(10, 1, Double.NaN, 1));
    }

    @Test
    public void closestBodyDistanceSubtractsTheElevation() {
        double surface = NaturalCamera.closestBodyDistance(1000, 100, false);
        double centre = NaturalCamera.closestBodyDistance(1000, 0, false);
        Assert.assertEquals(-100, surface - centre, 1e-9);
    }

    @Test
    public void billboardsNeedExtraClearance() {
        Assert.assertEquals(NaturalCamera.closestBodyDistance(1000, 0, true),
                            15 * NaturalCamera.closestBodyDistance(1000, 0, false), 1e-9);
    }

    @Test
    public void nonFiniteDistanceNeverReachesTheSpeedScaling() {
        Assert.assertEquals(NaturalCamera.NO_CLOSEST_BODY_DISTANCE,
                            NaturalCamera.closestBodyDistance(Double.NaN, 0, false), 0);
        Assert.assertEquals(NaturalCamera.NO_CLOSEST_BODY_DISTANCE,
                            NaturalCamera.closestBodyDistance(Double.POSITIVE_INFINITY, 0, false), 0);
        Assert.assertEquals(NaturalCamera.NO_CLOSEST_BODY_DISTANCE,
                            NaturalCamera.closestBodyDistance(1000, Double.NaN, false), 0);
    }
}
