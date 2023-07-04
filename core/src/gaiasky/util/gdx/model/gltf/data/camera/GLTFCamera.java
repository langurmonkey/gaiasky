/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.camera;

import gaiasky.util.gdx.model.gltf.data.GLTFEntity;

public class GLTFCamera extends GLTFEntity {
	public String type;
	public GLTFPerspective perspective;
	public GLTFOrthographic orthographic;
}
