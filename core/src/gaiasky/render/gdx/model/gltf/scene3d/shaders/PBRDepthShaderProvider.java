/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.VertexAttribute;
import gaiasky.render.gdx.IntRenderable;
import gaiasky.render.gdx.model.gltf.scene3d.attributes.PBRVertexAttributes;
import gaiasky.render.gdx.shader.DepthIntShader;
import gaiasky.render.gdx.shader.DepthIntShader.Config;
import gaiasky.render.gdx.shader.IntShader;
import gaiasky.render.gdx.shader.provider.DepthIntShaderProvider;

public class PBRDepthShaderProvider extends DepthIntShaderProvider
{
	private static String defaultVertexShader;

	public static String getDefaultVertexShader () {
		if (defaultVertexShader == null)
			defaultVertexShader = Gdx.files.classpath("net/mgsx/gltf/shaders/depth.vs.glsl").readString();
		return defaultVertexShader;
	}

	private static String defaultFragmentShader;

	public static String getDefaultFragmentShader () {
		if (defaultFragmentShader == null)
			defaultFragmentShader = Gdx.files.classpath("net/mgsx/gltf/shaders/depth.fs.glsl").readString();
		return defaultFragmentShader;
	}

	public static Config createDefaultConfig() {
		Config config = new Config();
		config.vertexShaderCode = getDefaultVertexShader();
		config.fragmentShaderCode = getDefaultFragmentShader();
		return config;
	}


    public PBRDepthShaderProvider(Config config) {
		super(config == null ? new Config() : config);
		if(config.vertexShaderCode == null) config.vertexShaderCode = getDefaultVertexShader();
		if(config.fragmentShaderCode == null) config.fragmentShaderCode = getDefaultFragmentShader();
	}

	protected String morphTargetsPrefix(IntRenderable renderable){
		String prefix = "";
		for(VertexAttribute att : renderable.meshPart.mesh.getVertexAttributes()){
			for(int i = 0; i< PBRCommon.MAX_MORPH_TARGETS ; i++){
				if(att.usage == PBRVertexAttributes.Usage.PositionTarget && att.unit == i){
					prefix += "#define " + "position" + i + "Flag\n";
				}
			}
		}
		return prefix;
	}
	
	@Override
	protected IntShader createShader(IntRenderable renderable) {
		
		// TODO only count used attributes, depth shader only require a few of them.
		PBRCommon.checkVertexAttributes(renderable);
		
		return new PBRDepthShader(renderable, config, DepthIntShader.createPrefix(renderable, config) + morphTargetsPrefix(renderable));
	}
}
