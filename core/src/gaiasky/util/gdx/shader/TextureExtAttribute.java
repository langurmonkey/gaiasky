/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;

public class TextureExtAttribute extends TextureAttribute {
    public final static String AOAlias = "AOTexture";
    public final static long AO = register(AOAlias);

    public final static String RoughnessAlias = "roughnessTexture";
    public final static long Roughness = register(RoughnessAlias);

    public final static String HeightAlias = "heightTexture";
    public final static long Height = register(HeightAlias);

    static {
        TextureAttribute.Mask |= Height;
    }

    public static TextureExtAttribute createHeight(final Texture texture) {
        return new TextureExtAttribute(Height, texture);
    }

    public static TextureExtAttribute createHeight(final TextureRegion region) {
        return new TextureExtAttribute(Height, region);
    }

    public TextureExtAttribute(final long type) {
        super(type);
    }

    public <T extends Texture> TextureExtAttribute(final long type, final TextureDescriptor<T> textureDescription) {
        super(type, textureDescription);
    }

    public <T extends Texture> TextureExtAttribute(final long type, final TextureDescriptor<T> textureDescription, float offsetU, float offsetV, float scaleU, float scaleV, int uvIndex) {
        super(type, textureDescription, offsetU, offsetV, scaleU, scaleV, uvIndex);
    }

    public <T extends Texture> TextureExtAttribute(final long type, final TextureDescriptor<T> textureDescription, float offsetU, float offsetV, float scaleU, float scaleV) {
        super(type, textureDescription, offsetU, offsetV, scaleU, scaleV);
    }

    public TextureExtAttribute(final long type, final Texture texture) {
        super(type, texture);
    }

    public TextureExtAttribute(final long type, final TextureRegion region) {
        super(type, region);
    }

    public TextureExtAttribute(final TextureAttribute copyFrom) {
        super(copyFrom);
    }
}
