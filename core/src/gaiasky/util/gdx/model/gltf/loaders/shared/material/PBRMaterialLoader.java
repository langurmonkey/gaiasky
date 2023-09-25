/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.material;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.util.gdx.model.gltf.data.extensions.*;
import gaiasky.util.gdx.model.gltf.data.material.GLTFMaterial;
import gaiasky.util.gdx.model.gltf.data.material.GLTFpbrMetallicRoughness;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFTextureInfo;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFIllegalException;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFTypes;
import gaiasky.util.gdx.model.gltf.loaders.shared.texture.TextureResolver;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRFloatAttribute;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRIridescenceAttribute;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRTextureAttribute;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRVolumeAttribute;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.*;

public class PBRMaterialLoader extends MaterialLoaderBase {

    public PBRMaterialLoader(TextureResolver textureResolver) {
        super(textureResolver, new Material());
    }

    @Override
    public Material loadMaterial(GLTFMaterial glMaterial) {
        Material material = new Material();
        if (glMaterial.name != null) material.id = glMaterial.name;

        if (glMaterial.emissiveFactor != null && glMaterial.emissiveFactor[0] != 0 && glMaterial.emissiveFactor[1] != 0 && glMaterial.emissiveFactor[2] != 0) {
            material.set(new ColorAttribute(ColorAttribute.Emissive, GLTFTypes.mapColor(glMaterial.emissiveFactor, Color.BLACK)));
        }

        if (glMaterial.emissiveTexture != null) {
            material.set(getTextureMap(TextureAttribute.Emissive, glMaterial.emissiveTexture));
        }

        if (glMaterial.doubleSided == Boolean.TRUE) {
            material.set(IntAttribute.createCullFace(0)); // 0 to disable culling
        }

        if (glMaterial.normalTexture != null) {
            material.set(getTextureMap(TextureAttribute.Normal, glMaterial.normalTexture));
            // TODO Activate
            //if (glMaterial.normalTexture.scale != 1.0) {
            //    material.set(PBRFloatAttribute.createNormalScale(glMaterial.normalTexture.scale));
            //}
        }

        if (glMaterial.occlusionTexture != null) {
            material.set(getTextureMap(TextureAttribute.AO, glMaterial.occlusionTexture));
            // TODO Activate
            //if (glMaterial.occlusionTexture.strength != 1.0) {
            //    material.set(PBRFloatAttribute.createOcclusionStrength(glMaterial.occlusionTexture.strength));
            //}
        }

        boolean alphaBlend = false;
        if ("OPAQUE".equals(glMaterial.alphaMode)) {
            // nothing to do
        } else if ("MASK".equals(glMaterial.alphaMode)) {
            float value = glMaterial.alphaCutoff == null ? 0.5f : glMaterial.alphaCutoff;
            material.set(FloatAttribute.createAlphaTest(value));
            material.set(new BlendingAttribute()); // necessary
        } else if ("BLEND".equals(glMaterial.alphaMode)) {
            material.set(new BlendingAttribute()); // opacity is set by pbrMetallicRoughness below
            alphaBlend = true;
        } else if (glMaterial.alphaMode != null) {
            throw new GLTFIllegalException("unknown alpha mode : " + glMaterial.alphaMode);
        }

        if (glMaterial.pbrMetallicRoughness != null) {
            GLTFpbrMetallicRoughness p = glMaterial.pbrMetallicRoughness;

            if (p.baseColorFactor != null) {
                Color baseColorFactor = GLTFTypes.mapColor(p.baseColorFactor, Color.WHITE);
                material.set(new ColorAttribute(ColorAttribute.Diffuse, baseColorFactor));
            }

            if (p.metallicFactor != 0) {
                material.set(new ColorAttribute(ColorAttribute.Metallic, p.metallicFactor));
            }
            if (p.roughnessFactor != 1) {
                material.set(new ColorAttribute(ColorAttribute.Roughness, p.roughnessFactor));
            }

            if (p.metallicRoughnessTexture != null) {
                material.set(getTextureMap(TextureAttribute.OcclusionMetallicRoughness, p.metallicRoughnessTexture));
            }

            if (p.baseColorTexture != null) {
                material.set(getTextureMap(TextureAttribute.Diffuse, p.baseColorTexture));
            }

            if (alphaBlend) {
                material.get(BlendingAttribute.class, BlendingAttribute.Type).opacity = 1.0f;
            }
        }

        // can have both PBR base and ext
        if (glMaterial.extensions != null) {
            {
                KHRMaterialsPBRSpecularGlossiness ext = glMaterial.extensions.get(KHRMaterialsPBRSpecularGlossiness.class, KHRMaterialsPBRSpecularGlossiness.EXT);
                if (ext != null) {
                    Gdx.app.error("GLTF", KHRMaterialsPBRSpecularGlossiness.EXT + " extension is deprecated by glTF 2.0 specification and not fully supported.");

                    material.set(new ColorAttribute(ColorAttribute.Diffuse, GLTFTypes.mapColor(ext.diffuseFactor, Color.WHITE)));
                    material.set(new ColorAttribute(ColorAttribute.Specular, GLTFTypes.mapColor(ext.specularFactor, Color.WHITE)));

                    // not sure how to map normalized gloss to exponent ...
                    material.set(new FloatAttribute(FloatAttribute.Shininess, MathUtils.lerp(1, 100, ext.glossinessFactor)));
                    if (ext.diffuseTexture != null) {
                        material.set(getTextureMap(TextureAttribute.Diffuse, ext.diffuseTexture));
                    }
                    if (ext.specularGlossinessTexture != null) {
                        material.set(getTextureMap(TextureAttribute.Specular, ext.specularGlossinessTexture));
                    }
                }
            }
            {
                KHRMaterialsUnlit ext = glMaterial.extensions.get(KHRMaterialsUnlit.class, KHRMaterialsUnlit.EXT);
                // TODO Activate
                //if (ext != null) {
                //    material.set(new PBRFlagAttribute(PBRFlagAttribute.Unlit));
                //}
            }
            {
                KHRMaterialsTransmission ext = glMaterial.extensions.get(KHRMaterialsTransmission.class, KHRMaterialsTransmission.EXT);
                if (ext != null) {
                    material.set(PBRFloatAttribute.createTransmissionFactor(ext.transmissionFactor));
                    if (ext.transmissionTexture != null) {
                        material.set(getTextureMapPBR(PBRTextureAttribute.TransmissionTexture, ext.transmissionTexture));
                    }
                }
            }
            {
                KHRMaterialsVolume ext = glMaterial.extensions.get(KHRMaterialsVolume.class, KHRMaterialsVolume.EXT);
                if (ext != null) {
                    material.set(new PBRVolumeAttribute(ext.thicknessFactor, ext.attenuationDistance == null ? 0f : ext.attenuationDistance, GLTFTypes.mapColor(ext.attenuationColor, Color.WHITE)));
                    // TODO Activate
                    //if (ext.thicknessTexture != null) {
                    //    material.set(getTextureMapPBR(PBRTextureAttribute.ThicknessTexture, ext.thicknessTexture));
                    //}
                }
            }
            {
                KHRMaterialsIOR ext = glMaterial.extensions.get(KHRMaterialsIOR.class, KHRMaterialsIOR.EXT);
                // TODO Activate
                //if (ext != null) {
                //    material.set(PBRFloatAttribute.createIOR(ext.ior));
                //}
            }
            {
                KHRMaterialsSpecular ext = glMaterial.extensions.get(KHRMaterialsSpecular.class, KHRMaterialsSpecular.EXT);
                if (ext != null) {
                    // TODO Activate
                    //material.set(PBRFloatAttribute.createSpecularFactor(ext.specularFactor));
                    material.set(new ColorAttribute(ColorAttribute.Specular, ext.specularColorFactor[0], ext.specularColorFactor[1], ext.specularColorFactor[2], 1f));
                    if (ext.specularTexture != null) {
                        material.set(getTextureMap(TextureAttribute.Specular, ext.specularTexture));
                    } else if (ext.specularColorTexture != null) {
                        // Watch out, specular color texture is not the same as specular texture!
                        // Usually, the color of the specular reflection depends on the light and the diffuse color of the surface.
                        material.set(getTextureMap(TextureAttribute.Specular, ext.specularColorTexture));
                    }
                }
            }
            {
                KHRMaterialsIridescence ext = glMaterial.extensions.get(KHRMaterialsIridescence.class, KHRMaterialsIridescence.EXT);
                if (ext != null) {
                    // TODO Activate
                    material.set(new PBRIridescenceAttribute(ext.iridescenceFactor, ext.iridescenceIor, ext.iridescenceThicknessMinimum, ext.iridescenceThicknessMaximum));
                    if (ext.iridescenceTexture != null) {
                        material.set(getTextureMapPBR(PBRTextureAttribute.IridescenceTexture, ext.iridescenceTexture));
                    }
                    if (ext.iridescenceThicknessTexture != null) {
                        material.set(getTextureMapPBR(PBRTextureAttribute.IridescenceThicknessTexture, ext.iridescenceThicknessTexture));
                    }
                }
            }
            {
                KHRMaterialsEmissiveStrength ext = glMaterial.extensions.get(KHRMaterialsEmissiveStrength.class, KHRMaterialsEmissiveStrength.EXT);
                if (ext != null) {
                    material.set(new ColorAttribute(ColorAttribute.Emissive, ext.emissiveStrength));
                }
            }
        }

        return material;
    }

