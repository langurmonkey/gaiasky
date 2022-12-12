/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * GaiaTools
 * Copyright (C) 2006 Gaia Data Processing and Analysis Consortium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package gaiasky.util.gaia.time;

/**
 * A small class to convert between GaiaTime/TCB and OBMT transparently. Needed for the
 * attitude providers which support the retrieval of attitude in OBMT and GaiaTime/TCB.
 *
 * @author Uwe Lammers
 * @version $Id$
 */
public class TimeMediator {
    static TimeMediator[] mediators = new TimeMediator[] {
            new TimeMediator(TimeContext.TCB, TimeContext.TCB),
            new TimeMediator(TimeContext.TCB, TimeContext.OBMT),
            new TimeMediator(TimeContext.OBMT, TimeContext.TCB),
            new TimeMediator(TimeContext.OBMT, TimeContext.OBMT)
    };
    protected TimeConverter converter;

    /**
     * Construct a new TimeMediator with a given native and requestd time context
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