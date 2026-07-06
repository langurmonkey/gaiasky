/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.gltf;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.render.gdx.model.gltf.loaders.shared.GLTFLoaderBase;
import gaiasky.render.gdx.model.gltf.scene3d.scene.SceneAsset;

public class GLTFLoader extends GLTFLoaderBase
{
	public SceneAsset load(FileHandle glFile){
		return load(glFile, false);
	}
	public SceneAsset load(FileHandle glFile, boolean withData){
		SeparatedDataFileResolver dataFileResolver = new SeparatedDataFileResolver();
		dataFileResolver.load(glFile);
		return load(dataFileResolver, withData);
	}

}