    protected TextureAttribute getTextureMap(int type, GLTFTextureInfo glMap) {
        TextureDescriptor<Texture> textureDescriptor = textureResolver.getTexture(glMap);

        TextureAttribute attribute = new TextureAttribute(type, textureDescriptor);
        attribute.uvIndex = glMap.texCoord;

        if (glMap.extensions != null) {
            {
                KHRTextureTransform ext = glMap.extensions.get(KHRTextureTransform.class, KHRTextureTransform.EXT);
                if (ext != null) {
                    attribute.offsetU = ext.offset[0];
                    attribute.offsetV = ext.offset[1];
                    attribute.scaleU = ext.scale[0];
                    attribute.scaleV = ext.scale[1];
                    //attribute.rotation = ext.rotation;
                    if (ext.texCoord != null) {
                        attribute.uvIndex = ext.texCoord;
                    }
                }
            }
        }

        return attribute;
    }

    protected PBRTextureAttribute getTextureMapPBR(int type, GLTFTextureInfo glMap) {
        TextureDescriptor<Texture> textureDescriptor = textureResolver.getTexture(glMap);

        PBRTextureAttribute attribute = new PBRTextureAttribute(type, textureDescriptor);
        attribute.uvIndex = glMap.texCoord;

        if (glMap.extensions != null) {
            {
                KHRTextureTransform ext = glMap.extensions.get(KHRTextureTransform.class, KHRTextureTransform.EXT);
                if (ext != null) {
                    attribute.offsetU = ext.offset[0];
                    attribute.offsetV = ext.offset[1];
                    attribute.scaleU = ext.scale[0];
                    attribute.scaleV = ext.scale[1];
                    attribute.rotationUV = ext.rotation;
                    if (ext.texCoord != null) {
                        attribute.uvIndex = ext.texCoord;
                    }
                }
            }
        }

        return attribute;
    }

}
