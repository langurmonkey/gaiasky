/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.geometry;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.util.gdx.model.gltf.data.GLTFObject;

public class GLTFPrimitive extends GLTFObject {
	public ObjectMap<String, Integer> attributes;
	public Integer indices;
	public Integer mode;
	public Integer material;
	public Array<GLTFMorphTarget> targets;
	
}
