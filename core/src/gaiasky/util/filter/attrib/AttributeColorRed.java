package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.i18n.I18n;

public class AttributeColorRed extends AttributeAbstract implements IAttribute {

    @Override
    public double get(IParticleRecord bean) {
        return bean.rgb()[0];
    }

    @Override
    public String getUnit() {
        return I18n.msg("gui.attrib.color.unit");
    }

    public String toString(){
        return I18n.msg("gui.attrib.color.red");
    }
}
