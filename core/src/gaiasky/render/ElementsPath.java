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
 * Selects where the positions of an orbital elements group are computed: in the vertex shader
 * (fast, but jitters on some drivers such as macOS) or on the CPU, in double precision.
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
     * Returns the path this platform prefers: the CPU on macOS, the GPU everywhere else.
     */
    public static ElementsPath platformDefault() {
        return SysUtils.isMac() ? CPU : GPU;
    }

    /**
     * Resolves a setting value to a path, turning {@link #AUTO} into the platform default.
     */
    public static ElementsPath resolve(ElementsPath path) {
        return path == null || path == AUTO ? platformDefault() : path;
    }

    /**
     * Returns the path in effect, resolving and caching {@link #AUTO}. Call {@link #reset()} after
     * changing the setting.
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
     * Clears the cached resolution, so the next call to {@link #current()} reads the setting.
     */
    public static void reset() {
        resolved = null;
    }
}
