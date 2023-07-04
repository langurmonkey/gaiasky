/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;

public class GLTFExtensions implements Serializable{

	private static final Json json = new Json();
	
	private JsonValue value;
	private ObjectMap<String, Object> extentions = new ObjectMap<String, Object>();

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
