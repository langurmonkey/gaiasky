/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.utils;


import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.attribute.Attribute;

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
		final int idx = indexOf(attribute.index);
		this.attributes.set(idx, attribute);
	}
}
