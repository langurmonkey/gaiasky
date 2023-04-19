/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

public class BlendingAttribute extends Attribute {
    public final static String Alias = "blended";
    public final static int Type = register(Alias);
    /**
     * Whether this material should be considered blended (default: true). This is used for sorting (back to front instead of front
     * to back).
     */
    public boolean blended;
    /** Specifies how the (incoming) red, green, blue, and alpha source blending factors are computed (default: GL_SRC_ALPHA) */
    public int sourceFunction;
    /**
     * Specifies how the (existing) red, green, blue, and alpha destination blending factors are computed (default:
     * GL_ONE_MINUS_SRC_ALPHA)
     */
    public int destFunction;
    /** The opacity used as source alpha value, ranging from 0 (fully transparent) to 1 (fully opaque), (default: 1). */
    public float opacity;
    public BlendingAttribute() {
        this((BlendingAttribute) null);
    }

    public BlendingAttribute(com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute other) {
        super(Type);
        this.blended = other.blended;
        this.sourceFunction = other.sourceFunction;
        this.destFunction = other.destFunction;
        this.opacity = other.opacity;
    }

    public BlendingAttribute(final boolean blended, final int sourceFunc, final int destFunc, final float opacity) {
        super(Type);
        this.blended = blended;
        this.sourceFunction = sourceFunc;
        this.destFunction = destFunc;
        this.opacity = opacity;
    }

    public BlendingAttribute(final int sourceFunc, final int destFunc, final float opacity) {
        this(true, sourceFunc, destFunc, opacity);
    }

    public BlendingAttribute(final int sourceFunc, final int destFunc) {
        this(sourceFunc, destFunc, 1.f);
    }

    public BlendingAttribute(final boolean blended, final float opacity) {
        this(blended, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, opacity);
    }

    public BlendingAttribute(final float opacity) {
        this(true, opacity);
    }

    public BlendingAttribute(final BlendingAttribute copyFrom) {
        this(copyFrom == null || copyFrom.blended, copyFrom == null ? GL20.GL_SRC_ALPHA : copyFrom.sourceFunction,
                copyFrom == null ? GL20.GL_ONE_MINUS_SRC_ALPHA : copyFrom.destFunction, copyFrom == null ? 1.f : copyFrom.opacity);
    }

    public final static boolean is(final int index) {
        return index == Type;
    }

    @Override
    public BlendingAttribute copy() {
        return new BlendingAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 947 * result + (blended ? 1 : 0);
        result = 947 * result + sourceFunction;
        result = 947 * result + destFunction;
        result = 947 * result + NumberUtils.floatToRawIntBits(opacity);
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        BlendingAttribute other = (BlendingAttribute) o;
        if (blended != other.blended)
            return blended ? 1 : -1;
        if (sourceFunction != other.sourceFunction)
            return sourceFunction - other.sourceFunction;
        if (destFunction != other.destFunction)
            return destFunction - other.destFunction;
        return (MathUtils.isEqual(opacity, other.opacity)) ? 0 : (opacity < other.opacity ? 1 : -1);
    }
}
