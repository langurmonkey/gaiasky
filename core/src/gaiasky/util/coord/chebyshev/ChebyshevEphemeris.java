package gaiasky.util.coord.chebyshev;

import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.coord.AbstractOrbitCoordinates;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3Q;

import java.nio.file.Path;
import java.time.Instant;

/**
 * This class provides coordinates for a single body. It is initialized with the
 * Chebyshev data file for that body.
 */
public class ChebyshevEphemeris extends AbstractOrbitCoordinates {

    public String dataFile;
    public ChebyshevCoefficients data;

    public ChebyshevEphemeris() {
    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    public boolean initialize() {
        if (data == null) {
            if (dataFile != null) {
                // Initialize.
                ChebyshevLoader reader = new ChebyshevLoader();
                try {
                    data = reader.loadData(Path.of(dataFile));
                    return data != null;
                } catch (Exception e) {
                    logger.error("Error initializing coordinates: " + dataFile, e);
                    return false;
                }

            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Returns a vector with the ephemeris
     * at the given date. The position is given in internal units.
     *
     * @param date The date as a Java {@link Instant}.
     * @param out  The vector to return the result.
     * @return The out vector if we could retrieve the position, or null if the time is out of range for the current data file.
     */
    public Vector3Q position(Instant date, final Vector3Q out) {
        if (!initialize()) {
            // Something is wrong.
            return null;
        }

        double jd = AstroUtils.getJulianDateCache(date) - AstroUtils.JD_J2010;
        long nanosecondsTcb = (long) (jd * Nature.D_TO_NS);

        final int positionTypeIndex = 0;

        final ChebyshevCoefficients.Header positionHeader = this.data.header[positionTypeIndex];
        final ChebyshevCoefficients.Coefficients positionCoefficients = this.data.coefficients[positionTypeIndex];

        // Evaluate the Chebyshev polynomials to compute the ephemeris
        return evaluateChebyshev(nanosecondsTcb, positionHeader, positionCoefficients, out);
    }

    /**
     * Evaluates the Chebyshev polynomial for the given time in nanoseconds TCB
     * using the information in the given
     * <code>{@link ChebyshevCoefficients.Header}</code> and the data in
     * <code>{@link ChebyshevCoefficients.Coefficients}</code>.
     *
     * @param nanosecondsTcb the time in nanoseconds TCB
     * @param header         the
     *                       <code>{@link ChebyshevCoefficients.Header}</code>
     * @param coefficients   the
     *                       <code>{@link ChebyshevCoefficients.Coefficients}</code>
     * @param out            The vector to put the result.
     * @return The out vector if we could retrieve the position, or null if the time is out of range for the current data file.
     */
    private Vector3Q evaluateChebyshev(final long nanosecondsTcb, final ChebyshevCoefficients.Header header,
                                       final ChebyshevCoefficients.Coefficients coefficients,
                                       final Vector3Q out) {

        // The index of the granule
        int iGranule;
        // The argument [0,+1] of the scaled Chebyshev polynomials
        double t;

        // Distinguish between equisized and non-equisized granules

        if (header.isEquidistant) {

            final long nanosecondsOffset = nanosecondsTcb - header.nanosecondsTcbBegin;

            // Compute granule index directly

            final double granuleLength = coefficients.nanoSecondsTcb[1] - coefficients.nanoSecondsTcb[0];
            iGranule = (int) (nanosecondsOffset / granuleLength);

            // Compute argument [0,+1] for scaled Chebyshev polynomials within granule
            // in such a way as to reduce round-off errors

            t = (nanosecondsOffset - iGranule * granuleLength) / granuleLength;

            // Correct values for the end of the last granule
            // which we want to include

            if (iGranule == header.nGranules) {
                --iGranule;
                t = 1.0;
            }

        } else {

            // Find granule index

            iGranule = this.binarySearch(nanosecondsTcb, coefficients.nanoSecondsTcb);

            // compute argument [0,+1] for scaled Chebyshev polynomials within granule

            final long nanosecondsTcbLow = coefficients.nanoSecondsTcb[iGranule];
            final long nanosecondsTcbHigh = coefficients.nanoSecondsTcb[iGranule + 1];
            final long granuleLength = nanosecondsTcbHigh - nanosecondsTcbLow;
            t = (nanosecondsTcb - nanosecondsTcbLow) / (double) granuleLength;
        }

        // Out of range.
        if (iGranule > coefficients.data.length || iGranule < 0) {
            return null;
        }

        // Compute ephemeris
        final double[] coefficientsX = coefficients.data[iGranule][0];
        final double[] coefficientsY = coefficients.data[iGranule][1];
        final double[] coefficientsZ = coefficients.data[iGranule][2];

        // Here we assume that the order of the Chebyshev polynomials is
        // the same for all three vector components -- this is OK, since we
        // explicitly tested for it while loading the data

        final int nCoefficients = coefficientsX.length;

        // 0th order Chebyshev
        double t0 = 1.0;
        double x = coefficientsX[0] * t0;
        double y = coefficientsY[0] * t0;
        double z = coefficientsZ[0] * t0;

        // 1st order Chebyshev
        double t1 = 2.0 * t - 1.0;
        x += coefficientsX[1] * t1;
        y += coefficientsY[1] * t1;
        z += coefficientsZ[1] * t1;

        // Higher orders Chebyshev recursively
        double tn;
        final double tau = 2.0 * t1;
        for (int iCoefficient = 2; iCoefficient < nCoefficients; iCoefficient++) {

            tn = tau * t1 - t0;
            t0 = t1;
            t1 = tn;

            x += coefficientsX[iCoefficient] * tn;
            y += coefficientsY[iCoefficient] * tn;
            z += coefficientsZ[iCoefficient] * tn;
        }

        out.set(y, z, x).scl(Constants.M_TO_U);
        return out;
    }

    /**
     * Finds the granule index for the given time in nanoseconds TCB in the given
     * array of granule begin times in nanoseconds TCB.
     *
     * @param nanosecondsTcb      the time in nanoseconds TCB
     * @param nanosecondsTcbArray the array of granule begin times in nanoseconds
     *                            TCB
     * @return the granule index
     */
    private int binarySearch(final long nanosecondsTcb, final long[] nanosecondsTcbArray) {

        final int nGranules = nanosecondsTcbArray.length;

        int low = 0;
        int high = nGranules;
        int mid;
        int lowPlus1 = low + 1;

        while (high > lowPlus1) {

            mid = low + high >>> 1;

            if (nanosecondsTcbArray[mid] <= nanosecondsTcb) {
                low = mid;
                lowPlus1 = low + 1;
            } else {
                high = mid;
            }

        }

        return low;
    }


    @Override
    public Vector3Q getEclipticSphericalCoordinates(Instant instant,
                                                    Vector3Q out) {
        position(instant, out);
        Coordinates.cartesianToSpherical(out, out);
        return out;
    }

    @Override
    public Vector3Q getEclipticCartesianCoordinates(Instant instant,
                                                    Vector3Q out) {
        position(instant, out);
        out.mul(Coordinates.eqToEcl());
        return out;
    }

    @Override
    public Vector3Q getEquatorialCartesianCoordinates(Instant instant,
                                                      Vector3Q out) {
        position(instant, out);
        return out;
    }
}
