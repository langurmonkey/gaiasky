package gaiasky.util.coord;

import gaiasky.util.math.Vector3b;

import java.time.Instant;

/**
 * This class manages body coordinates that are implemented in a Python script.
 * So far only equatorial cartesian coordinates are supported.
 */
public class PythonBodyCoordinates implements IBodyCoordinates {

    private final IPythonCoordinatesProvider provider;

    public PythonBodyCoordinates(IPythonCoordinatesProvider provider) {
        this.provider = provider;
    }

    @Override
    public void doneLoading(Object... params) {
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant instant, Vector3b out) {
        return null;
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant instant, Vector3b out) {
        return null;
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant instant, Vector3b out) {
        provider.getEquatorialCartesianCoordinates(AstroUtils.getJulianDate(instant), out);
        return out;
    }
}
