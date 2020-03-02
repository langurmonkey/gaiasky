/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.util.ucd.UCD;

public class AttributeUCD implements IAttribute<ParticleBean> {
    public UCD ucd;

    public AttributeUCD(UCD ucd) {
        this.ucd = ucd;
    }

    @Override
    public double get(ParticleBean bean) {
        return bean.extra != null ? bean.extra.get(ucd) : Double.NaN;
    }

    @Override
    public String getUnit() {
        return ucd.unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AttributeUCD that = (AttributeUCD) o;
        return ucd.colname.equals(that.ucd.colname);
    }

    public String toString() {
        return ucd.colname;
    }
}
