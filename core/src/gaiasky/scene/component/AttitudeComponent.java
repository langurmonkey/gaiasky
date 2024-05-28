package gaiasky.scene.component;

import com.badlogic.gdx.assets.AssetManager;
import gaiasky.data.api.OrientationServer;
import gaiasky.data.util.OrientationServerLoader.OrientationServerParameters;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

/**
 * Component that contains an attitude provider that spits out a quaternion for each time.
 */
public class AttitudeComponent {

    /** Quaternion orientation provider. */
    public String provider;

    /**
     * Source file(s) for the orientation server. Previously called
     * attitudeSource.
     */
    public String orientationSource;
    public OrientationServer orientationServer;

    public Vector3d nonRotatedPos;

    public void setOrientationProvider(String provider) {
        this.provider = provider;
    }

    public void setProvider(String provider) {
        setOrientationProvider(provider);
    }

    public AttitudeComponent copy() {
        var copy = new AttitudeComponent();
        copy.provider = provider;
        copy.orientationSource = orientationSource;
        copy.orientationServer = orientationServer;
        copy.nonRotatedPos = new Vector3d(nonRotatedPos);
        return copy;
    }

    public void initialize(AssetManager manager) {
        nonRotatedPos = new Vector3d();
        if (orientationSource != null && !orientationSource.isBlank()) {
            manager.load(orientationSource, OrientationServer.class, new OrientationServerParameters(provider));
        }
    }

    public void setUp(AssetManager manager) {
        if (isReady(manager)) {
            // Attitude-based models.
            orientationServer = manager.get(orientationSource);
        }
    }

    public void updateOrientation(Instant instant) {
        if (orientationServer != null) {
            orientationServer.updateOrientation(instant);
        }
    }

    public QuaternionDouble getCurrentQuaternion() {
        if (orientationServer != null) {
            return orientationServer.getCurrentOrientation();
        }
        return null;
    }

    public boolean isReady(AssetManager manager) {
        return orientationSource != null && manager.isLoaded(orientationSource);
    }

}
