/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.utils;


import gaiasky.render.gdx.shader.Environment;
import gaiasky.render.gdx.shader.attribute.Attribute;

public class EnvironmentCache extends Environment {

	/**
	 * fast way to copy only references
	 */
	public void setCache(Environment env){
		this.mask = env.getMask();
		this.attributes.clear();
		for(Attribute a : env) this.attributes.add(a);
		this.shadowMap  = env.shadowMap;
		this.sorted = true;
	}

	/**
	 * fast way to replace an attribute without sorting
	 */
	public void replaceCache(Attribute attribute) {
		int idx = indexOf(attribute.index);
		this.attributes.set(idx, attribute);
	}
}
