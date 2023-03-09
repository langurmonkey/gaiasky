package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrActionStateFloat;

import static org.lwjgl.openxr.XR10.*;

public class FloatAction extends SingleInputAction<Float> {
    
    private static final XrActionStateFloat state = XrActionStateFloat.calloc().type(XR_TYPE_ACTION_STATE_FLOAT);

    public FloatAction(String name, String localizedName, DeviceType deviceType) {
        super(name, localizedName, XR_ACTION_TYPE_FLOAT_INPUT, deviceType);
        currentState = 0f;
    }

    @Override
    public void sync(OpenXRDriver driver) {
        getInfo.action(handle);
        driver.check(xrGetActionStateFloat(driver.xrSession, getInfo, state), "xrGetActionStateFloat");
        this.currentState = state.currentState();
        this.changedSinceLastSync = state.changedSinceLastSync();
        this.lastChangeTime = state.lastChangeTime();
        this.isActive = state.isActive();
    }
}
