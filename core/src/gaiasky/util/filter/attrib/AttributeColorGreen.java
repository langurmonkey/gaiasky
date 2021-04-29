package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.I18n;

public class AttributeColorGreen extends AttributeAbstract implements IAttribute<IParticleRecord> {

    @Override
    public double get(IParticleRecord bean) {
        return bean.rgb()[1];
    }

    @Override
    public String getUnit() {
        return I18n.txt("gui.attrib.color.unit");
    }

    public String toString(){
        return I18n.txt("gui.attrib.color.green");
    }
}
