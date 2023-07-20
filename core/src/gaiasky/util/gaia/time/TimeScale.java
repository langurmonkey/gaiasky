/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

public enum TimeScale {

    /**
     * Gaia's proper Time "Temp de Gaia"
     */
    TG,

    /**
     * Barycentric Coordinate Time
     */
    TCB,

    /**
     * Terrestrial Time
     */
    TT,

    /**
     * Barycentric Dynamical Time
     */
    TDB,

    /**
     * Geocentric Coordinate Time
     */
    TCG,

    /**
     * Coordinated Universal Time
     */
    UTC,

    /**
     * International Atomic Time
     */
    TAI,

    /**
     * GPS time
     */
    GPS,

    /**
     * Embrace the unknown
     */
    UNKNOWN

}
