/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.scene;

import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.util.gdx.model.IntNode;

public class NodeResolver {
	
	ObjectMap<Integer, IntNode> nodeMap = new ObjectMap<>();
	
	public IntNode get(int index) {
		return nodeMap.get(index);
	}

	public void put(int index, IntNode node) {
		nodeMap.put(index, node);
	}

}
