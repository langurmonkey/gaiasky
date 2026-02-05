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
    /** Simple positional particles. Corresponds to {@link Particle}. **/
    PARTICLE,
    /** Extended particles, with proper motions, colors and sizes. Corresponds to {@link ParticleExt}. **/
    PARTICLE_EXT,
    /** Stars. Corresponds to {@link ParticleStar}. **/
    STAR,
    /** Variable stars. Corresponds to {@link ParticleVariable}. **/
    VARIABLE,
    /** Particle whose location is described as Keplerian orbital elements. **/
    KEPLER,
    /** Particle implemented as a double array. Can hold anything. Corresponds to {@link ParticleVector}. **/
    VECTOR;
}
