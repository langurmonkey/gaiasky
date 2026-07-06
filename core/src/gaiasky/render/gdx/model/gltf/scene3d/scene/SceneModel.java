/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.render.gdx.model.IntModel;
import gaiasky.render.gdx.model.IntNode;

public class SceneModel implements Disposable {
	public String name;
	public IntModel model;
	public ObjectMap<IntNode, Camera> cameras = new ObjectMap<>();
	public ObjectMap<IntNode, BaseLight> lights = new ObjectMap<>();
	
	@Override
	public void dispose() {
		model.dispose();
	}
}
