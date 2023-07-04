/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

public class PBRVertexAttributes
{
	// based on VertexAttributes maximum (biNormal = 256)
	public static final class Usage {
		public static final int PositionTarget = 512;
		public static final int NormalTarget = 1024;
		public static final int TangentTarget = 2048;
	}
}
