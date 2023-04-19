/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.i18n.I18n;

public class AttributeDistance extends AttributeAbstract implements IAttribute {
    @Override
    public double get(IParticleRecord bean) {
        return Math.sqrt(bean.x() * bean.x() + bean.y() * bean.y() + bean.z() * bean.z()) * Constants.U_TO_PC;
    }

    public String getUnit() {
        return I18n.msg("gui.unit.pc");
    }

    public String toString() {
        return I18n.msg("gui.attrib.distsun");
    }
}
