package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrActionStateFloat;

public class FloatAction extends SingleInputAction<Float> {
    
    private static final XrActionStateFloat state = XrActionStateFloat.calloc().type(XR10.XR_TYPE_ACTION_STATE_FLOAT);

    public FloatAction(String name) {
        super(name, XR10.XR_ACTION_TYPE_FLOAT_INPUT);
        currentState = 0f;
    }

    @Override
    public void sync(OpenXRDriver driver) {
        getInfo.action(handle);
        driver.check(XR10.xrGetActionStateFloat(driver.xrSession, getInfo, state), "xrGetActionStateFloat");
        this.currentState = state.currentState();
        this.changedSinceLastSync = state.changedSinceLastSync();
        this.lastChangeTime = state.lastChangeTime();
        this.isActive = state.isActive();
    }
}
