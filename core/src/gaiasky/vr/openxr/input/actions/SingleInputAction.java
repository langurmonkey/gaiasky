package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.input.XrControllerDevice;
import org.lwjgl.openxr.*;

import static org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_GET_INFO;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public abstract class SingleInputAction<T> extends Action implements InputAction {

    protected static final XrActionStateGetInfo getInfo = XrActionStateGetInfo.calloc().type(XR_TYPE_ACTION_STATE_GET_INFO);

    public T currentState;
    public boolean changedSinceLastSync;
    public long lastChangeTime;
    public boolean isActive;

    public SingleInputAction(String name, String localizedName, int type, XrControllerDevice device) {
        super(name, localizedName, type, device);
    }

}
