/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.model.IntAnimation;
import gaiasky.util.gdx.model.gltf.data.GLTF;

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
