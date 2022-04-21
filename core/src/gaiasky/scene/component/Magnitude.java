package gaiasky.scene.component;

import com.artemis.Component;

public class Magnitude extends Component {
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
}
