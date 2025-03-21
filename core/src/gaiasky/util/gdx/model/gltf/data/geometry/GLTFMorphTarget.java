/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.geometry;

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
