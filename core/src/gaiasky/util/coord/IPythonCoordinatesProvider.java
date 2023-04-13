package gaiasky.util.coord;

import java.util.List;

/**
 * A coordinates provider intended to be implemented in a Python script.
 */
public interface IPythonCoordinatesProvider {

    /**
     * This method takes in a julian date and outputs the coordinates
     * in the internal cartesian system.
     *
     * @param julianDate The julian date to get the coordinates for, as a 64-bit floating point number.
     *
     * @return A 3-vector containing the XYZ values in internal cartesian coordinates, and internal units.
     */
    Object getEquatorialCartesianCoordinates(Object julianDate, Object out);
}
