/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.animation;

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.model.IntNodeAnimation;
import gaiasky.render.gdx.model.gltf.loaders.shared.animation.Interpolation;
import gaiasky.render.gdx.model.gltf.scene3d.model.WeightVector;

public class NodeAnimationHack extends IntNodeAnimation
{
	public Interpolation translationMode;
	public Interpolation rotationMode;
	public Interpolation scalingMode;
	public Interpolation weightsMode;
	
	public Array<NodeKeyframe<WeightVector>> weights;
}
