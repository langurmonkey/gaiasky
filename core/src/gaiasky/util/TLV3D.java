/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.util.math.Vector3D;

/**
 * Thread local variable holding {@link Vector3D}.
 */
public class TLV3D extends ThreadLocal<Vector3D> {
    @Override
    protected Vector3D initialValue() {
        return new Vector3D();
    }
}
