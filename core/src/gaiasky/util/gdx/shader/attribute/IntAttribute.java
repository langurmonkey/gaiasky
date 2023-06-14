/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

public class IntAttribute extends Attribute {
    public static final String CullFaceAlias = "cullface";
    public static final int CullFace = register(CullFaceAlias);
    public static final String EclipseOutlinesAlias = "eclipseOutlines";
    public static final int EclipseOutlines = register(EclipseOutlinesAlias);
    public int value;

    public IntAttribute(int index) {
        super(index);
    }

    public IntAttribute(com.badlogic.gdx.graphics.g3d.attributes.IntAttribute other) {
        super(CullFace);
        this.value = other.value;
    }

    public IntAttribute(int index, int value) {
        super(index);
        this.value = value;
    }

    /**
     * create a cull face attribute to be used in a material
     *
     * @param value cull face value, possible values are GL_FRONT_AND_BACK, GL_BACK, GL_FRONT, or -1 to inherit default
     *
     * @return an attribute
     */
    public static IntAttribute createCullFace(int value) {
        return new IntAttribute(CullFace, value);
    }

    @Override
    public Attribute copy() {
        return new IntAttribute(index, value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 983 * result + value;
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        return value - ((IntAttribute) o).value;
    }
}
