package gaia.cu9.ari.gaiaorbit.render;

import gaia.cu9.ari.gaiaorbit.util.I18n;

import java.util.HashMap;
import java.util.Map;

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

    Equatorial("icon-elem-equatorial"),
    Ecliptic("icon-elem-ecliptic"),
    Galactic("icon-elem-galactic"),
    Labels("icon-elem-labels"),
    Titles("icon-elem-titles"),

    Orbits("icon-elem-orbits"),
    Locations("icon-elem-locations"),
    Countries("icon-elem-countries"),
    Constellations("icon-elem-constellations"),
    Boundaries("icon-elem-boundaries"),

    Ruler("icon-elem-ruler"),
    Effects("icon-elem-effects"),
    Atmospheres("icon-elem-atmospheres"),
    Clouds("icon-elem-clouds"),
    Others("icon-elem-others");

    private static Map<String, ComponentType> keysMap = new HashMap<String, ComponentType>();

    static {
        for (ComponentType ct : ComponentType.values()) {
            keysMap.put(ct.key, ct);
        }
    }

    public String key;
    public String style;

    private ComponentType(String icon) {
        this.key = "element." + name().toLowerCase();
        this.style = icon;
    }

    public String getName() {
        return I18n.bundle.get(key);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static ComponentType getFromKey(String key) {
        return keysMap.get(key);
    }
}
