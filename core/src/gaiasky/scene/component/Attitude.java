package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.data.attitude.IAttitudeServer;
import gaiasky.util.gaia.IAttitude;
import gaiasky.util.math.Vector3d;

/**
 * This component provides attitude to entities.
 */
public class Attitude implements Component {

    // Attitude provider
    public String provider;
    public String attitudeLocation;
    public IAttitudeServer attitudeServer;
    public IAttitude attitude;

    public Vector3d nonRotatedPos;

    public void setAttitudeProvider(String provider) {
        this.provider = provider;
    }

    public void setProvider(String provider) {
        setAttitudeProvider(provider);
    }

}
