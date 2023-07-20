/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

public class TimeMediator {
    static TimeMediator[] mediators = new TimeMediator[] {
            new TimeMediator(TimeContext.TCB, TimeContext.TCB),
            new TimeMediator(TimeContext.TCB, TimeContext.OBMT),
            new TimeMediator(TimeContext.OBMT, TimeContext.TCB),
            new TimeMediator(TimeContext.OBMT, TimeContext.OBMT)
    };
    protected TimeConverter converter;

    /**
     * Construct a new TimeMediator with a given native and requested time context
     *
     * @param nat native time context
     * @param req requested time context
     */
    public TimeMediator(TimeContext nat, TimeContext req) {
        setTimeContext(nat, req);
    }

    /**
     * Get an applicable {@link TimeMediator} for a given combination of time contexts.
     *
     * @param nat native time context
     * @param req requested time context
     *
     * @return
     */
    static public TimeMediator getTimeMediator(TimeContext nat, TimeContext req) {
        return mediators[nat.getIndex() * TimeContext.values().length + req.getIndex()];
    }

    /**
     * Convert a given time.
     *
     * @param t time [ns] to convert
     *
     * @return converted time
     *
     * @ data needed in conversion not available
     */
    public long convert(long t) {
        return converter.convert(t);
    }

    /**
     * Setup a converter according to a requested and native time context.
     *
     * @param nat native time context
     * @param req requested time context
     */
    public void setTimeContext(TimeContext nat, TimeContext req) {
        if (req == TimeContext.OBMT && nat == TimeContext.OBMT) {
            // OBMT->OBMT
            converter = new ObmtToObmt();
        } else if (req == TimeContext.OBMT && nat == TimeContext.TCB) {
            // OBMT->TCB
            //			converter = new ObmtToTcb();
        } else if (req == TimeContext.TCB && nat == TimeContext.OBMT) {
            // TCB->OBMT
            //			converter = new TcbToObmt();
        } else {
            // TCB->TCB
            converter = new TcbToTcb();
        }
    }

    private interface TimeConverter {
        long convert(long t);
    }

    // native OBMT - requested OBMT
    private class ObmtToObmt implements TimeConverter {
        public long convert(long t) {
            return t;
        }
    }

    // native: TCB - requested: TCB
    private class TcbToTcb implements TimeConverter {
        public long convert(long t) {
            return t;
        }
    }
}