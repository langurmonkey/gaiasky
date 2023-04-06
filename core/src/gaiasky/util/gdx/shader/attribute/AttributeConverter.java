package gaiasky.util.gdx.shader.attribute;

import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

public class AttributeConverter {

    public static Attribute convert(com.badlogic.gdx.graphics.g3d.Attribute attrib) {
        if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute) {

            return new BlendingAttribute((com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute) attrib);
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute) {
            return new ColorAttribute((com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute) attrib);
        } else if(attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute){
            return new TextureAttribute((com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) attrib);
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute) {
            return new FloatAttribute((com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute) attrib);
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.IntAttribute) {
            return new IntAttribute((com.badlogic.gdx.graphics.g3d.attributes.IntAttribute) attrib);
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute) {
            return new DirectionalLightsAttribute((com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute) attrib);
        } else if (attrib instanceof com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute) {
            return new DepthTestAttribute((com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute) attrib);
        }

        return null;
    }
}
