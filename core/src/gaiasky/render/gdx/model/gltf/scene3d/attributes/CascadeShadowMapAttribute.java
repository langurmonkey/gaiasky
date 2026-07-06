/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

import gaiasky.render.gdx.model.gltf.scene3d.scene.CascadeShadowMap;
import gaiasky.render.gdx.shader.attribute.Attribute;

public class CascadeShadowMapAttribute extends Attribute
{
	public static final String Alias = "CSM";
	public static final int Type = register(Alias);
	
	public final CascadeShadowMap cascadeShadowMap;
	
	public CascadeShadowMapAttribute(CascadeShadowMap cascadeShadowMap) {
		super(Type);
		this.cascadeShadowMap = cascadeShadowMap;
	}
	@Override
	public int compareTo(Attribute o) {
		return index - o.index;
	}

	@Override
	public Attribute copy() {
		return new CascadeShadowMapAttribute(cascadeShadowMap);
	}

}
