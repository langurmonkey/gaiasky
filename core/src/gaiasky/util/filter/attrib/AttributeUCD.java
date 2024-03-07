/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.ucd.UCD;

public class AttributeUCD implements IAttribute {
    public UCD ucd;

    public AttributeUCD(UCD ucd) {
        this.ucd = ucd;
    }

    @Override
    public Object get(IParticleRecord bean) {
        return bean.getExtra(ucd);
    }

    @Override
    public double getNumber(IParticleRecord bean) {
        var val = bean.getExtra(ucd);
        if (val instanceof Number n) {
            return n.doubleValue();
        } else if (val instanceof String s) {
            return s.hashCode();
        } else {
            return Double.NaN;
        }
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
        return ucd.colName.equals(that.ucd.colName);
    }

    public String toString() {
        return ucd.colName;
    }

    @Override
    public boolean isNumberAttribute() {
        return false;
    }
}
