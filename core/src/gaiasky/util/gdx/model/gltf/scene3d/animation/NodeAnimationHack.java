/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.animation;

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.IntNodeAnimation;
import gaiasky.util.gdx.model.gltf.loaders.shared.animation.Interpolation;
import gaiasky.util.gdx.model.gltf.scene3d.model.WeightVector;

public class NodeAnimationHack extends IntNodeAnimation
{
	public Interpolation translationMode;
	public Interpolation rotationMode;
	public Interpolation scalingMode;
	public Interpolation weightsMode;
	
	public Array<NodeKeyframe<WeightVector>> weights = null;
}
