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
 * An enumeration to distinguish between the different times that attitude can be expressed
 * in.
 *
 * @author Uwe Lammers
 * @version $Id: TimeContext.java 329790 2013-11-15 16:31:56Z ulammers $
 */
public enum TimeContext {
    /**
     * Number of elapsed ns in TCB since the reference epoch
     */
    TCB,

    /**
     * OnBoard-Mission Time: Strictly monotonically increasing values with a resolution of 50ns. Around the
     * times of resets of the onboard clock OBMT will have jumps. See BAS-030 for details.
     */
    OBMT;

    /**
     * @return unique index in range [0, 1]
     */
    public int getIndex() {
        return ordinal();
    }
}
