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
    PARTICLE,
    /** Extended particles, with proper motions, colors and sizes. **/
    PARTICLE_EXT,
    /** Stars. **/
    STAR,
    /** Variable stars. **/
    VARIABLE,
    /** Fake particle record, not implemented by this class! **/
    FAKE;

}
