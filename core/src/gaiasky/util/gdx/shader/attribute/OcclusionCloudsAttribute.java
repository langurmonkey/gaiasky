/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

public class OcclusionCloudsAttribute extends Attribute {

    public final static String Alias = "occlusionClouds";
    public final static int Type = register(Alias);

    public boolean occlusionClouds;

    public OcclusionCloudsAttribute(boolean state) {
        super(Type);
        this.occlusionClouds = state;
    }

    public OcclusionCloudsAttribute() {
        this(true);
    }

    @Override
    public Attribute copy() {
        return new OcclusionCloudsAttribute(this.occlusionClouds);
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        OcclusionCloudsAttribute other = (OcclusionCloudsAttribute) o;
        if (other.occlusionClouds != occlusionClouds) {
            return -1;
        }
        return 0;
    }
}
