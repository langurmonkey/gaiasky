/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.shader.attribute.Attribute;
import gaiasky.util.gdx.shader.attribute.Attributes;

public class Material extends Attributes {
    private static int counter = 0;
    public String id;

    /** Create an empty material */
    public Material() {
        this("mtl" + (++counter));
    }

    /** Create an empty material */
    public Material(final String id) {
        this.id = id;
    }

    /** Create a material with the specified attributes */
    public Material(final Attribute... attributes) {
        this();
        set(attributes);
    }

    /** Create a material with the specified attributes */
    public Material(final String id, final Attribute... attributes) {
        this(id);
        set(attributes);
    }

    /** Create a material with the specified attributes */
    public Material(final Array<Attribute> attributes) {
        this();
        set(attributes);
    }

    /** Create a material with the specified attributes */
    public Material(final String id, final Array<Attribute> attributes) {
        this(id);
        set(attributes);
    }

    /** Create a material which is an exact copy of the specified material */
    public Material(final Material copyFrom) {
        this(copyFrom.id, copyFrom);
    }

    /** Create a material which is an exact copy of the specified material */
    public Material(final String id, final Material copyFrom) {
        this(id);
        for (Attribute attr : copyFrom)
            set(attr.copy());
    }

    /** Create a copy of this material */
    public Material copy() {
        return new Material(this);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 3 * id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof Material) && ((other == this) || ((((Material) other).id.equals(id)) && super.equals(other)));
    }
}
