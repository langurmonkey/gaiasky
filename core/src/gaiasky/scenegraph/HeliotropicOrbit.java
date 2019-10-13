/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;

import java.time.Instant;

/**
 * Heliotropic orbits must be corrected using the Sun longitude. They are by
 * default in equatorial coordinates.
 * 
 * @author Toni Sagrista
 *
 */
public class HeliotropicOrbit extends Orbit {
    double angle;

    public HeliotropicOrbit() {
        super();
    }

    /**
     * Update the local transform with the transform and the rotations/scales
     * necessary. Override if your model contains more than just the position
     * and size.
     */
    protected void updateLocalTransform(Instant date) {
        angle = AstroUtils.getSunLongitude(date);
        translation.getMatrix(localTransformD).mul(Coordinates.eclToEq()).rotate(0, 1, 0, angle + 180);

        localTransformD.putIn(localTransform);
    }
}
