/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop2000;

import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.coord.AbstractOrbitCoordinates;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.vsop2000.VSOP2000Reader.VSOP2000Coordinate;
import gaiasky.util.math.Vector3b;
import net.jafama.FastMath;

import java.nio.file.Path;
import java.time.Instant;

/**
 * This class provides coordinates for a single body. It is initialized with the
 * VSOP2000 data file for that body.
 */
public class VSOP2000 extends AbstractOrbitCoordinates {

    /**
     * Mean motions [][0] and mean J2000 longitudes [][1] of contributing bodies, in radians.
     * These are also frequencies and phases respectively.
     */
    private final double[][] meanMotionsLongitudes = {
            { 26.0879031405997, 4.40260863422 }, // MERCURY
            { 10.2132855473855, 3.17613445715 }, // VENUS
            { 6.2830758504457, 1.75346994632 }, // EARTH - MOON
            { 3.3406124347175, 6.20349959869 }, // MARS
            { .5296909721118, .59954667809 }, // JUPITER
            { .2132990797783, .87401678345 }, // SATURNE
            { .0747816656905, 5.48122762581 }, // URANUS
            { .0381329181312, 5.31189410499 }, // NEPTUNE
            { .0253505050000, 4.17081920000 }, // PLUTO
            { 77.7137714681205, 5.19846674103 }, // l Moon
            { 84.3346615813083, 1.62790523337 }, // D Moon
            { 83.2869142695536, 2.35555589827 }, // F Moon
            { 17.3117650220000, 4.09149775810 }, // VESTA
            { 17.0445079110000, 1.71780146320 }, // IRIS
            { 14.2890301680000, 5.59335131690 }, // BAMBERGA
            { 13.6476337490000, 2.80627678560 }, // CERES
            { 13.6194950400000, 2.03486053350 } // PALLAS
    };

    private double lastT = Double.NaN;
    /**
     * Array used to store theta(t) for each body.
     */
    private final double[] theta = new double[16];
    /**
     * Location of the data file for this VSOP2000 instance.
     */
    private String dataFile;

    /**
     * VSOP2000 data for each of the coordinates (x, y, z) for the body.
     */
    private VSOP2000Coordinate[] data;

    public VSOP2000() {

    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    /**
     * Initializes the data, if needed.
     *
     * @return True if the data is initialized and ready to use. False otherwise.
     */
    public boolean initialize() {
        if (data == null) {
            if (dataFile != null) {
                // Initialize.
                VSOP2000Reader reader = new VSOP2000Reader();
                try {
                    data = reader.read(Path.of(dataFile));
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
     * Gets the rectangular heliocentric coordinates (geocentric for the Moon) of the
     * body, in internal units (see {@link Constants#U_TO_M}).
     *
     * @param date The date as a Java {@link Instant}.
     * @param out  The vector to return the result.
     */
    public void position(Instant date,
                         Vector3b out) {
        if (!initialize()) {
            // Something is wrong.
            return;
        }

        // Maximum number of terms to use. Depends on high accuracy setting.
        final int maxTerms = Settings.settings.data.highAccuracy ? 2000 : 500;

        double t = time(AstroUtils.getJulianDateCache(date));

        VSOP2000Coordinate[] d = data;

        // Maybe we can skip this if we have done it already.
        if (lastT != t) {
            for (int j = 0; j < 16; j++) {
                theta[j] = meanMotionsLongitudes[j][0] * t + meanMotionsLongitudes[j][1];
            }
            lastT = t;
        }

        double[] coordinates = new double[3];
        // X, Y, Z
        for (int coord = 0; coord < 3; coord++) {
            // Bodies.
            double sum_k = 0;
            for (int k = 0; k < 16; k++) {
                VSOP2000Coordinate c = d[coord];
                // Number of terms.
                int n_k = FastMath.min(c.numTerms[k], maxTerms);
                if (n_k == 0) {
                    break;
                }
                double sum_i = 0;
                for (int i = 0; i < n_k; i++) {
                    double phi_ki = 0;
                    for (int j = 0; j < 16; j++) {
                        phi_ki += c.terms[k][i][j] * theta[j];
                    }

                    // s_ki = c.terms[k][i][17];
                    // c_ki = c.terms[k][i][18];

                    // Sum over i.
                    sum_i += c.terms[k][i][17] * FastMath.sin(phi_ki) + c.terms[k][i][18] * FastMath.cos(phi_ki);
                }

                // Sum over k.
                sum_k += FastMath.pow(t, k) * sum_i;
            }
            coordinates[coord] = sum_k * Constants.AU_TO_U;
        }
        out.set(coordinates[1], coordinates[2], coordinates[0]);
    }

    // Time since J2000 in Julian years.
    public static double time(double julianDate) {
        return (julianDate - 2451545.0) / 365.25;
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant instant,
                                                    Vector3b out) {
        position(instant, out);
        Coordinates.cartesianToSpherical(out, out);
        return out;
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant instant,
                                                    Vector3b out) {
        position(instant, out);
        return out;
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant instant,
                                                      Vector3b out) {
        position(instant, out);
        out.mul(Coordinates.eclToEq());
        return out;
    }
}
