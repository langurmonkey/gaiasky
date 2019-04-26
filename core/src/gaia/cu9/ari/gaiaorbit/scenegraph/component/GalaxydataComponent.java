/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

public class GalaxydataComponent {
    /** Star positions/sizes **/
    public String starsource;

    /** Bulge **/
    public String bulgesource;

    /** Dust positions/sizes **/
    public String dustsource;

    /** HII positions/sizes **/
    public String hiisource;

    /** Nebulae - deprecated **/
    public String nebulasource;

    public GalaxydataComponent() {

    }

    public void setStarsource(String starsource) {
        this.starsource = starsource;
    }

    public void setDustsource(String dustsource) {
        this.dustsource = dustsource;
    }

    public void setNebulasource(String nebulasource) {
        this.nebulasource = nebulasource;
    }

    public void setHiisource(String hiisource) {
        this.hiisource = hiisource;
    }

    public void setBulgesource(String bulgesource) {
        this.bulgesource = bulgesource;
    }
}
