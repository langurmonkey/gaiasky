package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.data.attitude.IAttitudeServer;
import gaiasky.util.gaia.IAttitude;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3d;

public class Attitude implements Component {

    // Attitude
    public String provider;
    public String attitudeLocation;
    private IAttitudeServer attitudeServer;
    public IAttitude attitude;

    public Vector3d unrotatedPos;
    public Quaterniond quaterniond;
    public Quaternion quaternion;

}
