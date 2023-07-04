/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.util.gdx.shader.attribute.Attribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;

public class PBRTextureAttribute extends TextureAttribute
{
	public final static String BaseColorTextureAlias = "diffuseTexture";
	public final static int BaseColorTexture = register(BaseColorTextureAlias);
	
	public final static String EmissiveTextureAlias = "emissiveTexture";
	public final static int EmissiveTexture = register(EmissiveTextureAlias);
	
	public final static String NormalTextureAlias = "normalTexture";
	public final static int NormalTexture = register(NormalTextureAlias);
	
	public final static String MetallicRoughnessTextureAlias = "MetallicRoughnessSampler";
	public final static int MetallicRoughnessTexture = register(MetallicRoughnessTextureAlias);

	public final static String OcclusionTextureAlias = "OcclusionSampler";
	public final static int OcclusionTexture = register(OcclusionTextureAlias);
	
	// IBL environment only
	public final static String BRDFLUTTextureAlias = "brdfLUTSampler";
	public final static int BRDFLUTTexture = register(BRDFLUTTextureAlias);
	
	public final static String TransmissionTextureAlias = "TransmissionTexture";
	public final static int TransmissionTexture = register(TransmissionTextureAlias);
	
	public final static String ThicknessTextureAlias = "ThicknessTexture";
	public final static int ThicknessTexture = register(ThicknessTextureAlias);
	
	public final static String SpecularFactorTextureAlias = "SpecularFactorTexture";
	public final static int SpecularFactorTexture = register(SpecularFactorTextureAlias);
	
	public final static String IridescenceTextureAlias = "IridescenceTexture";
	public final static int IridescenceTexture = register(IridescenceTextureAlias);
	
	public final static String IridescenceThicknessTextureAlias = "IridescenceThicknessTexture";
	public final static int IridescenceThicknessTexture = register(IridescenceThicknessTextureAlias);
	
	public final static String TransmissionSourceTextureAlias = "TransmissionSourceTexture";
	public final static int TransmissionSourceTexture = register(TransmissionSourceTextureAlias);
	
	public final static String SpecularColorTextureAlias = "SpecularColorTexture";
	public final static int SpecularColorTexture = register(SpecularColorTextureAlias);
	

	public float rotationUV = 0f;
	
	public PBRTextureAttribute(int type) {
		super(type);
	}
	public PBRTextureAttribute(int type, TextureDescriptor<Texture> textureDescription) {
		super(type, textureDescription);
	}
	public PBRTextureAttribute(int type, Texture texture) {
		super(type, texture);
	}
	public PBRTextureAttribute(int type, TextureRegion region) {
		super(type, region);
	}
	public PBRTextureAttribute(PBRTextureAttribute attribute) {
		super(attribute);
		this.rotationUV = attribute.rotationUV;
	}
	public static PBRTextureAttribute createBaseColorTexture(Texture texture) {
		return new PBRTextureAttribute(BaseColorTexture, texture);
	}
	public static PBRTextureAttribute createEmissiveTexture(Texture texture) {
		return new PBRTextureAttribute(EmissiveTexture, texture);
	}
	public static PBRTextureAttribute createNormalTexture(Texture texture) {
		return new PBRTextureAttribute(NormalTexture, texture);
	}
	public static PBRTextureAttribute createMetallicRoughnessTexture(Texture texture) {
		return new PBRTextureAttribute(MetallicRoughnessTexture, texture);
	}
	public static PBRTextureAttribute createOcclusionTexture(Texture texture) {
		return new PBRTextureAttribute(OcclusionTexture, texture);
	}
	public static PBRTextureAttribute createBRDFLookupTexture(Texture texture) {
		return new PBRTextureAttribute(BRDFLUTTexture, texture);
	}
	public static PBRTextureAttribute createTransmissionTexture(Texture texture) {
		return new PBRTextureAttribute(TransmissionTexture, texture);
	}
	public static PBRTextureAttribute createThicknessTexture(Texture texture) {
		return new PBRTextureAttribute(ThicknessTexture, texture);
	}
	public static PBRTextureAttribute createSpecularFactorTexture(Texture texture) {
		return new PBRTextureAttribute(SpecularFactorTexture, texture);
	}
	public static PBRTextureAttribute createIridescenceTexture(Texture texture) {
		return new PBRTextureAttribute(IridescenceTexture, texture);
	}
	public static PBRTextureAttribute createIridescenceThicknessTexture(Texture texture) {
		return new PBRTextureAttribute(IridescenceThicknessTexture, texture);
	}
	
	public static PBRTextureAttribute createBaseColorTexture(TextureRegion region) {
		return new PBRTextureAttribute(BaseColorTexture, region);
	}
	public static PBRTextureAttribute createEmissiveTexture(TextureRegion region) {
		return new PBRTextureAttribute(EmissiveTexture, region);
	}
	public static PBRTextureAttribute createNormalTexture(TextureRegion region) {
		return new PBRTextureAttribute(NormalTexture, region);
	}
	public static PBRTextureAttribute createMetallicRoughnessTexture(TextureRegion region) {
		return new PBRTextureAttribute(MetallicRoughnessTexture, region);
	}
	public static PBRTextureAttribute createOcclusionTexture(TextureRegion region) {
		return new PBRTextureAttribute(OcclusionTexture, region);
	}
	public static PBRTextureAttribute createBRDFLookupTexture(TextureRegion region) {
		return new PBRTextureAttribute(BRDFLUTTexture, region);
	}
	public static PBRTextureAttribute createTransmissionTexture(TextureRegion region) {
		return new PBRTextureAttribute(TransmissionTexture, region);
	}
	public static PBRTextureAttribute createThicknessTexture(TextureRegion region) {
		return new PBRTextureAttribute(ThicknessTexture, region);
	}
	public static PBRTextureAttribute createSpecularFactorTexture(TextureRegion region) {
		return new PBRTextureAttribute(SpecularFactorTexture, region);
	}
	public static PBRTextureAttribute createIridescenceTexture(TextureRegion region) {
		return new PBRTextureAttribute(IridescenceTexture, region);
	}
	public static PBRTextureAttribute createIridescenceThicknessTexture(TextureRegion region) {
		return new PBRTextureAttribute(IridescenceThicknessTexture, region);
	}

	@Override
	public Attribute copy() {
		return new PBRTextureAttribute(this);
	}
	
	@Override
	public int compareTo(Attribute o) {
		int r = super.compareTo(o);
		if(r != 0) return r;
		if(o instanceof PBRTextureAttribute){
			PBRTextureAttribute other = (PBRTextureAttribute)o;
			if(!MathUtils.isEqual(rotationUV, other.rotationUV)) return rotationUV < other.rotationUV ? -1 : 1;
		}
		return 0;
	}
}
