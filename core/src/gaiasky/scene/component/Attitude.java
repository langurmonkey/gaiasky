package gaiasky.scene.component;

import com.artemis.Component;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.util.gaia.IAttitude;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3d;

public class Attitude extends Component {

    public Vector3d unrotatedPos;
    public IAttitude attitude;
    public Quaterniond quaterniond;
    public Quaternion quaternion;

}
