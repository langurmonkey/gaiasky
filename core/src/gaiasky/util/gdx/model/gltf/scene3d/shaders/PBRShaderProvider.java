/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.shaders;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.*;
import gaiasky.util.gdx.model.gltf.scene3d.shaders.PBRShaderConfig.SRGB;
import gaiasky.util.gdx.model.gltf.scene3d.utils.LightUtils;
import gaiasky.util.gdx.model.gltf.scene3d.utils.LightUtils.LightsInfo;
import gaiasky.util.gdx.model.gltf.scene3d.utils.ShaderParser;
import gaiasky.util.gdx.shader.DefaultIntShader;
import gaiasky.util.gdx.shader.DepthIntShader.Config;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.Matrix4Attribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.gdx.shader.provider.DefaultIntShaderProvider;
import gaiasky.util.gdx.shader.provider.DepthIntShaderProvider;

public class PBRShaderProvider extends DefaultIntShaderProvider
{
	public static final String TAG = "PBRShader";
	
	private static final LightsInfo lightsInfo = new LightsInfo();
	
	private static String defaultVertexShader = null;

	public static String getDefaultVertexShader () {
		if (defaultVertexShader == null)
			defaultVertexShader = ShaderParser.parse(Gdx.files.classpath("net/mgsx/gltf/shaders/pbr/pbr.vs.glsl"));
		return defaultVertexShader;
	}

	private static String defaultFragmentShader = null;

	public static String getDefaultFragmentShader () {
		if (defaultFragmentShader == null)
			defaultFragmentShader = ShaderParser.parse(Gdx.files.classpath("net/mgsx/gltf/shaders/pbr/pbr.fs.glsl"));
		return defaultFragmentShader;
	}

	
	public static PBRShaderConfig createDefaultConfig() {
		PBRShaderConfig config = new PBRShaderConfig();
		config.vertexShaderCode = getDefaultVertexShader();
		config.fragmentShaderCode = getDefaultFragmentShader();
		return config;
	};
	
	public static Config createDefaultDepthConfig() {
		return PBRDepthShaderProvider.createDefaultConfig();
	};
	
	public static PBRShaderProvider createDefault(int maxBones){
		PBRShaderConfig config = createDefaultConfig();
		config.numBones = maxBones;
		return createDefault(config);
	}
	
	public static PBRShaderProvider createDefault(PBRShaderConfig config){
		return new PBRShaderProvider(config);
	}
	
	public static DepthIntShaderProvider createDefaultDepth(int maxBones){
		Config config = createDefaultDepthConfig();
		config.numBones = maxBones;
		return createDefaultDepth(config);
	}
	
	public static DepthIntShaderProvider createDefaultDepth(Config config){
		return new PBRDepthShaderProvider(config);
	}
	
	public PBRShaderProvider(PBRShaderConfig config) {
		super(config == null ? createDefaultConfig() : config);
		if(this.config.vertexShaderCode == null) this.config.vertexShaderCode = getDefaultVertexShader();
		if(this.config.fragmentShaderCode == null) this.config.fragmentShaderCode = getDefaultFragmentShader();
	}
	
	public int getShaderCount(){
		return shaders.size;
	}
	
	public static String morphTargetsPrefix(IntRenderable renderable){
		StringBuilder prefix = new StringBuilder();
		// TODO optimize double loop
		for(VertexAttribute att : renderable.meshPart.mesh.getVertexAttributes()){
			for(int i = 0; i< PBRCommon.MAX_MORPH_TARGETS ; i++){
				if(att.usage == PBRVertexAttributes.Usage.PositionTarget && att.unit == i){
					prefix.append("#define " + "position").append(i).append("Flag\n");
				}else if(att.usage == PBRVertexAttributes.Usage.NormalTarget && att.unit == i){
					prefix.append("#define " + "normal").append(i).append("Flag\n");
				}else if(att.usage == PBRVertexAttributes.Usage.TangentTarget && att.unit == i){
					prefix.append("#define " + "tangent").append(i).append("Flag\n");
				}
			}
		}
		return prefix.toString();
	}
	
	/**
	 * @return if target platform is running with at least OpenGL ES 3 (GLSL 300 es), WebGL 2.0 (GLSL 300 es)
	 *  or desktop OpenGL 3.0 (GLSL 130).
	 */
	protected boolean isGL3(){
		return Gdx.graphics.getGLVersion().isVersionEqualToOrHigher(3, 0);
	}
	
