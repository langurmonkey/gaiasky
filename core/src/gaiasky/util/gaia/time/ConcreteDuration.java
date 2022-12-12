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
 * A {@code ConcreteDuration} is a duration that is linked to a time scale
 *
 * @author ulammers
 * @version $Id: ConcreteDuration.java 328492 2013-11-11 12:38:59Z ulammers $
 */
public abstract class ConcreteDuration implements Duration {
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
