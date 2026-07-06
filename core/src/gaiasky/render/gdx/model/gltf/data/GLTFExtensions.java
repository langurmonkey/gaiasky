/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;

public class GLTFExtensions implements Serializable{

	private static final Json json = new Json();
	
	private JsonValue value;
	private final ObjectMap<String, Object> extentions = new ObjectMap<String, Object>();

	@Override
	public void write(Json json) {
		for (Entry<String, Object> extension : extentions) {
			json.writeValue(extension.key, extension.value);
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		value = jsonData;
	}
	
	public <T> T get(Class<T> type, String ext) 
	{
		T result = (T)extentions.get(ext);
		if(result == null && value != null){
			result = json.readValue(type, value.get(ext));
			extentions.put(ext, result);
		}
		return result;
	}
	
	public void set(String ext, Object object){
		extentions.put(ext, object);
	}
}
