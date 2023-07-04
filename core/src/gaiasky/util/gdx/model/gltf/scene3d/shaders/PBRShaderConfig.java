/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.shaders;

import gaiasky.util.gdx.shader.DefaultIntShader;

public class PBRShaderConfig extends DefaultIntShader.Config
{
	public enum SRGB{NONE,FAST,ACCURATE}
	/**
	 * Enable conversion of SRGB space textures into linear space in shader.
	 * Should be {@link SRGB#NONE} if your textures are already in linear space
	 * or automatically converted by OpenGL when using {@link com.badlogic.gdx.graphics.GL30#GL_SRGB} format.
	 */
	public SRGB manualSRGB = SRGB.ACCURATE;
	
	/**
	 * Enable conversion of SRGB space frame buffer into linear space in shader for transmission source.
	 * Should be {@link SRGB#NONE} if the transmission frame buffer is already in linear space.
	 * By default, transmission source is considered rendered with gamma correction (sRGB space).
	 */
	public SRGB transmissionSRGB = SRGB.ACCURATE;

	/**
	 * Enable conversion of SRGB space frame buffer into linear space in shader for mirror source.
	 * Should be {@link SRGB#NONE} if the mirror frame buffer is already in linear space.
	 * By default, mirror source is considered rendered with gamma correction (sRGB space).
	 */
	public SRGB mirrorSRGB = SRGB.ACCURATE;

	/**
	 * Enable/Disable gamma correction.
	 * Since gamma correction should only be done once as a final step,
	 * this should be disabled when you want to apply it later (eg. in case of post process lighting calculation).
	 * It also should be disabled when drawing to SRGB framebuffers since gamma correction will
	 * be automatically done by OpenGL.
	 * Default is true.
	 */
	public boolean manualGammaCorrection = true;
	
	/** Default gamma factor that gives good results on most monitors. */
	public static final float DEFAULT_GAMMA = 2.2f;
	
	/**
	 * Gamma value used when {@link #manualGammaCorrection} is enabled.
	 * Default is 2.2 which is a standard value that gives good results on most monitors
	 */
	public float gamma = DEFAULT_GAMMA;
	
	/** string to prepend to shaders (version), automatic if null */
	public String glslVersion = null;

	/** Max vertex color layers. Default {@link PBRShader} only use 1 layer,
	 * custom shaders can implements more.
	 */
	public int numVertexColors = 1;

	/**
	 * Some custom GLSL code to inject in shaders.
	 * If not null it will be added after #version 
	 */
	public String prefix = null;
}
