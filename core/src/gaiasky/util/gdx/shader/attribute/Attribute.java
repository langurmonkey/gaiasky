/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.Bits;

public abstract class Attribute implements Comparable<Attribute> {
    /** The registered type aliases */
    private final static Array<String> types = new Array<>();
    /** The type of this attribute */
    //public final long type;
    public final Bits type;
    public final int index;
    private final int typeBit;

    protected Attribute(final int index) {
        this.type = Bits.empty();
        this.index = index;
        this.type.set(index);
        this.typeBit = index;
    }

    protected Attribute(long oldType) {
        this(getIndex(oldType));
    }

    protected static int getIndex(long oldType) {
        int idx = -1;
        while (oldType != 0 && ++idx < 63 && (((oldType >> idx) & 1) == 0))
            ;
        return idx;
    }

    /** @return The ID of the specified attribute type, or zero if not available */
    public static Bits getAttributeType(final String alias) {
        for (int i = 0; i < types.size; i++) {
            if (types.get(i).compareTo(alias) == 0) {
                Bits b = Bits.empty();
                b.set(i);
                return b;
            }
        }
        return Bits.empty();
    }

    /** @return The alias of the specified attribute type, or null if not available. */
    public static String getAttributeAlias(final Bits type) {
        int idx = type.nextSetBit(0);
        return (idx >= 0 && idx < types.size) ? types.get(idx) : null;
    }

    /** @return The index of the specified attribute type, or -1 if not available. */
    public static int getAttributeIndex(final Bits type) {
        return type.nextSetBit(0);
    }

    /** @return The index of the specified attribute alias, or -1 if not available. */
    public static int getAttributeIndex(final String alias) {
        for (int i = 0; i < types.size; i++) {
            if (types.get(i).compareTo(alias) == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Call this method to register a custom attribute type, see the wiki for an example. If the alias already exists, then that
     * ID will be reused. The alias should be unambiguous and will by default be returned by the call to {@link #toString()}.
     *
     * @param alias The alias of the type to register, must be different for each direct type, will be used for debugging.
     *
     * @return the index of the newly registered type, or the index of the existing type if the alias was already registered.
     */
    protected static int register(final String alias) {
        int result = getAttributeIndex(alias);
        if (result >= 0)
            return result;
        types.add(alias);
        return types.size - 1;
    }

    public static int getNumAttributes() {
        return types.size;
    }

    public static Array<String> getTypes() {
        return types;
    }

    public boolean has(int index) {
        return type.is(index);
    }

    /** @return An exact copy of this attribute */
    public abstract Attribute copy();

    protected boolean equals(Attribute other) {
        return other.hashCode() == hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Attribute other))
            return false;
        if (this.type.equals(other.type))
            return false;
        return equals(other);
    }

    @Override
    public String toString() {
        return getAttributeAlias(type);
    }

    @Override
    public int hashCode() {
        return 7489 * typeBit;
    }
}
