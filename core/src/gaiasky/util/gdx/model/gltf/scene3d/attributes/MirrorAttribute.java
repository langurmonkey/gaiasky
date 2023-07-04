/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;


import gaiasky.util.gdx.shader.attribute.Attribute;

public class MirrorAttribute extends Attribute
{
	public static final String SpecularAlias = "specularMirror";
	public static final int Specular = register(SpecularAlias);
	
	public static MirrorAttribute createSpecular(){
		return new MirrorAttribute(Specular);
	}
	
	public MirrorAttribute(int index) {
		super(index);
	}

	@Override
	public int compareTo(Attribute o) {
		return index - o.index;
	}

	@Override
	public Attribute copy() {
		return new MirrorAttribute(index);
	}

}
