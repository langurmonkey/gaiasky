/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.shared.material;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.model.gltf.data.material.GLTFMaterial;
import gaiasky.render.gdx.model.gltf.loaders.shared.texture.TextureResolver;
import gaiasky.render.gdx.shader.Material;

abstract public class MaterialLoaderBase implements MaterialLoader {
	protected TextureResolver textureResolver;
	private final Array<Material> materials = new Array<>();
	private final Material defaultMaterial;
	
	protected MaterialLoaderBase(TextureResolver textureResolver, Material defaultMaterial) {
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
