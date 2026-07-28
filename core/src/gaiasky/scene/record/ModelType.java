/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import java.util.Locale;

/**
 * Enum to represent model types.
 */
public enum ModelType {
    SPHERE, UVSPHERE,
    ICOSPHERE,
    CUBESPHERE,
    OCTAHEDRONSPHERE,
    PLANE, PATCH, SURFACE, BILLBOARD, QUAD,
    DISC,
    TWOFACEDBILLBOARD,
    CYLINDER,
    RING,
    CONE,
    CUBE, BOX;

    public static ModelType from(String name) {
        try {
            return ModelType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static ModelType from(String name, ModelType defaultValue) {
        try {
            return ModelType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

    public boolean isUVSphere() {
        return this == SPHERE || this == UVSPHERE;
    }
    public boolean isAnySphere() {
        return this == SPHERE || this == UVSPHERE || this == ICOSPHERE || this == CUBESPHERE || this == OCTAHEDRONSPHERE;
    }

    public boolean isRing() {
        return this == RING;
    }

    public boolean isQuad() {
        return this == PLANE || this == PATCH || this == SURFACE || this == BILLBOARD || this == QUAD;
    }
}
