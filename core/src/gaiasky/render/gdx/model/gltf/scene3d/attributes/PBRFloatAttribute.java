/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

import gaiasky.render.gdx.shader.attribute.Attribute;
import gaiasky.render.gdx.shader.attribute.FloatAttribute;

public class PBRFloatAttribute extends FloatAttribute
{
	public final static String MetallicAlias = "Metallic";
	public final static int Metallic = register(MetallicAlias);
	
	public final static String RoughnessAlias = "Roughness";
	public final static int Roughness = register(RoughnessAlias);
	
	public final static String NormalScaleAlias = "NormalScale";
	public final static int NormalScale = register(NormalScaleAlias);
	
	public final static String OcclusionStrengthAlias = "OcclusionStrength";
	public final static int OcclusionStrength = register(OcclusionStrengthAlias);
	
	public final static String ShadowBiasAlias = "ShadowBias";
	public final static int ShadowBias = register(ShadowBiasAlias);
	
	public final static String EmissiveIntensityAlias = "EmissiveIntensity";
	public final static int EmissiveIntensity = register(EmissiveIntensityAlias);
	
	public final static String TransmissionFactorAlias = "TransmissionFactor";
	public final static int TransmissionFactor = register(TransmissionFactorAlias);

	public final static String IORAlias = "IOR";
	public final static int IOR = register(IORAlias);
	
	public final static String SpecularFactorAlias = "SpecularFactor";
	public final static int SpecularFactor = register(SpecularFactorAlias);
	
	public PBRFloatAttribute(int index, float value) {
		super(index, value);
	}

	
	@Override
	public Attribute copy () {
		return new PBRFloatAttribute(index, value);
	}

	public static Attribute createMetallic(float value) {
		return new PBRFloatAttribute(Metallic, value);
	}
	public static Attribute createRoughness(float value) {
		return new PBRFloatAttribute(Roughness, value);
	}
	public static Attribute createNormalScale(float value) {
		return new PBRFloatAttribute(NormalScale, value);
	}
	public static Attribute createOcclusionStrength(float value) {
		return new PBRFloatAttribute(OcclusionStrength, value);
	}
	public static Attribute createEmissiveIntensity(float value) {
		return new PBRFloatAttribute(EmissiveIntensity, value);
	}
	public static Attribute createTransmissionFactor(float value) {
		return new PBRFloatAttribute(TransmissionFactor, value);
	}
	public static Attribute createIOR(float value) {
		return new PBRFloatAttribute(IOR, value);
	}
	public static Attribute createSpecularFactor(float value) {
		return new PBRFloatAttribute(SpecularFactor, value);
	}
}
