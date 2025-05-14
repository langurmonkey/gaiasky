/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.moon;

import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

/**
 * Computation of Moon coordinates as written in "Astronomical Algorithms 2d Edition" by Jean Meeus (1998, ISBN 9780943396354).
 */
public class MoonMeeusCoordinates {

    /**
     * Periodic terms for the longitude (Sum(l)) and distance (Sum(r)) of the
     * Moon. The unit is 0.000001 degree for Sum(l), and 0.001 km for Sum(r).
     * Multiple of D M M' F CoeffSine CoeffCosine.
     */
    private static final double[][] table47a = {{0, 0, 1, 0, 6288774.0, -20905355}, {2, 0, -1, 0, 1274027, -3699111}, {2, 0, 0, 0, 658314, -2955968}, {0, 0, 2, 0, 213618, -569925}, {0, 1, 0, 0, -185116, 48888}, {0, 0, 0, 2, -114332, -3149}, {2, 0, -2, 0, 58793, 246158}, {2, -1, -1, 0, 57066.0, -152138}, {2, 0, 1, 0, 53322, -170733}, {2, -1, 0, 0, 45758, -204586}, {0, 1, -1, 0, -40923, -129620}, {1, 0, 0, 0, -34720, 108743}, {0, 1, 1, 0, -30383, 104755},
            {2, 0, 0, -2, 15327, 10321}, {0, 0, 1, 2, -12528, 0}, {0, 0, 1, -2, 10980, 79661}, {4, 0, -1, 0, 10675, -34782}, {0, 0, 3, 0, 10034, -23210}, {4, 0, -2, 0, 8548, -21636}, {2, 1, -1, 0, -7888, 24208}, {2, 1, 0, 0, -6766, 30824}, {1, 0, -1, 0, -5163, -8379}, {1, 1, 0, 0, 4987, -16675}, {2, -1, 1, 0, 4036, -12831}, {2, 0, 2, 0, 3994, -10445}, {4, 0, 0, 0, 3861, -11650}, {2, 0, -3, 0, 3665, 14403}, {0, 1, -2, 0, -2689, -7003}, {2, 0, -1, 2, -2602, 0},
            {2, -1, -2, 0, 2390, 10056}, {1, 0, 1, 0, -2348, 6322}, {2, -2, 0, 0, 2236, -9884}, {0, 1, 2, 0, -2120, 5751}, {0, 2, 0, 0, -2069, 0}, {2, -2, -1, 0, 2048, -4950}, {2, 0, 1, -2, -1773, 4130}, {2, 0, 0, 2, -1595, 0}, {4, -1, -1, 0, 1215, -3958}, {0, 0, 2, 2, -1110, 0}, {3, 0, -1, 0, -892, 3258}, {2, 1, 1, 0, -810, 2616}, {4, -1, -2, 0, 759, -1897}, {0, 2, -1, 0, -713, -2117}, {2, 2, -1, 0, -700, 2354}, {2, 1, -2, 0, 691, 0}, {2, -1, 0, -2, 596, 0},
            {4, 0, 1, 0, 549, -1423}, {0, 0, 4, 0, 537, -1117}, {4, -1, 0, 0, 520, -1571}, {1, 0, -2, 0, -487, -1739}, {2, 1, 0, -2, -399, 0}, {0, 0, 2, -2, -381, -4421}, {1, 1, 1, 0, 351, 0}, {3, 0, -2, 0, -340, 0}, {4, 0, -3, 0, 330, 0}, {2, -1, 2, 0, 327, 0}, {0, 2, 1, 0, -323, 1165}, {1, 1, -1, 0, 299, 0}, {2, 0, 3, 0, 294, 0}, {2, 0, -1, -2, 0, 8752}};
    /**
     * Periodic terms for the latitude of the Moon (Sum(b)). The unit is
     * 0.000001 degree. Multiple of D M M' F Coefficient of the sine of the
     * argument.
     */
    private static final double[][] table47b = {{0, 0, 0, 1, 5128122}, {0, 0, 1, 1, 280602}, {0, 0, 1, -1, 277693}, {2, 0, 0, -1, 173237}, {2, 0, -1, 1, 55413}, {2, 0, -1, -1, 46271}, {2, 0, 0, 1, 32573}, {0, 0, 2, 1, 17198}, {2, 0, 1, -1, 9266}, {0, 0, 2, -1, 8822}, {2, -1, 0, -1, 8216}, {2, 0, -2, -1, 4324}, {2, 0, 1, 1, 4200}, {2, 1, 0, -1, -3359}, {2, -1, -1, 1, 2463}, {2, -1, 0, 1, 2211}, {2, -1, -1, -1, 2065}, {0, 1, -1, -1, -1870},
            {4, 0, -1, -1, 1828}, {0, 1, 0, 1, -1794}, {0, 0, 0, 3, -1749}, {0, 1, -1, 1, -1565}, {1, 0, 0, 1, -1491}, {0, 1, 1, 1, -1475}, {0, 1, 1, -1, -1410}, {0, 1, 0, -1, -1344}, {1, 0, 0, -1, -1335}, {0, 0, 3, 1, 1107}, {4, 0, 0, -1, 1021}, {4, 0, -1, 1, 833}, {0, 0, 1, -3, 777}, {4, 0, -2, 1, 671}, {2, 0, 0, -3, 607}, {2, 0, 2, -1, 596}, {2, -1, 1, -1, 491}, {2, 0, -2, 1, -451}, {0, 0, 3, -1, 439}, {2, 0, 2, 1, 422}, {2, 0, -3, -1, 421},
            {2, 1, -1, 1, -366}, {2, 1, 0, 1, -351}, {4, 0, 0, 1, 331}, {2, -1, 1, 1, 315}, {2, -2, 0, -1, 302}, {0, 0, 1, 3, -283}, {2, 1, 1, -1, -229}, {1, 1, 0, -1, 223}, {1, 1, 0, 1, 223}, {0, 1, -2, -1, -220}, {2, 1, -1, -1, -220}, {1, 0, 1, 1, -185}, {2, -1, -2, -1, 181}, {0, 1, 2, 1, -177}, {4, 0, -2, -1, 176}, {4, -1, -1, -1, 166}, {1, 0, 1, -1, -164}, {4, 0, 1, -1, 132}, {1, 0, -1, -1, -119}, {4, -1, 0, -1, 115}, {2, -2, 0, 1, 107}};

