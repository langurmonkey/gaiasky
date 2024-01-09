/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

import java.io.Serial;

public abstract class ConcreteDuration implements Duration {
    @Serial
    private static final long serialVersionUID = 4887575731600869371L;
    protected double value;
    private TimeScale scale = TimeScale.UNKNOWN;

    @Override
    public TimeScale getScale() {
        return this.scale;
    }

    @Override
    public void setScale(TimeScale scale) {
        this.scale = scale;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#negate()
     */
    @Override
    public Duration negate() {
        value = -value;

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#mult(double)
     */
    @Override
    public Duration mult(double s) {
        value *= s;

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#isLongerThan(gaiasky.util.gaia.time.Duration)
     */
    @Override
    public boolean isLongerThan(Duration d) {
        if (d.getScale() != scale) {
            throw new RuntimeException(d.getScale().toString() + " cannot be compared to " + scale.toString());
        }

        return asNanoSecs() > d.asNanoSecs();
    }
}
