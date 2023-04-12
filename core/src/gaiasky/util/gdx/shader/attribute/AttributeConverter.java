package gaiasky.util.gdx.shader.attribute;

import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;

public class AttributeConverter {

    public static Attribute convert(com.badlogic.gdx.graphics.g3d.Attribute attrib) {
        if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute) {
            var attribute = new BlendingAttribute((com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute) attrib);
            if (attribute.index >= 0) {
                return attribute;
            }
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute) {
            var attribute = new ColorAttribute((com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute) attrib);
            if (attribute.index >= 0) {
                return attribute;
            }
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) {
            var attribute = new TextureAttribute((com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) attrib);
            if (attribute.index >= 0) {
                return attribute;
            }
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute) {
            if (attrib instanceof PBRFloatAttribute) {
                var pbrAttribute = (PBRFloatAttribute) attrib;
                if (pbrAttribute.type == PBRFloatAttribute.Metallic) {
                    return new ColorAttribute(ColorAttribute.Metallic, pbrAttribute.value);
                } else if (pbrAttribute.type == PBRFloatAttribute.Roughness) {
                    return new ColorAttribute(ColorAttribute.Roughness, pbrAttribute.value);
                }
            }
            var attribute = new FloatAttribute((com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute) attrib);
            if (attribute.index >= 0) {
                return attribute;
            }
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.IntAttribute) {
            var attribute = new IntAttribute((com.badlogic.gdx.graphics.g3d.attributes.IntAttribute) attrib);
            if (attribute.index >= 0) {
                return attribute;
            }
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute) {
            var attribute = new DirectionalLightsAttribute((com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute) attrib);
            if (attribute.index >= 0) {
                return attribute;
            }
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute) {
            var attribute = new DepthTestAttribute((com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute) attrib);
            if (attribute.index >= 0) {
                return attribute;
            }
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.CubemapAttribute) {
            var attribute = new CubemapAttribute((com.badlogic.gdx.graphics.g3d.attributes.CubemapAttribute) attrib);
            if (attribute.index >= 0) {
                return attribute;
            }
        }

        return null;
    }
}
