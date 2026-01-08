/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

public class FlagAttribute extends Attribute {
    public static final String ThinSurfaceAlias = "thinSurface";
    public static final int ThinSurface = register(ThinSurfaceAlias);
    public boolean value;

    public FlagAttribute(int index) {
        super(index);
    }

    public FlagAttribute(FlagAttribute other) {
        super(ThinSurface);
        this.value = other.value;
    }

    public FlagAttribute(int index, boolean value) {
        super(index);
        this.value = value;
    }


    @Override
    public Attribute copy() {
        return new FlagAttribute(index, value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 983 * result + (value ? 1 : 0);
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        return Boolean.compare(value, ((FlagAttribute) o).value);
    }
}
