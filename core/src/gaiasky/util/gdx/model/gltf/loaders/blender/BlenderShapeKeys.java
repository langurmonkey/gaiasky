/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.blender;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.util.gdx.model.gltf.data.geometry.GLTFMesh;

public class BlenderShapeKeys {

	/** Blender store shape key names in mesh extras.
	 * <pre>
	 *  "meshes" : [
          {
            "name" : "Plane",
            "extras" : {
                "targetNames" : [
                    "Water",
                    "Mountains"
                ]
            },
            "primitives" : ...,
            "weights" : [0.6, 0.3]
          }
        ]
        </pre>
	 */
	public static Array<String> parse(GLTFMesh glMesh) {
		if(glMesh.extras == null) return null;
		JsonValue targetNames = glMesh.extras.value.get("targetNames");
		if(targetNames != null && targetNames.isArray()){
			return new Array<>(targetNames.asStringArray());
		}
		return null;
	}
	
}
