/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.ucd.UCD;

public class AttributeUCD implements IAttribute<ParticleBean> {
    public UCD ucd;
    public AttributeUCD(UCD ucd){
        this.ucd = ucd;
    }

    @Override
    public double get(ParticleBean bean) {
        if(bean instanceof StarBean){
            StarBean sb = (StarBean) bean;
            return sb.extra != null ? sb.extra.get(ucd) : Double.NaN;
        }
        return Double.NaN;
    }

    @Override
    public String getUnit() {
        return ucd.unit;
    }

    public String toString(){
        return ucd.colname;
    }
}
