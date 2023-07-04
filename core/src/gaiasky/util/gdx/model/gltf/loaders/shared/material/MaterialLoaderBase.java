/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.material;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.gltf.data.material.GLTFMaterial;
import gaiasky.util.gdx.model.gltf.loaders.shared.texture.TextureResolver;
import gaiasky.util.gdx.shader.Material;

abstract public class MaterialLoaderBase implements MaterialLoader {
	protected TextureResolver textureResolver;
	private final Array<Material> materials = new Array<>();
	private final Material defaultMaterial;
	
	public MaterialLoaderBase(TextureResolver textureResolver, Material defaultMaterial) {
		super();
		this.textureResolver = textureResolver;
		this.defaultMaterial = defaultMaterial;
	}
	
	@Override
	public Material getDefaultMaterial() {
		return defaultMaterial;
	}

	@Override
	public Material get(int index) {
		return materials.get(index);
	}

	@Override
	public void loadMaterials(Array<GLTFMaterial> glMaterials) {
		if(glMaterials != null){
			for(int i=0 ; i<glMaterials.size ; i++){
				GLTFMaterial glMaterial = glMaterials.get(i);
				Material material = loadMaterial(glMaterial);
				materials.add(material);
			}
		}
	}

	abstract protected Material loadMaterial(GLTFMaterial glMaterial);
	
}
