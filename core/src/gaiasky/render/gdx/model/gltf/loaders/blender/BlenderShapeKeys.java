/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.blender;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.render.gdx.model.gltf.data.geometry.GLTFMesh;

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
