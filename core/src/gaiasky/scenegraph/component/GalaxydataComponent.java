/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import gaiasky.data.group.PointDataProvider;
import gaiasky.render.BlendMode;
import gaiasky.scenegraph.particle.BillboardDataset;
import gaiasky.scenegraph.particle.BillboardDataset.ParticleType;
import gaiasky.util.Logger;

/**
 * Contains galaxy data pointers and utilities to convert to the new system.
 * @deprecated This will be phased out eventually.
 * TODO remove this when possible.
 */
@Deprecated
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

    public BillboardDataset[] transformToDatasets() {
        try {
            BillboardDataset[] datasets = new BillboardDataset[5];
            PointDataProvider provider = new PointDataProvider();
            if (starsource != null) {
                datasets[0] = generateDataset(provider,
                        starsource,
                        ParticleType.STAR,
                        2.0,
                        8.0,
                        new int[] { 0, 1 }, new double[] { 0.1, 0.1, 0.1, 0.1 });
            }
            if (bulgesource != null) {
                datasets[1] = generateDataset(provider,
                        bulgesource,
                        ParticleType.BULGE,
                        30.0,
                        10.0,
                        new int[] { 0, 1 }, new double[] { 0.1, 0.1, 0.1, 0.15 });
            }
            if (dustsource != null) {
                datasets[2] = generateDataset(provider,
                        dustsource,
                        ParticleType.DUST,
                        300.0,
                        2.0,
                        new int[] { 3, 5, 6 }, new double[] { 13.0, 13.0, 20.0, 30.0 });
                datasets[2].depthMask = true;
                datasets[2].blending = BlendMode.ALPHA;
            }
            if (hiisource != null) {
                datasets[3] = generateDataset(provider,
                        hiisource,
                        ParticleType.HII,
                        100.0,
                        0.5,
                        new int[] { 2, 3, 4, 5, 6, 7 }, new double[] { 1.5, 1.5, 2.0, 8.0 });
            }
            if (gassource != null) {
                datasets[4] = generateDataset(provider,
                        gassource,
                        ParticleType.GAS,
                        40.0,
                        0.5,
                        new int[] { 0, 1, 2, 3, 4, 5, 6, 7 }, new double[] { 2.0, 2.3, 8.0, 10.0 });
            }
            return datasets;
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
        return null;
    }

    private BillboardDataset generateDataset(PointDataProvider provider,
            String file,
            ParticleType type,
            double size,
            double intensity,
            int[] layers,
            double[] maxSizes) {
        BillboardDataset bd = new BillboardDataset();
        bd.file = file;
        bd.type = type;
        bd.size = (float) size;
        bd.intensity = (float) intensity;
        bd.setLayers(layers);
        bd.setMaxsizes(maxSizes);
        bd.initialize(provider, false);
        return bd;
    }

}
