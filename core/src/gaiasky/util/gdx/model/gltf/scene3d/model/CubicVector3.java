/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.model;

import com.badlogic.gdx.math.Vector3;

@SuppressWarnings("serial")
public class CubicVector3 extends Vector3
{
	public final Vector3 tangentIn = new Vector3();
	public final Vector3 tangentOut = new Vector3();
	
}