/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;


import gaiasky.scene.record.OrbitComponent;
import gaiasky.util.Constants;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3d;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

/**
 * Tests the keplerian orbital elements transformation to state vectors. See <code>gaiasky/core/scripts/other/keplerian-to-cartesian.py</code>
 * for an alternative implementation.
 */
public class OrbitComponentTest {
    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testKeplerianElements() {
        OrbitComponent oc = new OrbitComponent();
        oc.period = 365.25;
        oc.epoch = 2451545.0;
        oc.semiMajorAxis = 149597870.7;  // 1 AU
        oc.e = 0.0167;
        oc.i = 0.00005;
        oc.ascendingNode = -11.26064;
        oc.argOfPericenter = 114.20783;
        oc.meanAnomaly = 358.617;  // at epoch
        oc.mu =1.32712440018e11;

        Vector3d out = new Vector3d();
        Instant i = AstroUtils.julianDateToInstant(oc.epoch + 100.0);
        oc.loadDataPoint(out, i);
        out.scl(Constants.U_TO_KM);
        var xp = out.z;
        var yp = out.x;
        var zp = out.y;

        var xe = -1.39012365e+8;
        var ye = -5.62216786e+7;
        var ze = -7.18068969e+1;

        Assert.assertEquals(xe, xp, 0.5);
        Assert.assertEquals(ye, yp, 0.5);
        Assert.assertEquals(ze, zp, 0.5);


    }
}
