package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import gaiasky.vr.openvr.VRContext;

public class VRDevice implements Component {

    public VRContext.VRDevice device;
    public Vector3 beamP0;
    public Vector3 beamP1;

}
