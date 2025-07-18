/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.Bits;
import gaiasky.util.Logger;

import java.util.Comparator;
import java.util.Iterator;

public class Attributes implements Iterable<Attribute>, Comparator<Attribute>, Comparable<Attributes> {
    protected final Array<Attribute> attributes;
    protected Bits mask;
    protected boolean sorted = true;

    public Attributes() {
        mask = Bits.empty();
        attributes = new Array<>(Bits.DEFAULT_LENGTH);
    }

    /** Sort the attributes by their ID */
    public final void sort() {
        if (!sorted) {
            attributes.sort(this);
            sorted = true;
        }
    }

    /** @return Bitwise mask of the ID's of all the containing attributes */
    public final Bits getMask() {
        return mask;
    }

    /**
     * Example usage: ((BlendingAttribute)material.get(BlendingAttribute.ID)).sourceFunction;
     *
     * @return The attribute (which can safely be cast) if any, otherwise null
     */
    public final Attribute get(final int index) {
        if (has(index))
            for (int i = 0; i < attributes.size; i++)
                if (attributes.get(i).index == index)
                    return attributes.get(i);
        return null;
    }

    /**
     * Example usage: ((BlendingAttribute)material.get(BlendingAttribute.ID)).sourceFunction;
     *
     * @return The attribute if any, otherwise null
     */
    @SuppressWarnings("unchecked")
    public final <T extends Attribute> T get(Class<T> ignored, final int index) {
        return (T) get(index);
    }

    /** Removes all attributes */
    public void clear() {
        mask.clear();
        attributes.clear();
    }

    /** @return The amount of attributes this material contains. */
    public int size() {
        return attributes.size;
    }

    private void enable(final Bits mask) {
        var ignored = this.mask.or(mask);
    }

    private void disable(final Bits mask) {
        var ignored = this.mask.andNot(mask);
    }

    /** Add an attribute to this material. If the material already contains an attribute of the same type it is overwritten. */
    public final void set(final Attribute attribute) {
        final int idx = indexOf(attribute.index);
        if (idx < 0) {
            this.mask.set(attribute.index);
            attributes.add(attribute);
            sorted = false;
        } else {
            attributes.set(idx, attribute);
        }
        sort();
    }

    /** Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten. */
    public final void set(final Attribute attribute1, final Attribute attribute2) {
        set(attribute1);
        set(attribute2);
    }

    /** Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten. */
    public final void set(final Attribute attribute1, final Attribute attribute2, final Attribute attribute3) {
        set(attribute1);
        set(attribute2);
        set(attribute3);
    }

    /** Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten. */
    public final void set(final Attribute attribute1, final Attribute attribute2, final Attribute attribute3,
            final Attribute attribute4) {
        set(attribute1);
        set(attribute2);
        set(attribute3);
        set(attribute4);
    }

    /**
     * Add an array of attributes to this material. If the material already contains an attribute of the same type it is
     * overwritten.
     */
    public final void set(final Attribute... attributes) {
        for (final Attribute attr : attributes)
            set(attr);
    }

    /**
     * Add an array of attributes to this material. If the material already contains an attribute of the same type it is
     * overwritten.
     */
    public final void set(final Iterable<Attribute> attributes) {
        for (final Attribute attr : attributes)
            set(attr);
    }

    /**
     * Removes the attribute from the material, i.e.: material.remove(BlendingAttribute.ID); Can also be used to remove multiple
     * attributes also, i.e. remove(AttributeA.ID | AttributeB.ID);
     */
    public final void remove(final Bits mask) {
        for (int i = attributes.size - 1; i >= 0; i--) {
            final Bits type = attributes.get(i).type;
            if ((mask.and(type).equals(type))) {
                attributes.removeIndex(i);
                disable(type);
                sorted = false;
            }
        }
        sort();
    }

    public final void remove(final int index) {
        if (mask.has(index)) {
            Attribute candidate = null;
            for (Attribute a : attributes) {
                if (a.index == index) {
                    candidate = a;
                    break;
                }
            }
            if (candidate != null) {
                attributes.removeValue(candidate, true);
            } else {
                Logger.getLogger(Attributes.class).warn("Attribute not found: " + index);
            }
            Bits type = Bits.empty();
            type.set(index);
            disable(type);
            sorted = false;
        }
        sort();
    }

    /**
     * @return True if this collection has the specified attribute, i.e. attributes.has(ColorAttribute.Diffuse); Or when multiple
     * attribute types are specified, true if this collection has all specified attributes, i.e. attributes.has(out,
     * ColorAttribute.Diffuse | ColorAttribute.Specular | TextureAttribute.Diffuse);
     */
    public final boolean has(final Bits mask) {
        return !mask.isEmpty() && (this.mask.copy().and(mask).equals(mask));
    }

    public final boolean has(final int index) {
        return mask.get(index);
    }

    /** @return the index of the attribute with the specified type or negative if not available. */
    protected int indexOf(final int index) {
        if (has(index))
            for (int i = 0; i < attributes.size; i++)
                if (attributes.get(i).index == index)
                    return i;
        return -1;
    }

    /**
     * Check if this collection has the same attributes as the other collection. If compareValues is true, it also compares the
     * values of each attribute.
     *
     * @param compareValues True to compare attribute values, false to only compare attribute types
     *
     * @return True if this collection contains the same attributes (and optionally attribute values) as the other.
     */
    public final boolean same(final Attributes other, boolean compareValues) {
        if (other == this)
            return true;
        if ((other == null) || (!mask.equals(other.mask)))
            return false;
        if (!compareValues)
            return true;
        sort();
        other.sort();
        for (int i = 0; i < attributes.size; i++)
            if (!attributes.get(i).equals(other.attributes.get(i)))
                return false;
        return true;
    }

    /**
     * See {@link #same(Attributes, boolean)}
     *
     * @return True if this collection contains the same attributes (but not values) as the other.
     */
    public final boolean same(final Attributes other) {
        return same(other, false);
    }

    /** Used for sorting attributes by type (not by value) */
    @Override
    public final int compare(final Attribute arg0, final Attribute arg1) {
        return arg0.compareTo(arg1);
    }

    /** Used for iterating through the attributes */
    @Override
    public final Iterator<Attribute> iterator() {
        return attributes.iterator();
    }

    /**
     * @return A hash code based on only the attribute values, which might be different compared to {@link #hashCode()} because the latter
     * might include other properties as well, i.e. the material id.
     */
    public int attributesHash() {
        sort();
        final int n = attributes.size;
        long result = 71 + mask.hashCode();
        int m = 1;
        for (int i = 0; i < n; i++)
            result += (long) mask.hashCode() * attributes.get(i).hashCode() * (m = (m * 7) & 0xFFFF);
        return (int) (result ^ (result >> 32));
    }

    @Override
    public int hashCode() {
        return attributesHash();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Attributes))
            return false;
        if (other == this)
            return true;
        return same((Attributes) other, true);
    }

    @Override
    public int compareTo(Attributes other) {
        if (other == this)
            return 0;
        return mask.compareTo(other.mask);
    }
}
