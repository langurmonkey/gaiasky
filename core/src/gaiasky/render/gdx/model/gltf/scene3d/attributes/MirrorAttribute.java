/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;


import gaiasky.render.gdx.shader.attribute.Attribute;

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
