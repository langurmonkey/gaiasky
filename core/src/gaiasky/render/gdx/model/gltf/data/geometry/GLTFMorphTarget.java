/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.geometry;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.JsonIterator;
import com.badlogic.gdx.utils.ObjectMap;

public class GLTFMorphTarget extends ObjectMap<String, Integer> implements Serializable {

	@Override
	public void write(Json json) {
		for (Entry<String, Integer> entry : this) {
			json.writeValue(entry.key, entry.value);
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		for(JsonIterator i = jsonData.iterator(); i.hasNext() ; ){
			JsonValue e = i.next();
			put(e.name, e.asInt());
		}
	}
	
}
