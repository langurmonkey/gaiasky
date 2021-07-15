/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.particle.IParticleRecord;

public class AttributeGalLatitude extends AttributeAbstract implements IAttribute {
    @Override
    public double get(IParticleRecord bean) {
        return bean.b();
    }
    public String getUnit(){
        return "deg";
    }
    public String toString(){
        return "Galactic latitude (b)";
    }
}
