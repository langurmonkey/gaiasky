/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

/**
 * Enumeration to identify the type of record.
 */
public enum ParticleType {
    /** Simple positional particles. **/
    PARTICLE(3, 0, new int[]{0, 1, 2}, new int[]{}),
    /** Stars. **/
    STAR(3, 12, new int[]{0, 1, 2}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}),
    /** Extended particles, with proper motions, colors and sizes. **/
    PARTICLE_EXT(3, 10, new int[]{0, 1, 2}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}),
    /** Fake particle record, not implemented by this class! **/
    FAKE(0, 0, null, null);

    final public int doubleArraySize, floatArraySize;
    final int[] doubleIndexIndirection, floatIndexIndirection;

    ParticleType(int doubleArraySize,
                       int floatArraySize,
                       int[] doubleIndexIndirection,
                       int[] floatIndexIndirection) {
        this.doubleArraySize = doubleArraySize;
        this.floatArraySize = floatArraySize;
        this.doubleIndexIndirection = doubleIndexIndirection;
        this.floatIndexIndirection = floatIndexIndirection;
    }
}
