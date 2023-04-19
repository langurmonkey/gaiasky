/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.time;

import java.time.Instant;

public interface ITimeFrameProvider {

    /**
     * The simulation time difference in hours.
     *
     * @return The simulation time difference in hours.
     */
    double getHdiff();

    /**
     * The frame time difference in seconds.
     *
     * @return The frame time difference in seconds.
     */
    double getDt();

    /**
     * Gets the current time in UTC.
     *
     * @return The time as an instant, in UTC.
     */
    Instant getTime();

    /**
     * Updates this time frame with the system time difference
     *
     * @param dt System time difference in seconds
     */
    void update(double dt);

    /**
     * Gets the current warp factor
     *
     * @return The warp factor
     */
    double getWarpFactor();

    /**
     * Is the time on?
     *
     * @return True if time is on
     */
    boolean isTimeOn();

    /**
     * Returns whether the frame rate is set to fixed or not
     *
     * @return Whether fix rate mode is on
     */
    boolean isFixedRateMode();

    /**
     * Returns the fixed frame rate if the mode is fixed frame rate. Returns -1
     * otherwise
     *
     * @return The fixed rate
     */
    float getFixedRate();

}
