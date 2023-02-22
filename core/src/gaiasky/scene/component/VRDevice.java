package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.math.Vector3d;
import gaiasky.vr.openvr.VRContext;

public class VRDevice implements Component {

    public VRContext.VRDevice device;
    public Vector3d beamP0 = new Vector3d();
    public Vector3d beamP1 = new Vector3d();
    public boolean hitUI = false;

}
