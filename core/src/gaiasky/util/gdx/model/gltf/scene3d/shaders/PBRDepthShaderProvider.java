/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.VertexAttribute;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRVertexAttributes;
import gaiasky.util.gdx.shader.DepthIntShader;
import gaiasky.util.gdx.shader.DepthIntShader.Config;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.provider.DepthIntShaderProvider;

public class PBRDepthShaderProvider extends DepthIntShaderProvider
{
	private static String defaultVertexShader = null;

	public static String getDefaultVertexShader () {
		if (defaultVertexShader == null)
			defaultVertexShader = Gdx.files.classpath("net/mgsx/gltf/shaders/depth.vs.glsl").readString();
		return defaultVertexShader;
	}

	private static String defaultFragmentShader = null;

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
	};

	
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
