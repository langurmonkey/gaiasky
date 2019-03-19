/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 only, as published by
 * the Free Software Foundation. Oracle designates this particular file as
 * subject to the "Classpath" exception as provided by Oracle in the LICENSE
 * file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License version 2 for more
 * details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or
 * visit www.oracle.com if you need additional information or have any
 * questions.
 */

package gaia.cu9.ari.gaiaorbit.util;

import gaia.cu9.ari.gaiaorbit.render.ComponentType;

import java.util.BitSet;

/**
 * BitSet with some added functionality
 *
 * @author Toni Sagrista
 */
public class ComponentTypes extends BitSet {
    private static final long serialVersionUID = 1L;
    public static final int CT_SIZE = 32;

    public ComponentTypes() {
        super(CT_SIZE);
    }

    public ComponentTypes(int ordinal) {
        super(CT_SIZE);
        set(ordinal);
    }

    public ComponentTypes(ComponentType... cts) {
        super();
        for (ComponentType ct : cts)
            set(ct.ordinal());
    }

    /**
     * Returns the index of the rightmost bit set to 1. If no bits are set to 1,
     * returns -1
     * 
     * @return The first ordinal
     */
    public int getFirstOrdinal() {
        return nextSetBit(0);
    }

    /**
     * Checks if all the t bits in this bit set are also set in other.
     * 
     * @param other
     *            The bit set to check against
     * @return True if all the bits set to true in this bit set are also true in
     *         other. Returns false otherwise
     */
    public boolean allSetLike(ComponentTypes other) {
        long thisval = this.toLongArray()[0];
        return (thisval & other.toLongArray()[0]) == thisval;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ComponentType[] values = ComponentType.values();
        for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
            // operate on index i here
            sb.append(values[i]).append(" ");
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        return sb.toString();
    }

}