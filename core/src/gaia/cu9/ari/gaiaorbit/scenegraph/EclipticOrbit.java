package gaia.cu9.ari.gaiaorbit.scenegraph;

import java.time.Instant;

import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;

/**
 * Heliotropic orbits must be corrected using the Sun longitude. They are by default
 * in equatorial coordinates.
 * @author Toni Sagrista
 *
 */
public class EclipticOrbit extends Orbit {
    double angle;

    public EclipticOrbit() {
        super();
    }

    /**
     * Update the local transform with the transform and the rotations/scales necessary.
     * Override if your model contains more than just the position and size.
     */
    @Override
    protected void updateLocalTransform(Instant date) {
        translation.getMatrix(localTransformD).rotate(0, 0, 1, AstroUtils.obliquity(AstroUtils.getJulianDate(date)));
    }
}