	/**
	 * override this method in order to add your own prefix.
	 */
	public String createPrefixBase(IntRenderable renderable, PBRShaderConfig config) {
		
		String defaultPrefix = DefaultIntShader.createPrefix(renderable, config);
		String version = config.glslVersion;
		if(isGL3()){
			if(Gdx.app.getType() == ApplicationType.Desktop){
				if(version == null)
					version = """
							#version 130
							#define GLSL3
							""";
			}else if(Gdx.app.getType() == ApplicationType.Android || 
					Gdx.app.getType() == ApplicationType.iOS ||
					Gdx.app.getType() == ApplicationType.WebGL){
				if(version == null)
					version = """
							#version 300 es
							#define GLSL3
							""";
			}
		}
		String prefix = "";
		if(version != null) prefix += version;
		if(config.prefix != null) prefix += config.prefix;
		prefix += defaultPrefix;
		
		return prefix;
	}
	
	public String createPrefixSRGB(IntRenderable renderable, PBRShaderConfig config){
		String prefix = "";
		if(config.manualSRGB != SRGB.NONE){
			prefix += "#define MANUAL_SRGB\n";
			if(config.manualSRGB == SRGB.FAST){
				prefix += "#define SRGB_FAST_APPROXIMATION\n";
			}
		}
		if(config.manualGammaCorrection){
			prefix += "#define GAMMA_CORRECTION " + config.gamma + "\n";
		}
		if(config.transmissionSRGB != SRGB.NONE){
			prefix += "#define TS_MANUAL_SRGB\n";
			if(config.transmissionSRGB == SRGB.FAST){
				prefix += "#define TS_SRGB_FAST_APPROXIMATION\n";
			}
		}
		if(config.mirrorSRGB != SRGB.NONE){
			prefix += "#define MS_MANUAL_SRGB\n";
			if(config.mirrorSRGB == SRGB.FAST){
				prefix += "#define MS_SRGB_FAST_APPROXIMATION\n";
			}
		}
		return prefix;
	}
	
