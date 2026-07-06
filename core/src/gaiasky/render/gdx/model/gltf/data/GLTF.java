/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.model.gltf.data.animation.GLTFAnimation;
import gaiasky.render.gdx.model.gltf.data.camera.GLTFCamera;
import gaiasky.render.gdx.model.gltf.data.data.GLTFAccessor;
import gaiasky.render.gdx.model.gltf.data.data.GLTFBuffer;
import gaiasky.render.gdx.model.gltf.data.data.GLTFBufferView;
import gaiasky.render.gdx.model.gltf.data.geometry.GLTFMesh;
import gaiasky.render.gdx.model.gltf.data.material.GLTFMaterial;
import gaiasky.render.gdx.model.gltf.data.scene.GLTFNode;
import gaiasky.render.gdx.model.gltf.data.scene.GLTFScene;
import gaiasky.render.gdx.model.gltf.data.scene.GLTFSkin;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFImage;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFSampler;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFTexture;

public class GLTF extends GLTFObject {
	public GLTFAsset asset;
	public int scene;
	public Array<GLTFScene> scenes;
	public Array<GLTFNode> nodes;
	public Array<GLTFCamera> cameras;
	public Array<GLTFMesh> meshes;
	
	public Array<GLTFImage> images;
	public Array<GLTFSampler> samplers;
	public Array<GLTFTexture> textures;
	
	public Array<GLTFAnimation> animations;
	public Array<GLTFSkin> skins;

	public Array<GLTFAccessor> accessors;
	public Array<GLTFMaterial> materials;
	public Array<GLTFBufferView> bufferViews;
	public Array<GLTFBuffer> buffers;
	
	public Array<String> extensionsUsed;
	public Array<String> extensionsRequired;
}
