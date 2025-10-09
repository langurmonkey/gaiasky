/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

public class Texture3DAttribute extends Attribute {
    public final static String VolumeDensityAlias = "volumeDensityTexture";
    public final static int VolumeDensity = register(VolumeDensityAlias);

    public final static String Texture3d0Alias = "texture3d0";
    public final static int Texture3d0 = register(Texture3d0Alias);
    public final static String Texture3d1Alias = "texture3d1";
    public final static int Texture3d1 = register(Texture3d1Alias);

    public final TextureDescriptor<Texture3D> textureDescription;
    public float offsetU = 0;
    public float offsetV = 0;
    public float offsetW = 0;
    public float scaleU = 1;
    public float scaleV = 1;
    public float scaleW = 1;

    public Texture3DAttribute(final int index) {
        super(index);
        textureDescription = new TextureDescriptor<>();
    }

    public <T extends Texture3D> Texture3DAttribute(final int index, final TextureDescriptor<T> textureDescription) {
        this(index);
        this.textureDescription.set(textureDescription);
    }

    public <T extends Texture3D> Texture3DAttribute(final int index, final TextureDescriptor<T> textureDescription, float offsetU,
                                                  float offsetV, float offsetW, float scaleU, float scaleV, float scaleW) {
        this(index, textureDescription);
        this.offsetU = offsetU;
        this.offsetV = offsetV;
        this.offsetW = offsetW;
        this.scaleU = scaleU;
        this.scaleV = scaleV;
        this.scaleW = scaleW;
    }


    public Texture3DAttribute(final int index, final Texture3D texture) {
        this(index);
        textureDescription.texture = texture;
    }

    public Texture3DAttribute(final Texture3DAttribute copyFrom) {
        this(copyFrom.index,
             copyFrom.textureDescription,
             copyFrom.offsetU,
             copyFrom.offsetV,
             copyFrom.offsetW,
             copyFrom.scaleU,
             copyFrom.scaleV,
             copyFrom.scaleW);
    }

    public static Texture3DAttribute createVolumeDensity(final Texture3D texture) {
        return new Texture3DAttribute(VolumeDensity, texture);
    }


    @Override
    public Attribute copy() {
        return new Texture3DAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 991 * result + textureDescription.hashCode();
        result = 991 * result + NumberUtils.floatToRawIntBits(offsetU);
        result = 991 * result + NumberUtils.floatToRawIntBits(offsetV);
        result = 991 * result + NumberUtils.floatToRawIntBits(offsetW);
        result = 991 * result + NumberUtils.floatToRawIntBits(scaleU);
        result = 991 * result + NumberUtils.floatToRawIntBits(scaleV);
        result = 991 * result + NumberUtils.floatToRawIntBits(scaleW);
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index < o.index ? -1 : 1;
        Texture3DAttribute other = (Texture3DAttribute) o;
        final int c = textureDescription.compareTo(other.textureDescription);
        if (c != 0)
            return c;
        if (!MathUtils.isEqual(scaleU, other.scaleU))
            return scaleU > other.scaleU ? 1 : -1;
        if (!MathUtils.isEqual(scaleV, other.scaleV))
            return scaleV > other.scaleV ? 1 : -1;
        if (!MathUtils.isEqual(scaleW, other.scaleW))
            return scaleW > other.scaleW ? 1 : -1;
        if (!MathUtils.isEqual(offsetU, other.offsetU))
            return offsetU > other.offsetU ? 1 : -1;
        if (!MathUtils.isEqual(offsetV, other.offsetV))
            return offsetV > other.offsetV ? 1 : -1;
        if (!MathUtils.isEqual(offsetW, other.offsetW))
            return offsetW > other.offsetW ? 1 : -1;
        return 0;
    }
}
