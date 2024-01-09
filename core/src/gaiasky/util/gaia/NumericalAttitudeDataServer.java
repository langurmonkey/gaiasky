/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.Logger;
import gaiasky.util.gaia.time.Gti;
import gaiasky.util.gaia.time.GtiList;

public abstract class NumericalAttitudeDataServer<A extends IAttitude> extends BaseAttitudeDataServer<A> {
    //	protected boolean initialized = false;

    /**
     * List of good time intervals
     */
    protected GtiList gtis;

    /**
     * Any numerical attitude needs to be initialized
     *
     * @throws RuntimeException initialization fails
     */
    public abstract void initialize();

    /**
     * Check if the there is a valid attitude at a given time
     *
     * @param t time
     *
     * @return true if the attitude is valid for time t
     */
    public boolean isValid(long t) {
        return (gtis.inside(t) != null);
    }

    /**
     * Get the set of good time intervals for the spacecraft attitude. Note that
     * this may not be the same as the times for which an attitude is defined in
     * the input data; an implementation may censor time periods, for example
     * when the attitude uncertainty is higher than a defined threshold.
     *
     * @return the list of attitude Good Time Intervals.
     */
    public GtiList getGtis() {
        // return a deep copy of gtis
        GtiList gtisCopy = new GtiList();
        int nGtis = gtis.size();
        for (Gti gti : gtis) {
            long tStart = gti.getStart();
            long tEnd = gti.getEnd();
            try {
                gtisCopy.add(tStart, tEnd);
            } catch (RuntimeException e) {
                Logger.getLogger(this.getClass()).error(e);
            }
        }
        return gtisCopy;
    }

    /**
     * @return start time of this attitude segment - this is the earliest time
     * at which attitude can be requested
     */
    public long getStartTime() {
        Gti gti = gtis.get(0);
        return gti.getStart();
    }

    /**
     * @return end time of this attitude segment - this is the latest time
     * at which attitude can be requested
     */
    public long getStopTime() {
        Gti gti = gtis.get(gtis.size() - 1);
        return gti.getEnd();
    }

}
