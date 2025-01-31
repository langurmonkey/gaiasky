/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.gltf;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import gaiasky.util.gdx.model.gltf.data.GLTF;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFImage;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFSampler;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFTexture;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFLoaderBase;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFTypes;
import gaiasky.util.gdx.model.gltf.loaders.shared.SceneAssetLoaderParameters;
import gaiasky.util.gdx.model.gltf.loaders.shared.texture.ImageResolver;
import gaiasky.util.gdx.model.gltf.loaders.shared.texture.TextureResolver;
import gaiasky.util.gdx.model.gltf.scene3d.scene.SceneAsset;

public class GLTFAssetLoader  extends AsynchronousAssetLoader<SceneAsset, SceneAssetLoaderParameters>{

	private class ManagedTextureResolver extends TextureResolver {
		
		private final ObjectMap<Integer, AssetDescriptor<Texture>> textureDescriptorsSimple = new ObjectMap<>();
		private final ObjectMap<Integer, AssetDescriptor<Texture>> textureDescriptorsMipMap = new ObjectMap<>();
		
		private final ObjectMap<Integer, Pixmap> pixmaps = new ObjectMap<>();
		private final ObjectMap<Integer, TextureParameter> textureParameters = new ObjectMap<>();
		
		private final GLTF glModel;
		
		public ManagedTextureResolver(GLTF glModel) {
			super();
			this.glModel = glModel;
		}

		@Override
		public void loadTextures(Array<GLTFTexture> glTextures, Array<GLTFSampler> glSamplers, ImageResolver imageResolver) {
		}

		public void fetch(AssetManager manager) {
			for(Entry<Integer, AssetDescriptor<Texture>> e : textureDescriptorsSimple){
				texturesSimple.put(e.key, manager.get(e.value));
			}
			for(Entry<Integer, AssetDescriptor<Texture>> e : textureDescriptorsMipMap){
				texturesMipmap.put(e.key, manager.get(e.value));
			}
		}
		
		public void loadTextures(){
			for(Entry<Integer, TextureParameter> entry : textureResolver.textureParameters){
				TextureParameter params = entry.value;
				GLTFTexture glTexure = glTextures.get(entry.key);
				Pixmap pixmap = pixmaps.get(glTexure.source);
				Texture texture = new Texture(pixmap, params.genMipMaps);
				texture.setFilter(params.minFilter, params.magFilter);
				texture.setWrap(params.wrapU, params.wrapV);
				if(params.genMipMaps){
					texturesMipmap.put(entry.key, texture);
				}else{
					texturesSimple.put(entry.key, texture);
				}
			}
			for(Entry<Integer, Pixmap> entry : pixmaps){
				entry.value.dispose();
			}
		}

		public void getDependencies(Array<AssetDescriptor> deps) {
			this.glTextures = glModel.textures;
			this.glSamplers = glModel.samplers;
			if(glTextures != null){
				for(int i=0 ; i<glTextures.size ; i++){
					GLTFTexture glTexture = glTextures.get(i);
					
					GLTFImage glImage = glModel.images.get(glTexture.source);
					FileHandle imageFile = dataFileResolver.getImageFile(glImage);
					TextureParameter textureParameter = new TextureParameter();
					if(glTexture.sampler != null){
						GLTFSampler sampler = glSamplers.get(glTexture.sampler);
						if(GLTFTypes.isMipMapFilter(sampler)){
							textureParameter.genMipMaps = true;
						}
						GLTFTypes.mapTextureSampler(textureParameter, sampler);
					}else{
						GLTFTypes.mapTextureSampler(textureParameter);
					}
					if(imageFile == null){
						Pixmap pixmap = pixmaps.get(glTexture.source);
						if(pixmap == null){
							pixmaps.put(glTexture.source, pixmap = dataFileResolver.load(glImage));
						}
						textureParameters.put(i, textureParameter);
					}else{
						AssetDescriptor<Texture> assetDescriptor = new AssetDescriptor<>(imageFile, Texture.class, textureParameter);
						deps.add(assetDescriptor);
						if(textureParameter.genMipMaps){
							textureDescriptorsMipMap.put(glTexture.source, assetDescriptor);
						}else{
							textureDescriptorsSimple.put(glTexture.source, assetDescriptor);
						}
					}
				}
			}
		}
	}
	
	private SeparatedDataFileResolver dataFileResolver;
	private ManagedTextureResolver textureResolver;

	public GLTFAssetLoader() {
		this(new InternalFileHandleResolver());
	}
	
	public GLTFAssetLoader(FileHandleResolver resolver) {
		super(resolver);
	}

	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file,
			SceneAssetLoaderParameters parameter) {
		
		textureResolver.fetch(manager);
	}

	@Override
	public SceneAsset loadSync(AssetManager manager, String fileName, FileHandle file,
			SceneAssetLoaderParameters parameter) {
		
		final boolean withData = parameter != null && parameter.withData;
		
		textureResolver.loadTextures();
		
		GLTFLoaderBase loader = new GLTFLoaderBase(textureResolver);
		SceneAsset sceneAsset = loader.load(dataFileResolver, withData);
		
		// Delegates texture disposal to AssetManager.
		Array<String> deps = manager.getDependencies(fileName);
		if(deps != null){
			for(String depFileName : deps){
				Object dep = manager.get(depFileName);
				if(dep instanceof Texture){
					sceneAsset.textures.removeValue((Texture)dep, true);
				}
			}
		}
		
		this.textureResolver = null;
		this.dataFileResolver = null;
		return sceneAsset;
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file,
			SceneAssetLoaderParameters parameter) {
		
		Array<AssetDescriptor> deps = new Array<>();
		
		dataFileResolver = new SeparatedDataFileResolver();
		dataFileResolver.load(file);
		GLTF glModel = dataFileResolver.getRoot();
		
		textureResolver = new ManagedTextureResolver(glModel);
		textureResolver.getDependencies(deps);
		
		return deps;
	}

}
