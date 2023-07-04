/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.util.gdx.shader.attribute.Attribute;

public class PBRVolumeAttribute extends Attribute {
    public static final String Alias = "volume";
    public static final int Type = register(Alias);

    public float thicknessFactor = 0f;
    /**
     * a value of zero means positive infinity (no attenuation)
     */
    public float attenuationDistance = 0f;
    public final Color attenuationColor = new Color(Color.WHITE);

    public PBRVolumeAttribute() {
        super(Type);
    }

    public PBRVolumeAttribute(float thicknessFactor, float attenuationDistance, Color attenuationColor) {
        super(Type);
        this.thicknessFactor = thicknessFactor;
        this.attenuationDistance = attenuationDistance;
        this.attenuationColor.set(attenuationColor);
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        PBRVolumeAttribute other = (PBRVolumeAttribute) o;
        if (!MathUtils.isEqual(thicknessFactor, other.thicknessFactor))
            return thicknessFactor < other.thicknessFactor ? -1 : 1;
        if (!MathUtils.isEqual(attenuationDistance, other.attenuationDistance))
            return attenuationDistance < other.attenuationDistance ? -1 : 1;
        return attenuationColor.toIntBits() - other.attenuationColor.toIntBits();

    }

    @Override
    public Attribute copy() {
        return new PBRVolumeAttribute(thicknessFactor, attenuationDistance, attenuationColor);
    }


}
