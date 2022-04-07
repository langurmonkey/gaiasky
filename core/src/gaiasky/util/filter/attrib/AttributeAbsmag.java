/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.i18n.I18n;

public class AttributeAbsmag extends AttributeAbstract implements IAttribute {
    @Override
    public double get(IParticleRecord pb) {
        return pb.absmag();
    }

    public String getUnit(){
        return "mag";
    }
    public String toString(){
        return I18n.msg("gui.focusinfo.absmag");
    }
}
