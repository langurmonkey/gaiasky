/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import net.jafama.FastMath;

/**
 * Constants in nature, mostly to convert units.
 */
public class Nature {

    /*
     * ======= ANGLE UNITS =======
     */
    /** Degrees to radians **/
    public static final double TO_RAD = FastMath.PI / 180;
    /** Radians to degrees **/
    public static final double TO_DEG = 180 / FastMath.PI;
    /** Degrees to arc-seconds **/
    public static final double DEG_TO_ARCSEC = 3600;
    /** Arc-seconds to degrees **/
    public static final double ARCSEC_TO_DEG = 1 / DEG_TO_ARCSEC;
    public static final double ARCSEC_TO_RAD = ARCSEC_TO_DEG * TO_RAD;
    public static final double DEG_TO_MILLARCSEC = DEG_TO_ARCSEC * 1000;
    public static final double MILLARCSEC_TO_DEG = 1 / DEG_TO_MILLARCSEC;
    public static final double MILLARCSEC_TO_RAD = MILLARCSEC_TO_DEG * TO_RAD;
    public static final double RAD_TO_MILLARCSEC = TO_DEG * DEG_TO_MILLARCSEC;
    public static final double MILLIARCSEC_TO_ARCSEC = 1d / 1000d;
    public static final double ARCSEC_TO_MILLIARCSEC = 1000d;
    public static final double ARCSEC_PER_YEAR_TO_KMS = 4.74047;

    /*
     * ======= DISTANCE UNITS =======
     */
    /** Parsecs to kilometres **/
    public static final double PC_TO_KM = 3.08567758149137e13;
    /** Kilometres to parsecs **/
    public static final double KM_TO_PC = 1.0 / PC_TO_KM;
    /** Parsecs to metres **/
    public static final double PC_TO_M = PC_TO_KM * 1000.0;
    /** Metres to parsecs **/
    public static final double M_TO_PC = 1.0 / PC_TO_M;
    /** Astronomical units to kilometres **/
    public static final double AU_TO_KM = 149597871.0;
    /** Kilometres to astronomical units **/
    public static final double KM_TO_AU = 1.0 / AU_TO_KM;
    /** Light years to kilometers **/
    public static final double LY_TO_KM = 9.46073e12;
    /** Kilometers to light years **/
    public static final double KM_TO_LY = 1.0 / LY_TO_KM;
    /** Kilometers to metres **/
    public static final double KM_TO_M = 1000.0;

    /*
     * ======= TIME UNITS =======
     */
    /** Seconds to milliseconds **/
    public static final double S_TO_MS = 1000.0;
    /** Milliseconds to seconds **/
    public static final double MS_TO_S = 1.0 / S_TO_MS;
    /** Milliseconds to nanoseconds **/
    public static final double MS_TO_NS = 1.0e6;
    /** Seconds to nanoseconds **/
    public static final double S_TO_NS = S_TO_MS * MS_TO_NS;
    /** Hours to seconds **/
    public static final double H_TO_S = 3600.0;
    /** Seconds to hours **/
    public static final double S_TO_H = 1.0 / H_TO_S;
    /** Hours to milliseconds **/
    public static final double H_TO_MS = H_TO_S * 1000.0;
    /** Milliseconds to hours **/
    public static final double MS_TO_H = 1.0 / H_TO_MS;
    /** Days to seconds **/
    public static final double D_TO_S = 86400.0;
    /** Seconds to days **/
    public static final double S_TO_D = 1.0 / D_TO_S;
    /** Days to milliseconds **/
    public static final double D_TO_MS = D_TO_S * 1000.0;
    /** Milliseconds to days **/
    public static final double MS_TO_D = 1.0 / D_TO_MS;
    /** Days to nanoseconds **/
    public static final double D_TO_NS = D_TO_S * 1.0e9;
    /** Nanoseconds to days **/
    public static final double NS_TO_D = 1.0 / D_TO_NS;
    /** Years to seconds **/
    public static final double Y_TO_S = 31557600.0;
    /** Seconds to years **/
    public static final double S_TO_Y = 1.0 / Y_TO_S;
    /** Years to milliseconds **/
    public static final double Y_TO_MS = Y_TO_S * 1000.0;
    /** Milliseconds to year **/
    public static final double MS_TO_Y = 1.0 / Y_TO_MS;
    /** Minutes to seconds **/
    public static final double MIN_TO_S = 60.0;

    /*
     * ======= SPEEDS =======
     */
    /**
     * Speed of light in m/s
     */
    public static final double C = 299792458;

}