    /**
     * Algorithm in "Astronomical Algorithms" book by Jean Meeus. Returns a
     * vector with the geocentric ecliptic longitude (&lambda;) in radians, the ecliptic
     * latitude (&beta;) in radians and the distance between the centers of the
     * Earth and the Moon in kilometers.
     *
     * @param julianDate The julian days since J2000.
     * @param out        The output vector with geocentric longitude (lambda) [rad],
     *                   geocentric latitude (beta) [rad], distance between the centers
     *                   of the Earth and the Moon [km].
     * @return The out vector with geocentric [lambda, beta, r] in radians and kilometres.
     */
    public static Vector3D moonEclipticCoordinates(double julianDate, Vector3D out) {

        // Time T measured in Julian centuries from the Epoch J2000.0.
        double T = AstroUtils.T(julianDate);
        double T2 = T * T;
        double T3 = T2 * T;
        double T4 = T3 * T;
        // Moon's mean longitude, referred to the mean equinox of the date.
        double Lp = 218.3164477 + 481267.88123421 * T - 0.0015786 * T2 + T3 / 538841.0 - T4 / 65194000.0;
        Lp = prettyAngle(Lp);
        // Mean elongation of the Moon
        double D = 297.8501921 + 445267.1114034 * T - 0.0018819 * T2 + T3 / 545868.0 - T4 / 113065000.0;
        D = prettyAngle(D);
        // Sun's mean anomaly
        double M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T2 + T3 / 24490000.0;
        M = prettyAngle(M);
        // Moon's mean anomaly
        double Mp = 134.9633964 + 477198.8675055 * T + 0.0087414 * T2 + T3 / 69699.0 - T4 / 14712000.0;
        Mp = prettyAngle(Mp);
        // Moon's argument of latitude (mean distance of the Moon from its
        // ascending node).
        double F = 93.272095 + 483202.0175233 * T - 0.0036539 * T2 - T3 / 3526000.0 + T4 / 863310000.0;
        F = prettyAngle(F);
        // Three further arguments (again, in degrees) are needed.
        double A1 = 119.75 + 131.849 * T;
        A1 = prettyAngle(A1);
        double A2 = 53.09 + 479264.290 * T;
        A2 = prettyAngle(A2);
        double A3 = 313.45 + 481266.484 * T;
        A3 = prettyAngle(A3);

        // Multiply by E the arguments that contain M or -M, multiply by E2 the
        // arguments that contain 2M or -2M.
        double E = 1.0 - 0.002516 * T - 0.0000074 * T2;

        double[] aux = calculateSumlSumr(D, M, Mp, F, E, A1, A2, Lp);
        double sumL = aux[0];
        double sumR = aux[1];
        double sumB = calculateSumb(D, M, Mp, F, E, A1, A3, Lp);

        double lambda = prettyAngle(Lp + sumL * 0.000001);
        double beta = declination(prettyAngle((sumB * 0.000001)));
        double dist = 385000.56 + sumR * 0.001;

        return out.set(Math.toRadians(lambda), FastMath.toRadians(beta), dist);
    }

