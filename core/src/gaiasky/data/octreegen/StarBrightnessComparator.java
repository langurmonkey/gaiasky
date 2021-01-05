/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import gaiasky.scenegraph.ParticleGroup.ParticleRecord;

import java.util.Comparator;

public class StarBrightnessComparator implements Comparator<ParticleRecord> {

    @Override
    public int compare(ParticleRecord o1, ParticleRecord o2) {
        return Double.compare(o1.absmag(), o2.absmag());
    }

}
