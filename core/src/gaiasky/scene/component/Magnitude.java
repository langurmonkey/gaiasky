package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;

public class Magnitude implements Component, ICopy {
    /** Absolute magnitude, m = -2.5 log10(flux), with the flux at 10 pc **/
    public float absMag = Float.NaN;
    /** Apparent magnitude, m = -2.5 log10(flux) **/
    public float appMag = Float.NaN;

    public void setAbsMag(Double absMag) {
        this.absMag = absMag.floatValue();
    }

    public void setAbsmag(Double absMag) {
        setAbsMag(absMag);
    }

    public void setAppMag(Double appMag) {
        this.appMag = appMag.floatValue();
    }

    public void setAppmag(Double appMag) {
        setAppMag(appMag);
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.absMag = absMag;
        copy.appMag = appMag;
        return copy;
    }
}
