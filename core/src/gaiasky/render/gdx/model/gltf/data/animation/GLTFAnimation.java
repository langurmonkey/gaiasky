/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.animation;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.model.gltf.data.GLTFEntity;

public class GLTFAnimation extends GLTFEntity {
	public Array<GLTFAnimationChannel> channels;
	public Array<GLTFAnimationSampler> samplers;
}
