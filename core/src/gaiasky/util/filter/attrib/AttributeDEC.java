/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.i18n.I18n;

public class AttributeDEC extends AttributeAbstract implements IAttribute {
    @Override
    public Object get(IParticleRecord bean) {
        return bean.dec();
    }

    @Override
    public double getNumber(IParticleRecord bean) {
        return bean.dec();
    }

    public String getUnit() {
        return I18n.msg("gui.unit.deg");
    }

    public String toString() {
        return I18n.msg("gui.attrib.dec") + " (" + I18n.msg("gui.focusinfo.delta") + ")";
    }

    @Override
    public boolean isNumberAttribute() {
        return true;
    }
}
