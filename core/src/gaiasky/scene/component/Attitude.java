package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.util.gaia.IAttitude;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3d;

public class Attitude implements Component {

    public Vector3d unrotatedPos;
    public IAttitude attitude;
    public Quaterniond quaterniond;
    public Quaternion quaternion;

}
