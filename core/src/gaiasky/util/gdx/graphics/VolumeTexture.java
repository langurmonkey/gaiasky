/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.math.Vector3;

public record VolumeTexture(Texture3D texture, int width, int height, int depth, VolumeType type, Vector3 boundsMin, Vector3 boundsMax) {
}
