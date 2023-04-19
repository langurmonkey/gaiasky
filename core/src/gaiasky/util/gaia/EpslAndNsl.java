/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

public class EpslAndNsl extends AnalyticalAttitudeDataServer {

    private final Epsl epsl = new Epsl();
    private final Nsl37 nsl = new Nsl37();

    /**
     * Default constructor:
     */
    public EpslAndNsl() {
        setDefault();
        copyRefValues();
    }

    /**
     * Constructor for arbitrary reference time (= switch from EPSL to NSL)
     * and Epsl mode (PRECEDING or FOLLOWING):
     *
     * @param tRef time of the switch
     * @param mode which mode to switch to
     */
    public EpslAndNsl(long tRef, Epsl.Mode mode) {
        setDefault();
        if (mode == Epsl.Mode.FOLLOWING) {
            this.setNuRef(Math.PI);
        }
        copyRefValues();
        setRefTime(tRef);
    }

    /**
     * Copy reference values from this to Epsl and Nsl37:
     */
    private void copyRefValues() {
        // set reference parameters at tRef for EPSL:
        epsl.setXiRef(getXiRef());
        epsl.setNuRef(getNuRef());
        epsl.setOmegaRef(getOmegaRef());
        epsl.setTargetScanPeriod(getTargetScanPeriod());
        epsl.setInitialized(true); // no need to recompute anything

        // set NSL parameters = new Nsl37();
        nsl.setXiRef(getXiRef());
        nsl.setNuRef(getNuRef());
        nsl.setOmegaRef(getOmegaRef());
        nsl.setTargetPrecessionRate(getTargetPrecessionRate());
        nsl.setTargetScanPeriod(getTargetScanPeriod());
        nsl.setInitialized(false);
    }

    /**
     * @param tNow - the time elapsed since the epoch of J2010 in ns (TCB)
     *
     * @return attitude for the given time
     *
     * @see gaiasky.util.gaia.AnalyticalAttitudeDataServer#getAttitude(long)
     */
    @Override
    public IAttitude getAttitudeNative(long tNow) {
        if (!initialized) {
            copyRefValues();
            super.setInitialized(true);
        }

        // The reference time is the time to switch.
        if (tNow < getRefTime()) {
            return epsl.getAttitude(tNow);
        } else {
            return nsl.getAttitude(tNow);
        }
    }

    @Override
    public void setRefTime(long t) {
        super.setRefTime(t);

        nsl.setRefTime(t);
        epsl.setRefTime(t);

        setInitialized(false);
    }
}
