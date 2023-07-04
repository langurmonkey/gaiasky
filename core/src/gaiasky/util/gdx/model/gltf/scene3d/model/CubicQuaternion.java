/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.model;

import com.badlogic.gdx.math.Quaternion;

@SuppressWarnings("serial")
public class CubicQuaternion extends Quaternion
{
	public final Quaternion tangentIn = new Quaternion();
	public final Quaternion tangentOut = new Quaternion();

}
