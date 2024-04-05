/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop87;

public class MarsVSOP87 extends AbstractVSOP87 {
    public MarsVSOP87() {
        super();
    }
    public double[] getData(double tau) {
        if (vsop87 != null) {
            return vsop87.getMars(tau);
        } else {
            return null;
        }
    }
}
