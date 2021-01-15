/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.particle.ParticleRecord;
import gaiasky.util.Constants;

public class AttributeDistance extends AttributeAbstract implements IAttribute<ParticleRecord> {
    @Override
    public double get(ParticleRecord bean) {
        return Math.sqrt(bean.x() * bean.x() + bean.y() * bean.y() + bean.z() * bean.z()) * Constants.U_TO_PC;
    }
    public String getUnit(){
        return "pc";
    }
    public String toString(){
        return "Distance from Sun";
    }
}
