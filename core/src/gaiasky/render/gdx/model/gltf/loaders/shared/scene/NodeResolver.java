/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.shared.scene;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.render.gdx.model.IntNode;

public class NodeResolver {
	
	ObjectMap<Integer, IntNode> nodeMap = new ObjectMap<>();
	
	public IntNode get(int index) {
		return nodeMap.get(index);
	}

	public void put(int index, IntNode node) {
		nodeMap.put(index, node);
	}

}
