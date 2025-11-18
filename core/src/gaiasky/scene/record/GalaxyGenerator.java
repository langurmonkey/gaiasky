package gaiasky.scene.record;

import gaiasky.util.Pair;

/**
 * Generates galaxies as lists of {@link BillboardDataset} objects.
 */
public class GalaxyGenerator {

    public GalaxyGenerator() {
        super();
    }

    /**
     * Galaxy morphologies according to Edwin Hubble's classification.
     */
    public enum GalaxyMorphology {
        E0, E3, E5, E7, // Ellipticals (0-spherical, 7-highly elliptical)
        S0, // Lenticular (large bulb, surrounded with disk).
        Sa, Sb, Sc, // Spirals (a-low angle, c-high angle)
        SBa, SBb, SBc, // Barred spirals
        Im // Irregulars
    }

    public Pair<BillboardDataset[], BillboardDataset[]> generateGalaxy(){


        return null;
    }



}
