/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.Matrix4;

import java.nio.FloatBuffer;

public class Matrix4Utils {

    public static void put(Matrix4 m, FloatBuffer buffer) {
        get(m, buffer.position(), buffer);
    }

    public static void get(Matrix4 m, int offset, FloatBuffer src) {
        m.val[0] = src.get(offset);
        m.val[1] = src.get(offset + 1);
        m.val[2] = src.get(offset + 2);
        m.val[3] = src.get(offset + 3);

        m.val[4] = src.get(offset + 4);
        m.val[5] = src.get(offset + 5);
        m.val[6] = src.get(offset + 6);
        m.val[7] = src.get(offset + 7);

        m.val[8] = src.get(offset + 8);
        m.val[9] = src.get(offset + 9);
        m.val[10] = src.get(offset + 10);
        m.val[11] = src.get(offset + 11);

        m.val[12] = src.get(offset + 12);
        m.val[13] = src.get(offset + 13);
        m.val[14] = src.get(offset + 14);
        m.val[15] = src.get(offset + 15);
    }
}
