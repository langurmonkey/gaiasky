/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.i18n.I18n;

public class AttributeAppmag extends AttributeAbstract implements IAttribute {
    @Override
    public double get(IParticleRecord bean) {
        return bean.appmag();
    }

    public String getUnit() {
        return I18n.msg("gui.unit.mag");
    }

    public String toString() {
        return I18n.msg("gui.attrib.appmag");
    }
}
