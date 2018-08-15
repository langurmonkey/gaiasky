package gaia.cu9.ari.gaiaorbit.scenegraph;

import java.time.Instant;

import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;

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
