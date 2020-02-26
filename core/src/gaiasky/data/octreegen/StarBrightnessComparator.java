/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;

import java.util.Comparator;

public class StarBrightnessComparator implements Comparator<ParticleBean> {
    private ParticleBean o1;
    private ParticleBean o2;

    @Override
    public int compare(ParticleBean o1, ParticleBean o2) {
        this.o1 = o1;
        this.o2 = o2;
        return Double.compare(((StarBean) o1).absmag(), ((StarBean) o2).absmag());
    }

}
