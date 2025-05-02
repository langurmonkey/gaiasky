/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.math.Vector3;

/**
 * Thread local variable holding {@link Vector3}.
 */
public class TLV3 extends ThreadLocal<Vector3> {
    @Override
    protected Vector3 initialValue() {
        return new Vector3();
    }
}
