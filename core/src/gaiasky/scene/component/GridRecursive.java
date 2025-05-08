/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.Pair;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.Vector3D;

import java.util.List;

public class GridRecursive implements Component {
    public final float[] ccEq = ColorUtils.gRed;
    public final float[] ccEcl = ColorUtils.gGreen;
    public final float[] ccGal = ColorUtils.gBlue;
    public final float[] ccL = ColorUtils.gYellow;

    public Pair<Double, Double> scalingFading;
    public float fovFactor;
    public List<Pair<Double, String>> annotations;

    // Mid-points of lines in refys mode
    public Vector3D p01, p02, a, b, c, d;
    public double d01, d02;

    // Regime: 1 - normal with depth buffer, 2 - rescaling quad
    public byte regime = 1;

}
