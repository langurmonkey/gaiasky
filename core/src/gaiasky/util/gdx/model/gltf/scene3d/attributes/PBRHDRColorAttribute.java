/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;


import gaiasky.util.gdx.shader.attribute.Attribute;

public class PBRHDRColorAttribute extends Attribute
{
	public static final String SpecularAlias = "specularColorHDR";
	public static final int Specular = register(SpecularAlias);

	public float r,g,b;
	
	public PBRHDRColorAttribute (int type, float r, float g, float b) {
		super(type);
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public PBRHDRColorAttribute set(float r, float g, float b){
		this.r = r;
		this.g = g;
		this.b = b;
		return this;
	}

	@Override
	public Attribute copy () {
		return new PBRHDRColorAttribute(index, r, g, b);
	}

	@Override
	public int compareTo (Attribute o) {
		if (index != o.index)
			return index - o.index;
		PBRHDRColorAttribute a = (PBRHDRColorAttribute)o;
		int cr = Float.compare(r, a.r);
		if(cr != 0) return cr;
		int cg = Float.compare(g, a.g);
		if(cg != 0) return cg;
		int cb = Float.compare(b, a.b);
		if(cb != 0) return cb;
		return 0;
	}

}