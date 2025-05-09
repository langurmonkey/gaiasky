/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.util.Bits;
import gaiasky.util.i18n.I18n;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

public class ComponentTypes extends Bits {
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
     * Checks if all the bits in this bit set are also set in the other.
     *
     * @param other The bit set to check against.
     *
     * @return True if all the bits set to true in this bit set are also true in
     * the other. Returns false otherwise.
     */
    public boolean allSetLike(ComponentTypes other) {
        return other.containsAll(this);
    }

    /**
     * Checks whether the given {@link ComponentType} is enabled in this
     * {@link ComponentTypes} instance
     *
     * @param ct The component type to check
     *
     * @return True if the component type is enabled
     */
    public boolean isEnabled(ComponentType ct) {
        return this.get(ct.ordinal());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isEmpty())
            return "Empty";
        ComponentType[] values = ComponentType.values();
        for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
            // operate on index i here
            sb.append(values[i]).append(" ");
        }
        sb.replace(sb.length() - 1, sb.length(), "");
        return sb.toString();
    }

    public enum ComponentType {
        Stars("icon-elem-stars"),
        Planets("icon-elem-planets"),
        Moons("icon-elem-moons"),
        Satellites("icon-elem-satellites"),

        Asteroids("icon-elem-asteroids"),
        Clusters("icon-elem-clusters"),
        MilkyWay("icon-elem-milkyway"),
        Galaxies("icon-elem-galaxies"),

        Nebulae("icon-elem-nebulae"),
        Meshes("icon-elem-meshes"),
        Systems("icon-elem-systems"),
        Labels("icon-elem-labels"),

        Orbits("icon-elem-orbits"),
        Locations("icon-elem-locations"),
        Countries("icon-elem-countries"),
        Ruler("icon-elem-ruler"),

        Equatorial("icon-elem-equatorial"),
        Ecliptic("icon-elem-ecliptic"),
        Galactic("icon-elem-galactic"),
        RecursiveGrid("icon-elem-recgrid"),

        Constellations("icon-elem-constellations"),
        Boundaries("icon-elem-boundaries"),
        Atmospheres("icon-elem-atmospheres"),
        Clouds("icon-elem-clouds"),

        Effects("icon-elem-effects"),
        Axes("icon-elem-axes"),
        VelocityVectors("icon-elem-arrows"),
        Keyframes("icon-elem-keyframes"),

        Others("icon-elem-others"),

        // ALWAYS LAST
        Invisible(null);

        private static final Map<String, ComponentType> keysMap = new HashMap<>();

        static {
            for (ComponentType ct : ComponentType.values()) {
                keysMap.put(ct.key, ct);
            }
        }

        public final String key;
        public final String style;

        ComponentType(String icon) {
            this.key = "element." + name().toLowerCase();
            this.style = icon;
        }

        public static ComponentType getFromKey(String key) {
            return keysMap.get(key);
        }

        public String getName() {
            try {
                return I18n.msg(key);
            } catch (MissingResourceException e) {
                return null;
            }
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

}