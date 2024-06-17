/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.shaders;

import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRColorAttribute;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import net.jafama.FastMath;

public class PBREmissiveShaderProvider extends PBRShaderProvider
{
	
	public PBREmissiveShaderProvider(PBRShaderConfig config) {
		super(config);
	}

	@Override
	protected IntShader createShader(IntRenderable renderable) {
		
		PBRShaderConfig config = (PBRShaderConfig)this.config;
		
		// if material has some alpha settings, emissive is impacted and albedo is required.
		boolean hasAlpha = renderable.material.has(BlendingAttribute.Type) || renderable.material.has(FloatAttribute.AlphaTest);
		
		String prefix = createPrefixBase(renderable, config);
		
		prefix += morphTargetsPrefix(renderable);
		
		prefix += createPrefixSRGB(renderable, config);
		
		// optional base color factor
		if(renderable.material.has(PBRColorAttribute.BaseColorFactor)){
			prefix += "#define baseColorFactorFlag\n";
		}
		
		int maxUVIndex = 0;
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, TextureAttribute.Emissive);
			if(attribute != null){
				prefix += "#define v_emissiveUV v_texCoord" + attribute.uvIndex + "\n";
				maxUVIndex = FastMath.max(maxUVIndex, attribute.uvIndex);
			}
		}
		if(hasAlpha){
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, TextureAttribute.Diffuse);
			if(attribute != null){
				prefix += "#define v_diffuseUV v_texCoord" + attribute.uvIndex + "\n";
				maxUVIndex = FastMath.max(maxUVIndex, attribute.uvIndex);
			}
		}
		
		
		if(maxUVIndex >= 0){
			prefix += "#define textureFlag\n";
		}
		if(maxUVIndex == 1){
			prefix += "#define textureCoord1Flag\n";
		}else if(maxUVIndex > 1){
			throw new GdxRuntimeException("more than 2 texture coordinates attribute not supported");
		}
		
		PBRShader shader = new PBRShader(renderable, config, prefix);
		checkShaderCompilation(shader.program);
		
		// prevent infinite loop
		if(!shader.canRender(renderable)){
			throw new GdxRuntimeException("cannot render with this shader");
		}
		
		return shader;
	}

	public static PBRShaderConfig createConfig(int maxBones) {
		return null;
	}
}