	protected IntShader createShader(IntRenderable renderable) {
		
		PBRShaderConfig config = (PBRShaderConfig)this.config;
		
		StringBuilder prefix = new StringBuilder(createPrefixBase(renderable, config));
		
		// Morph targets
		prefix.append(morphTargetsPrefix(renderable));
		
		// optional base color factor
		if(renderable.material.has(PBRColorAttribute.BaseColorFactor)){
			prefix.append("#define baseColorFactorFlag\n");
		}
		
		// Lighting
		int primitiveType = renderable.meshPart.primitiveType;
		boolean isLineOrPoint = primitiveType == GL20.GL_POINTS || primitiveType == GL20.GL_LINES || primitiveType == GL20.GL_LINE_LOOP || primitiveType == GL20.GL_LINE_STRIP;
		boolean unlit = isLineOrPoint || renderable.material.has(PBRFlagAttribute.Unlit) || renderable.meshPart.mesh.getVertexAttribute(Usage.Normal) == null;
		
		if(unlit){
			
			prefix.append("#define unlitFlag\n");
			
		}else{
			
			if(renderable.material.has(PBRTextureAttribute.MetallicRoughnessTexture)){
				prefix.append("#define metallicRoughnessTextureFlag\n");
			}
			if(renderable.material.has(PBRTextureAttribute.OcclusionTexture)){
				prefix.append("#define occlusionTextureFlag\n");
			}
			if(renderable.material.has(PBRFloatAttribute.TransmissionFactor)){
				prefix.append("#define transmissionFlag\n");
			}
			if(renderable.material.has(PBRTextureAttribute.TransmissionTexture)){
				prefix.append("#define transmissionTextureFlag\n");
			}
			if(renderable.material.has(PBRVolumeAttribute.Type)){
				prefix.append("#define volumeFlag\n");
			}
			if(renderable.material.has(PBRTextureAttribute.ThicknessTexture)){
				prefix.append("#define thicknessTextureFlag\n");
			}
			if(renderable.material.has(PBRFloatAttribute.IOR)){
				prefix.append("#define iorFlag\n");
			}
			
			// Material specular
			boolean hasSpecular = false;
			if(renderable.material.has(PBRFloatAttribute.SpecularFactor)){
				prefix.append("#define specularFactorFlag\n");
				hasSpecular = true;
			}
			if(renderable.material.has(PBRHDRColorAttribute.Specular)){
				hasSpecular = true;
				prefix.append("#define specularColorFlag\n");
			}
			if(renderable.material.has(PBRTextureAttribute.SpecularFactorTexture)){
				prefix.append("#define specularFactorTextureFlag\n");
				hasSpecular = true;
			}
			if(renderable.material.has(PBRTextureAttribute.SpecularColorTexture)){
				prefix.append("#define specularColorTextureFlag\n");
				hasSpecular = true;
			}
			if(hasSpecular){
				prefix.append("#define specularFlag\n");
			}
			
			// Material Iridescence
			if(renderable.material.has(PBRIridescenceAttribute.Type)){
				prefix.append("#define iridescenceFlag\n");
			}
			if(renderable.material.has(PBRTextureAttribute.IridescenceTexture)){
				prefix.append("#define iridescenceTextureFlag\n");
			}
			if(renderable.material.has(PBRTextureAttribute.IridescenceThicknessTexture)){
				prefix.append("#define iridescenceThicknessTextureFlag\n");
			}
			if(renderable.environment.has(ClippingPlaneAttribute.Type)){
				prefix.append("#define clippingPlaneFlag\n");
			}
			CascadeShadowMapAttribute csm = renderable.environment.get(CascadeShadowMapAttribute.class, CascadeShadowMapAttribute.Type);
			if(csm != null){
				prefix.append("#define numCSM ").append(csm.cascadeShadowMap.lights.size).append("\n");
			}
			
			// IBL options
			PBRCubemapAttribute specualarCubemapAttribute = null;
			MirrorAttribute specularMirrorAttribute = null;
			if(renderable.environment != null){
				if(renderable.environment.has(PBRTextureAttribute.TransmissionSourceTexture)){
					prefix.append("#define transmissionSourceFlag\n");
				}
				if(renderable.environment.has(PBRCubemapAttribute.SpecularEnv)){
					prefix.append("#define diffuseSpecularEnvSeparateFlag\n");
					specualarCubemapAttribute = renderable.environment.get(PBRCubemapAttribute.class, PBRCubemapAttribute.SpecularEnv);
				}else if(renderable.environment.has(PBRCubemapAttribute.DiffuseEnv)){
					specualarCubemapAttribute = renderable.environment.get(PBRCubemapAttribute.class, PBRCubemapAttribute.DiffuseEnv);
				}else if(renderable.environment.has(PBRCubemapAttribute.ReflectionCubemap)){
					specualarCubemapAttribute = renderable.environment.get(PBRCubemapAttribute.class, PBRCubemapAttribute.ReflectionCubemap);
				}
				
				if(renderable.environment.has(MirrorSourceAttribute.Type) && renderable.material.has(MirrorAttribute.Specular)){
					specularMirrorAttribute = renderable.environment.get(MirrorAttribute.class, MirrorAttribute.Specular);
					prefix.append("#define mirrorSpecularFlag\n");
				}
				
				if(specualarCubemapAttribute != null || specularMirrorAttribute != null){
					prefix.append("#define USE_IBL\n");
					
					boolean textureLodSupported;
					if(isGL3()){
						textureLodSupported = true;
					}else if(Gdx.graphics.supportsExtension("EXT_shader_texture_lod")){
						prefix.append("#define USE_TEXTURE_LOD_EXT\n");
						textureLodSupported = true;
					}else{
						textureLodSupported = false;
					}
					
					if(specualarCubemapAttribute != null){
						TextureFilter textureFilter = specualarCubemapAttribute.textureDescription.minFilter != null ? specualarCubemapAttribute.textureDescription.minFilter : specualarCubemapAttribute.textureDescription.texture.getMinFilter();
						if(textureLodSupported && textureFilter.equals(TextureFilter.MipMap)){
							prefix.append("#define USE_TEX_LOD\n");
						}
					}
					
					if(renderable.environment.has(PBRTextureAttribute.BRDFLUTTexture)){
						prefix.append("#define brdfLUTTexture\n");
					}
				}
				// TODO check GLSL extension 'OES_standard_derivatives' for WebGL
				
				if(renderable.environment.has(ColorAttribute.AmbientLight)){
					prefix.append("#define ambientLightFlag\n");
				}
				
				if(renderable.environment.has(Matrix4Attribute.EnvRotation)){
					prefix.append("#define ENV_ROTATION\n");
				}
			}
			
		}
		
		// SRGB
		prefix.append(createPrefixSRGB(renderable, config));
		
		
		// multi UVs
		int maxUVIndex = -1;
		
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, TextureAttribute.Diffuse);
			if(attribute != null){
				prefix.append("#define v_diffuseUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, TextureAttribute.Emissive);
			if(attribute != null){
				prefix.append("#define v_emissiveUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, TextureAttribute.Normal);
			if(attribute != null){
				prefix.append("#define v_normalUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, PBRTextureAttribute.MetallicRoughnessTexture);
			if(attribute != null){
				prefix.append("#define v_metallicRoughnessUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, TextureAttribute.AO);
			if(attribute != null){
				prefix.append("#define v_occlusionUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, PBRTextureAttribute.TransmissionTexture);
			if(attribute != null){
				prefix.append("#define v_transmissionUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, PBRTextureAttribute.ThicknessTexture);
			if(attribute != null){
				prefix.append("#define v_thicknessUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, PBRTextureAttribute.SpecularFactorTexture);
			if(attribute != null){
				prefix.append("#define v_specularFactorUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, TextureAttribute.Specular);
			if(attribute != null){
				prefix.append("#define v_specularColorUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, PBRTextureAttribute.IridescenceTexture);
			if(attribute != null){
				prefix.append("#define v_iridescenceUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		{
			TextureAttribute attribute = renderable.material.get(TextureAttribute.class, PBRTextureAttribute.IridescenceThicknessTexture);
			if(attribute != null){
				prefix.append("#define v_iridescenceThicknessUV v_texCoord").append(attribute.uvIndex).append("\n");
				maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex);
			}
		}
		
		if(maxUVIndex >= 0){
			prefix.append("#define textureFlag\n");
		}
		if(maxUVIndex == 1){
			prefix.append("#define textureCoord1Flag\n");
		}else if(maxUVIndex > 1){
			throw new GdxRuntimeException("more than 2 texture coordinates attribute not supported");
		}
		
		// Fog
		
		if(renderable.environment != null && renderable.environment.has(FogAttribute.FogEquation)){
			prefix.append("#define fogEquationFlag\n");
		}
		
		
		// colors
		for(VertexAttribute attribute : renderable.meshPart.mesh.getVertexAttributes()){
			if(attribute.usage == Usage.ColorUnpacked){
				prefix.append("#define color").append(attribute.unit).append("Flag\n");
			}
		}
		
		// 
		
		int numBoneInfluence = 0;
		int numMorphTarget = 0;
		int numColor = 0;
		
		for(VertexAttribute attribute : renderable.meshPart.mesh.getVertexAttributes()){
			if(attribute.usage == Usage.ColorPacked){
				throw new GdxRuntimeException("color packed attribute not supported");
			}else if(attribute.usage == Usage.ColorUnpacked){
				numColor = Math.max(numColor, attribute.unit+1);
			}else if(attribute.usage == PBRVertexAttributes.Usage.PositionTarget && attribute.unit >= PBRCommon.MAX_MORPH_TARGETS ||
					attribute.usage == PBRVertexAttributes.Usage.NormalTarget && attribute.unit >= PBRCommon.MAX_MORPH_TARGETS ||
					attribute.usage == PBRVertexAttributes.Usage.TangentTarget && attribute.unit >= PBRCommon.MAX_MORPH_TARGETS ){
				numMorphTarget = Math.max(numMorphTarget, attribute.unit+1);
			}else if(attribute.usage == Usage.BoneWeight){
				numBoneInfluence = Math.max(numBoneInfluence, attribute.unit+1);
			}
		}
		
		
		PBRCommon.checkVertexAttributes(renderable);
		
		if(numBoneInfluence > 8){
			Gdx.app.error(TAG, "more than 8 bones influence attributes not supported: " + numBoneInfluence + " found.");
		}
		if(numMorphTarget > PBRCommon.MAX_MORPH_TARGETS){
			Gdx.app.error(TAG, "more than 8 morph target attributes not supported: " + numMorphTarget + " found.");
		}
		if(numColor > config.numVertexColors){
			Gdx.app.error(TAG, "more than " + config.numVertexColors + " color attributes not supported: " + numColor + " found.");
		}
		
		if(renderable.environment != null){
			LightUtils.getLightsInfo(lightsInfo, renderable.environment);
			if(lightsInfo.dirLights > config.numDirectionalLights){
				Gdx.app.error(TAG, "too many directional lights detected: " + lightsInfo.dirLights + "/" + config.numDirectionalLights);
			}
			if(lightsInfo.pointLights > config.numPointLights){
				Gdx.app.error(TAG, "too many point lights detected: " + lightsInfo.pointLights + "/" + config.numPointLights);
			}
			if(lightsInfo.spotLights > config.numSpotLights){
				Gdx.app.error(TAG, "too many spot lights detected: " + lightsInfo.spotLights + "/" + config.numSpotLights);
			}
			if(lightsInfo.miscLights > 0){
				Gdx.app.error(TAG, "unknow type lights not supported.");
			}
		}
		
		PBRShader shader = createShader(renderable, config, prefix.toString());
		checkShaderCompilation(shader.program);
		
		// prevent infinite loop (TODO remove this for libgdx 1.9.12+)
		if(!shader.canRender(renderable)){
			throw new GdxRuntimeException("cannot render with this shader");
		}
		
		return shader;
	}
	
	/**
	 * override this method in order to provide your own PBRShader subclass.
	 */
	protected PBRShader createShader(IntRenderable renderable, PBRShaderConfig config, String prefix){
		return new PBRShader(renderable, config, prefix);
	}

	protected void checkShaderCompilation(ExtShaderProgram program){
		String shaderLog = program.getLog();
		if(program.isCompiled()){
			if(shaderLog.isEmpty()){
				Gdx.app.debug(TAG, "Shader compilation success");
			}else{
				Gdx.app.error(TAG, "Shader compilation warnings:\n" + shaderLog);
			}
		}else{
			throw new GdxRuntimeException("Shader compilation failed:\n" + shaderLog);
		}
	}
}
