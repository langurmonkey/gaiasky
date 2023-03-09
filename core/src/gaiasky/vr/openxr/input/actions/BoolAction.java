package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrActionStateBoolean;

import static org.lwjgl.openxr.XR10.*;

public class BoolAction extends SingleInputAction<Boolean> {

    private static final XrActionStateBoolean state = XrActionStateBoolean.calloc().type(XR_TYPE_ACTION_STATE_BOOLEAN);

    public BoolAction(String name, String localizedName, DeviceType deviceType) {
        super(name, localizedName, XR_ACTION_TYPE_BOOLEAN_INPUT, deviceType);
        currentState = false;
    }

    @Override
    public void sync(OpenXRDriver driver) {
        getInfo.action(handle);
        driver.check(xrGetActionStateBoolean(driver.xrSession, getInfo, state), "xrGetActionStateBoolean");
        this.currentState = state.currentState();
        this.changedSinceLastSync = state.changedSinceLastSync();
        this.lastChangeTime = state.lastChangeTime();
        this.isActive = state.isActive();
    }
}
