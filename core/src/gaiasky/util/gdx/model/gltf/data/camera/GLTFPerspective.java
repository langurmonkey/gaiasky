/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.camera;

import gaiasky.util.gdx.model.gltf.data.GLTFObject;

public class GLTFPerspective extends GLTFObject {
	public float yfov, znear;
	public Float aspectRatio, zfar;
}
