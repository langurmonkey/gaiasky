/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

public sealed interface IHeightData permits HeightDataPixmap, HeightDataSVT {

    /**
     * Gets the height normalized in [0,1] for the given UV coordinates.
     * @param u The U coordinate.
     * @param v The V coordinate.
     * @return The height value in [0,1].
     */
    double getNormalizedHeight(double u, double v);

}
