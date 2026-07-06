/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.render.gdx.shader.attribute.Attribute;

public class PBRVolumeAttribute extends Attribute {
    public static final String Alias = "volume";
    public static final int Type = register(Alias);

    public float thicknessFactor;
    /**
     * a value of zero means positive infinity (no attenuation)
     */
    public float attenuationDistance;
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
