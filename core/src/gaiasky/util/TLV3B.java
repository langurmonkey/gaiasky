/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.util.math.Vector3b;

public class TLV3B extends ThreadLocal<Vector3b> {
    @Override
    protected Vector3b initialValue() {
        return new Vector3b();
    }
}
