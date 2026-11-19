/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.GaiaSky;
import gaiasky.util.SysUtils;

/**
 * Selects where the positions of the elements of an orbital elements group are computed.
 * <p>
 * The GPU path solves Kepler's equation in the vertex shader, once per vertex, from orbital
 * elements that are uploaded once. It is the cheapest option by far, but it recomputes the
 * position in single precision from time to time, which some drivers (macOS) do not keep stable:
 * the elements visibly jitter.
 * <p>
 * The CPU path solves it once per element and frame, in double precision, and uploads the result.
 * The arithmetic is more expensive and costs twelve bytes per element and frame of transfer, but
 * the position is exact, so it does not jitter anywhere.
 * <p>
 * Both shader variants are compiled at startup and the choice is read when a mesh is built, so the
 * path can be changed at runtime: {@link #reset()} drops the cached resolution and the renderers
 * rebuild their meshes with the new layout.
 */
public enum ElementsPath {
    /** Pick the path that suits the platform. */
    AUTO,
    /** Compute the positions in the CPU, in double precision. No jitter, more traffic. */
    CPU,
    /** Solve the orbits in the vertex shader. Fastest, but jitters on some drivers. */
    GPU;

    /** Overrides the setting, for benchmarking. */
    private static final String PROPERTY = "gaiasky.elements.path";

    private static ElementsPath resolved;

    /**
     * Returns the path this platform prefers: the CPU on macOS, where the GPU path jitters, and the
     * GPU everywhere else, where it is much faster.
     *
     * @return The default path for the platform in use.
     */
    public static ElementsPath platformDefault() {
        return SysUtils.isMac() ? CPU : GPU;
    }

    /**
     * Returns the path that a setting value stands for, resolving {@link #AUTO}.
     *
     * @param path The setting value.
     * @return The path to use, never {@link #AUTO}.
     */
    public static ElementsPath resolve(ElementsPath path) {
        return path == null || path == AUTO ? platformDefault() : path;
    }

    /**
     * Returns the path to use, resolving {@link #AUTO} for the current platform. The result is
     * cached, because it is read once per mesh and per frame, and it must not change in the middle
     * of a frame: the layout of the mesh is built from it. Call {@link #reset()} after changing the
     * setting to pick up the new value.
     *
     * @return The path in effect.
     */
    public static ElementsPath current() {
        if (resolved == null) {
            ElementsPath path = resolve(GaiaSky.settings() != null ? GaiaSky.settings().program.elementsPath : null);
            String property = System.getProperty(PROPERTY);
            if (property != null) {
                try {
                    path = resolve(valueOf(property.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Unknown value: keep the resolved setting.
                }
            }
            resolved = path;
        }
        return resolved;
    }

    /**
     * @return Whether the element positions are solved in the vertex shader.
     */
    public static boolean isGpu() {
        return current() == GPU;
    }

    /**
     * Clears the cached resolution, so the next call to {@link #current()} reads the setting again.
     */
    public static void reset() {
        resolved = null;
    }
}
