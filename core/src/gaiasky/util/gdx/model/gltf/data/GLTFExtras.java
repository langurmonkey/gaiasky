/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class GLTFExtras implements Serializable
{
	public JsonValue value;
	
	@Override
	public void write(Json json) {
		
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		value = jsonData;
	}
	
	/**
	 * @return a new array of extra properties keys
	 */
	public Array<String> keys(){
		Array<String> keys = new Array<String>();
		for (JsonValue entry = value.child; entry != null; entry = entry.next) {
			keys.add(entry.name);
		}
		return keys;
	}
	
	/**
	 * @return a new array of extra properties
	 */
	public Array<JsonValue> entries(){
		Array<JsonValue> entries = new Array<JsonValue>();
		for (JsonValue entry = value.child; entry != null; entry = entry.next) {
			entries.add(entry);
		}
		return entries;
	}
}
