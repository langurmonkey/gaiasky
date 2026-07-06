/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FlushablePool;
import gaiasky.render.gdx.IntModelBatch;
import gaiasky.render.gdx.IntRenderable;
import gaiasky.render.gdx.IntRenderableProvider;
import gaiasky.render.gdx.IntRenderableSorter;
import gaiasky.render.gdx.model.gltf.scene3d.attributes.PBRFloatAttribute;
import gaiasky.render.gdx.model.gltf.scene3d.attributes.PBRTextureAttribute;
import gaiasky.render.gdx.shader.Environment;
import gaiasky.render.gdx.shader.provider.IntShaderProvider;

public class TransmissionSource implements Disposable {

	private final IntModelBatch batch;
	private FrameBuffer fbo;
	private int width;
	private int height;
	private boolean hasTransmission;
	private Camera camera;
	
	/** attribute to be added to the environment in the final render pass. */
	public final PBRTextureAttribute attribute = new PBRTextureAttribute(PBRTextureAttribute.TransmissionSourceTexture);
	
	private final Array<IntRenderable> allRenderables = new Array<>();
	private final Array<IntRenderable> selectedRenderables = new Array<>();
	private final FlushablePool<IntRenderable> renderablePool = new FlushablePool<>() {
		@Override
		protected IntRenderable newObject() {
			return new IntRenderable();
		}

		@Override
		public IntRenderable obtain () {
			IntRenderable renderable = super.obtain();
			renderable.environment = null;
			renderable.material = null;
			renderable.meshPart.set("", null, 0, 0, 0);
			renderable.shader = null;
			renderable.userData = null;
			return renderable;
		}
	};
	
	public TransmissionSource(IntShaderProvider shaderProvider) {
		this(shaderProvider, new SceneRenderableSorter());
		
	}
	
	public TransmissionSource(IntShaderProvider shaderProvider, IntRenderableSorter sorter) {
		batch = new IntModelBatch(shaderProvider, sorter);
		attribute.textureDescription.minFilter = TextureFilter.MipMap;
		attribute.textureDescription.magFilter = TextureFilter.Linear;
		
	}
	
	protected FrameBuffer createFrameBuffer(int width, int height){
		return new FrameBuffer(Format.RGBA8888, width, height, true);
	}
	
	/**
	 * Set transmission source frame buffer size (usually the same as the final render resolution).
	 * 
	 * @param width when set to zero, default back buffer width will be used.
	 * @param height when set to zero, default back buffer height will be used.
	 */
	public void setSize(int width, int height){
		this.width = width;
		this.height = height;
	}
	
	public void begin(Camera camera){
		this.camera = camera;
		ensureFrameBufferSize(width, height);
		this.hasTransmission = false;
	}
	
	private void ensureFrameBufferSize(int width, int height) {
		if(width <= 0) width = Gdx.graphics.getBackBufferWidth();
		if(height <= 0) height = Gdx.graphics.getBackBufferHeight();
		
		if(fbo == null || fbo.getWidth() != width || fbo.getHeight() != height){
			if(fbo != null) fbo.dispose();
			fbo = createFrameBuffer(width, height);
		}
		
	}

	public void render(Iterable<IntRenderableProvider> providers, Environment environment){
		for(IntRenderableProvider provider : providers){
			render(provider, environment);
		}
	}
	
	public void render(IntRenderableProvider provider, Environment environment) {
		int start = allRenderables.size;
		provider.getRenderables(allRenderables, renderablePool);
		for(int i=start ; i<allRenderables.size ; i++){
			IntRenderable renderable = allRenderables.get(i);
			if(shouldBeRendered(renderable)){
				renderable.environment = environment;
				selectedRenderables.add(renderable);
			}
		}
	}
	public void render(IntRenderableProvider provider) {
		int start = allRenderables.size;
		provider.getRenderables(allRenderables, renderablePool);
		for(int i=start ; i<allRenderables.size ; i++){
			IntRenderable renderable = allRenderables.get(i);
			if(shouldBeRendered(renderable)){
				selectedRenderables.add(renderable);
			}
		}
	}

	public void end(){
		if(hasTransmission){
			fbo.begin();
			Gdx.gl.glClearColor(0, 0, 0, 0);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			batch.begin(camera);
			
			for(IntRenderable renderable : selectedRenderables){
				batch.render(renderable);
			}
			
			batch.end();
			fbo.end();
			
			// gen mipmaps for roughness simulation
			Texture texture = fbo.getColorBufferTexture();
			texture.bind();
			Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D);
		}

		attribute.textureDescription.texture = fbo.getColorBufferTexture();
		
		renderablePool.flush();
		selectedRenderables.clear();
		allRenderables.clear();
	}

	private boolean shouldBeRendered(IntRenderable renderable) {
		// we consider having a texture or having a strictly positive factor as transmitting material
		boolean hasTransmission = renderable.material.has(PBRTextureAttribute.TransmissionTexture) ||
			(renderable.material.has(PBRFloatAttribute.TransmissionFactor)
					&& renderable.material.get(PBRFloatAttribute.class, PBRFloatAttribute.TransmissionFactor).value > 0);
		this.hasTransmission |= hasTransmission;
		return !hasTransmission;
	}

	@Override
	public void dispose() {
		if(fbo != null) fbo.dispose();
		batch.dispose();
	}
}
