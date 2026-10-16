/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.shader.attribute;

import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;
import gaiasky.render.gdx.graphics.VolumeType;

public class Texture3DAttribute extends Attribute {
    public final static String Volume0Alias = "volume0";
    public final static int Volume0 = register(Volume0Alias);
    public final static String Volume1Alias = "volume1";
    public final static int Volume1 = register(Volume1Alias);
    public final static String Volume2Alias = "volume2";
    public final static int Volume2 = register(Volume2Alias);
    public final static String Volume3Alias = "volume3";
    public final static int Volume3 = register(Volume3Alias);

    public final TextureDescriptor<Texture3D> textureDescription;
    public float offsetU;
    public float offsetV;
    public float offsetW;
    public float scaleU = 1;
    public float scaleV = 1;
    public float scaleW = 1;
    /**
     * Storage format of the texture at this slot, propagated from
     * {@link gaiasky.render.gdx.graphics.VolumeTexture#type()}. It tells the shader what the
     * sampled texel means: a density-only (R8) volume reads its density from {@code .r}, while a
     * color+density (RGBA) volume reads colour from {@code .rgb} and density from {@code .a}.
     * Defaults to {@link VolumeType#DENSITY}, so a slot populated without an explicit type keeps
     * the legacy density-only behaviour.
     */
    public VolumeType volumeType = VolumeType.DENSITY;
    /**
     * Minimum density for this slot to be treated as an occluder in the depth-only pre-pass
     * ({@link gaiasky.render.RenderGroup#VOLUME_DEPTH}). A sample of this volume contributes to
     * the written depth only where its density exceeds this value.
     * <p>
     * Zero (the default) or negative means the slot never occludes: it still renders, but
     * nothing can be hidden behind it. That is the right default for a volume that only emits
     * (gas): opacity is the job of the absorbing volume, and a faint emissive shell must not
     * punch an opaque hole in the star field.
     * <p>
     * The whole depth pass is skipped for a volume whose scene sets
     * {@code "writeDepth": false}.
     */
    public float depthThreshold = 0f;

    /**
     * The brick occupancy grid of the volume at this slot, if it has one: an R8 3D texture covering
     * the same box, each texel the maximum density of the corresponding brick of voxels, built by
     * {@link gaiasky.render.gdx.loader.VolumeTextureLoader#brickOccupancy}. A zero texel means
     * every sample in that brick would read a zero, so the shader's march jumps the whole brick.
     * <p>
     * Null means the slot has no occupancy grid, and the shader is bound a 1x1x1 volume of ones
     * instead - see {@link gaiasky.render.gdx.graphics.VolumeTexture#occupancyDescriptor}. This is
     * deliberately not part of {@link #hashCode()} or {@link #compareTo}: it is derived from
     * {@link #textureDescription}, so two attributes with the same texture always have the same
     * grid, and comparing it would only ever repeat the texture comparison.
     */
    public TextureDescriptor<Texture3D> occupancyDescription;

    public Texture3DAttribute(int index) {
        super(index);
        textureDescription = new TextureDescriptor<>();
    }

    public <T extends Texture3D> Texture3DAttribute(int index, TextureDescriptor<T> textureDescription) {
        this(index);
        this.textureDescription.set(textureDescription);
    }

    public <T extends Texture3D> Texture3DAttribute(int index, TextureDescriptor<T> textureDescription, float offsetU,
                                                    float offsetV, float offsetW, float scaleU, float scaleV, float scaleW) {
        this(index, textureDescription, offsetU, offsetV, offsetW, scaleU, scaleV, scaleW, VolumeType.DENSITY);
    }

    public <T extends Texture3D> Texture3DAttribute(int index, TextureDescriptor<T> textureDescription, float offsetU,
                                                    float offsetV, float offsetW, float scaleU, float scaleV, float scaleW,
                                                    VolumeType volumeType) {
        this(index, textureDescription);
        this.offsetU = offsetU;
        this.offsetV = offsetV;
        this.offsetW = offsetW;
        this.scaleU = scaleU;
        this.scaleV = scaleV;
        this.scaleW = scaleW;
        this.volumeType = volumeType == null ? VolumeType.DENSITY : volumeType;
    }


    public Texture3DAttribute(int index, Texture3D texture) {
        this(index, texture, VolumeType.DENSITY);
    }

    public Texture3DAttribute(int index, Texture3D texture, VolumeType volumeType) {
        this(index);
        textureDescription.texture = texture;
        this.volumeType = volumeType == null ? VolumeType.DENSITY : volumeType;
    }

    public Texture3DAttribute(Texture3DAttribute copyFrom) {
        this(copyFrom.index,
             copyFrom.textureDescription,
             copyFrom.offsetU,
             copyFrom.offsetV,
             copyFrom.offsetW,
             copyFrom.scaleU,
             copyFrom.scaleV,
             copyFrom.scaleW,
             copyFrom.volumeType);
        this.depthThreshold = copyFrom.depthThreshold;
        this.occupancyDescription = copyFrom.occupancyDescription;
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
        result = 991 * result + volumeType.ordinal();
        result = 991 * result + NumberUtils.floatToRawIntBits(depthThreshold);
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index < o.index ? -1 : 1;
        Texture3DAttribute other = (Texture3DAttribute) o;
        int c = textureDescription.compareTo(other.textureDescription);
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
        if (volumeType != other.volumeType)
            return volumeType.ordinal() < other.volumeType.ordinal() ? -1 : 1;
        if (!MathUtils.isEqual(depthThreshold, other.depthThreshold))
            return depthThreshold > other.depthThreshold ? 1 : -1;
        return 0;
    }
}
