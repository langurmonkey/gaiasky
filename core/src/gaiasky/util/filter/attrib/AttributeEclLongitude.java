/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.ParticleGroup.ParticleRecord;

public class AttributeEclLongitude extends AttributeAbstract implements IAttribute<ParticleRecord> {
    @Override
    public double get(ParticleRecord bean) {
        return bean.lambda();
    }
    public String getUnit(){
        return "deg";
    }
    public String toString(){
        return "Ecliptic longitude (λ)";
    }
}
