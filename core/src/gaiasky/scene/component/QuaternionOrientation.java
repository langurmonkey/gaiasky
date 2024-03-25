package gaiasky.scene.component;

import com.badlogic.gdx.assets.AssetManager;
import gaiasky.data.api.OrientationServer;
import gaiasky.data.util.OrientationServerLoader.OrientationServerParameters;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

public class QuaternionOrientation implements Cloneable {

    /** Attitude provider. */
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

    public QuaternionOrientation clone() {
        try {
            var copy = (QuaternionOrientation) super.clone();
            copy.provider = provider;
            copy.orientationSource = orientationSource;
            copy.orientationServer = orientationServer;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
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

    public QuaternionDouble getQuaternion(Instant instant) {
        if(orientationServer != null) {
            return orientationServer.getOrientation(instant);
        }
        return null;
    }

    public QuaternionDouble getCurrentQuaternion() {
        if(orientationServer != null) {
            return orientationServer.getLastOrientation();
        }
        return null;
    }

    public boolean isReady(AssetManager manager) {
        return orientationSource != null && manager.isLoaded(orientationSource);
    }

}
