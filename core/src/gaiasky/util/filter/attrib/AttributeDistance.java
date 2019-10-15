/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.util.Constants;

public class AttributeDistance implements IAttribute<ParticleBean> {
    @Override
    public double get(ParticleBean bean) {
        return Math.sqrt(bean.x() * bean.x() + bean.y() * bean.y() + bean.z() * bean.z()) * Constants.U_TO_PC;
    }
    public String getUnit(){
        return "pc";
    }
}
