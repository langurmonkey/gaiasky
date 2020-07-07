/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

public class GalaxydataComponent {
    /** Star positions/sizes **/
    public String starsource;
    public String starsourceUnpack;

    /** Bulge **/
    public String bulgesource;
    public String bulgesourceUnpack;

    /** Dust positions/sizes **/
    public String dustsource;
    public String dustsourceUnpack;

    /** HII positions/sizes **/
    public String hiisource;
    public String hiisourceUnpack;

    /** Gas positions/sizes **/
    public String gassource;
    public String gassourceUnpack;

    /** Nebulae - deprecated **/
    public String nebulasource;
    public String nebulasourceUnpack;

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

    public void setGassource(String gassource) {
        this.gassource = gassource;
    }
}
