/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.scene;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.gltf.data.GLTFEntity;

public class GLTFNode extends GLTFEntity {
	
	public Array<Integer> children;
	public float [] matrix;
	public float [] translation;
	public float [] rotation;
	public float [] scale;
	
	public Integer mesh;
	public Integer camera;
	public Integer skin;
	
	public float [] weights;
}
