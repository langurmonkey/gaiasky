/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

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
