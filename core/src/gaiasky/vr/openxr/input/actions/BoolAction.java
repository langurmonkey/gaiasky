package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrActionStateBoolean;

public class BoolAction extends SingleInputAction<Boolean> {

    private static final XrActionStateBoolean state = XrActionStateBoolean.calloc().type(XR10.XR_TYPE_ACTION_STATE_BOOLEAN);

    public BoolAction(String name) {
        super(name, XR10.XR_ACTION_TYPE_BOOLEAN_INPUT);
        currentState = false;
    }

    @Override
    public void sync(OpenXRDriver driver) {
        getInfo.action(handle);
        driver.check(XR10.xrGetActionStateBoolean(driver.xrSession, getInfo, state), "xrGetActionStateBoolean");
        this.currentState = state.currentState();
        this.changedSinceLastSync = state.changedSinceLastSync();
        this.lastChangeTime = state.lastChangeTime();
        this.isActive = state.isActive();
    }
}
