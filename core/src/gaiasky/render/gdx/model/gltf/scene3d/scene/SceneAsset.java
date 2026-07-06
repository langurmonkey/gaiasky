/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.gdx.mesh.IntMesh;
import gaiasky.render.gdx.model.IntAnimation;
import gaiasky.render.gdx.model.gltf.data.GLTF;

public class SceneAsset implements Disposable
{
	/** underlying GLTF data structure, null if loaded without "withData" option. */
	public GLTF data;
	
	public Array<SceneModel> scenes;
	public SceneModel scene;

	public Array<IntAnimation> animations;
	public int maxBones;
	
	/** Keep track of loaded texture in order to dispose them. Textures handled by AssetManager are excluded. */
	public Array<Texture> textures;
	
	/** Keep track of loaded pixmaps in order to dispose them. Pixmaps handled by AssetManager are excluded. */
	public Array<Pixmap> pixmaps;
	
	/** Keep track of loaded meshes in order to dispose them. */
	public Array<IntMesh> meshes;
	
	@Override
	public void dispose() {
		if(scenes != null){
			for(SceneModel scene : scenes){
				scene.dispose();
			}
		}
		if(textures != null){
			for(Texture texture : textures){
				texture.dispose();
			}
		}
		if(pixmaps != null){
			for(Pixmap pixmap : pixmaps){
				pixmap.dispose();
			}
		}
		if(meshes != null){
			for(IntMesh mesh : meshes){
				mesh.dispose();
			}
		}
	}
}
