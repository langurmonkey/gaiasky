package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.record.AtmosphereComponent;

public class Atmosphere implements Component {
    public AtmosphereComponent atmosphere;

    public void updateAtmosphere(AtmosphereComponent atmosphere) {
        if(this.atmosphere != null) {
            this.atmosphere.updateWith(atmosphere);
        } else {
            this.atmosphere = atmosphere;
        }
    }
}