    /**
     * Calculates the longitude Sum(l) and distance Sum(r) of the Moon using the
     * table.
     *
     * @param D  Mean elongation of the Moon
     * @param M  Sun's mean anomaly
     * @param Mp Moon's mean anomaly
     * @param F  Moon's argument of latitude (mean distance of the Moon from
     *           its ascending node)
     * @param E  Factor for eccentricity of Earth's orbit around the Sun
     * @param A1 Term due to action of Venus
     * @param A2 Term due to Jupiter
     * @param Lp Moon's mean longitude, referring to the equinox of the date
     * @return Suml and Sumr
     */
    private static double[] calculateSumlSumr(double D, double M, double Mp, double F, double E, double A1, double A2, double Lp) {
        double suml = 0.0, sumr = 0.0;
        for (double[] curr : table47a) {
            // Take into effect terms that contain M and thus depend on the
            // eccentricity of the Earth's orbit around the
            // Sun, which presently is decreasing with time.
            double mul = 1.0;
            if (curr[1] == 1.0 || curr[1] == -1.0) {
                mul = E;
            } else if (curr[1] == 2.0 || curr[1] == -2.0) {
                mul = E * E;
            }
            double argument = FastMath.toRadians(curr[0] * D + curr[1] * M + curr[2] * Mp + curr[3] * F);
            suml += curr[4] * mul * FastMath.sin(argument);
            sumr += curr[5] * mul * FastMath.cos(argument);
        }
        // Addition to Suml. The terms involving A1 are due to the action of
        // Venus. The term involving A2 is due to Jupiter
        // while those involving L' are due to the flattening of the Earth.
        double sumladd = 3958.0 * FastMath.sin(Math.toRadians(A1)) + 1962.0 * FastMath.sin(Math.toRadians(Lp - F)) + 318.0 * FastMath.sin(Math.toRadians(A2));
        suml += sumladd;

        return new double[]{suml, sumr};

    }

    private static double calculateSumb(double D, double M, double Mp, double F, double E, double A1, double A3, double Lp) {
        double sumB = 0.0;
        for (double[] curr : table47b) {
            // Take into effect terms that contain M and thus depend on the
            // eccentricity of the Earth's orbit around the
            // Sun, which presently is decreasing with time.
            double mul = 1.0;
            if (curr[1] == 1.0 || curr[1] == -1.0) {
                mul = E;
            } else if (curr[1] == 2.0 || curr[1] == -2.0) {
                mul = E * E;
            }
            sumB += curr[4] * mul * FastMath.sin(Math.toRadians(curr[0] * D + curr[1] * M + curr[2] * Mp + curr[3] * F));
        }
        // Addition to SumB. The terms involving A1 are due to the action of
        // Venus. The term involving A2 is due to Jupiter
        // while those involving L are due to the flattening of the Earth.
        double sumBAdd = -2235.0 * FastMath.sin(Math.toRadians(Lp)) + 382.0 * FastMath.sin(Math.toRadians(A3)) + 175.0 * FastMath.sin(Math.toRadians(A1 - F)) + 175.0 * FastMath.sin(Math.toRadians(A1 + F)) + 127.0 * FastMath.sin(Math.toRadians(Lp - Mp)) - 115.0 * FastMath.sin(Math.toRadians(Lp + Mp));
        sumB += sumBAdd;

        return sumB;
    }

    private static double prettyAngle(double angle) {
        return angle - 360d * (int) (angle / 360d);
    }

    private static double declination(double angle) {
        return FastMath.abs(angle) <= 90 ? angle : angle - 360d;
    }
}
