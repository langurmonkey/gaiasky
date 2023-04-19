/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.utils;

public interface DiffnFunctionNs {
    /**
     * @param t time
     * @param y the set of dependent variables
     *
     * @return double[] derivatives of y with respect to time
     */
    double[] derivn(final long t, final double[] y);
}
