/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gaia.time.TimeContext;

import java.time.Instant;
import java.util.Date;

public abstract class BaseAttitudeDataServer<A extends IAttitude> {

    /**
     * Some scanning laws have constants or tables for interpolation that need
     * to be computed before the first use and recomputed after changing certain
     * reference values. This flag indicates that the constants or tables
     * (whatever applicable) are up-to-date.
     */
    protected boolean initialized = false;
    /**
     * native and initially requested time context of the server - has to be set by the implementing class
     */
    protected TimeContext nativeTimeContext = null;
    protected TimeContext initialRequestedTimeContext = null;
    /**
     * switch to decide if attitude uncertainties and correlations should be calculated
     */
    protected boolean withUncertaintiesCorrelations = true;
    private long refEpoch = -1;

    /**
     * @return Returns the initialised.
     */
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public A getAttitude(Date date) {
        long tNs = (long) ((AstroUtils.getJulianDateCache(date.toInstant()) - AstroUtils.JD_J2010) * Nature.D_TO_NS);
        return getAttitudeNative(tNs);
    }

    public A getAttitude(Instant instant) {
        long tNs = (long) ((AstroUtils.getJulianDateCache(instant) - AstroUtils.JD_J2010) * Nature.D_TO_NS);
        return getAttitudeNative(tNs);
    }

    /**
     * @param time The elapsed time in nanoseconds since J2010
     */
    public synchronized A getAttitude(long time) {
        return getAttitudeNative(time);
    }

    /**
     * Evaluate the attitude in the native time system of the server
     */
    abstract protected A getAttitudeNative(long time);

    /**
     * @return The reference time in ns
     */
    public long getRefTime() {
        return refEpoch;
    }

    /**
     * @param t Reference time in nanoseconds (jd)
     */
    public void setRefTime(long t) {
        this.refEpoch = t;
    }

}
