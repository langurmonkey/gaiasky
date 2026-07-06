/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.scene;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.model.gltf.data.GLTFEntity;

public class GLTFNode extends GLTFEntity {
	
	public Array<Integer> children;
	public float [] matrix;
	public float [] translation;
	public float [] rotation;
	public float [] scale;
	
	public Integer mesh;
	public Integer camera;
	public Integer skin;
	
	public float [] weights;
}
