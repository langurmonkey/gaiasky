/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;


import gaiasky.util.gdx.shader.attribute.Attribute;

public class PBRFlagAttribute extends Attribute {
    public static final String UnlitAlias = "unlit";
    public static final int Unlit = register(UnlitAlias);

    public PBRFlagAttribute(int index) {
        super(index);
    }

    @Override
    public Attribute copy() {
        return new PBRFlagAttribute(index);
    }

    @Override
    public int compareTo(Attribute o) {
        return index - o.index;
    }

}
