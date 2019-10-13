/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.octreegen;

import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;

import java.util.Comparator;

public class StarBrightnessComparator implements Comparator<StarBean> {
    @Override
    public int compare(StarBean o1, StarBean o2) {
        return Double.compare(o1.absmag(), o2.absmag());
    }

}
