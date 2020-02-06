/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.util.I18n;

public class AttributeRA extends AttributeAbstract implements IAttribute<ParticleBean> {
    @Override
    public double get(ParticleBean bean) {
        return bean.ra();
    }
    public String getUnit(){
        return "deg";
    }
    public String toString(){
        return "Right ascension (" + I18n.txt("gui.focusinfo.alpha") + ")";
    }
}
