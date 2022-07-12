package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;

public class Magnitude implements Component, ICopy {
    /** Absolute magnitude, m = -2.5 log10(flux), with the flux at 10 pc **/
    public float absmag = Float.NaN;
    /** Apparent magnitude, m = -2.5 log10(flux) **/
    public float appmag;

    public void setAbsmag(Double absmag) {
        this.absmag = absmag.floatValue();
    }
    public void setAppmag(Double appmag) {
        this.appmag = appmag.floatValue();
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.absmag = absmag;
        copy.appmag = appmag;
        return copy;
    }
}
