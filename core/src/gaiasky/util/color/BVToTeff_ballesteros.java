/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.color;

/**
 * Black-body based conversion between effective temperature and B-V color.
 * <p>
 * Ballesteros 2012 (EPL 97, 34008) present a conversion between
 * effective temperature and B-V color index based on a black body
 * spectrum and the filter functions.
 * </p>
 */
public class BVToTeff_ballesteros {
    private final double a;
    private final double b;
    private final double c;
    private final double T0;

    public BVToTeff_ballesteros() {
        a = 0.92;
        b = 1.7;
        c = 0.62;
        T0 = 4600.0;
    }

    /**
     * Convert the incoming B-V color index [mag] into an effective temperature [K]
     *
     * @param bv The B-V color index [mag]
     *
     * @return Effective temperature [K]
     */
    public double bvToTeff(double bv) {
        return T0 * (1.0 / (a * bv + b) + 1.0 / (a * bv + c));
    }

    /**
     * Convert the incoming effective temperature [K] into a B-V color index [mag]
     *
     * @param teff Effective temperature [K]
     *
     * @return The B-V color index [mag]
     */
    public double teffToBv(double teff) {
        double z = teff / T0;
        double ap = z * (a * a);
        double bp = a * c * z + b * a * z - 2.0 * a;
        double cp = b * c * z - c - b;

        double sqrtarg = (bp * bp) - 4.0 * ap * cp;
        //By comparison with a BB it can be verified that
        // the physical solution is this one
        return (-bp + Math.sqrt(sqrtarg)) / (2.0 * ap);
    }

}
