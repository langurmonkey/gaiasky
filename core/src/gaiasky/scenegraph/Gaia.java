/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

/**
 * The Gaia satellite is just a heliotropic satellite. It is now phased out.
 * @deprecated Use {@link HeliotropicSatellite} instead.
 * TODO Remove this when possible.
 */
public class Gaia extends HeliotropicSatellite {

    public Gaia() {
        super();
    }

    public void initialize() {
        super.initialize();
        provider = "gaiasky.util.gaia.GaiaAttitudeServer";
        attitudeLocation = "data/attitudexml";
    }

}
