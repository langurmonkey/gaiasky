/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.geometry;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.render.gdx.model.gltf.data.GLTFObject;

public class GLTFPrimitive extends GLTFObject {
	public ObjectMap<String, Integer> attributes;
	public Integer indices;
	public Integer mode;
	public Integer material;
	public Array<GLTFMorphTarget> targets;
	
}
