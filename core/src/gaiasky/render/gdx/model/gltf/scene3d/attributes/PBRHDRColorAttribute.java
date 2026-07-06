/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;


import gaiasky.render.gdx.shader.attribute.Attribute;

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
        return cb;
    }

}