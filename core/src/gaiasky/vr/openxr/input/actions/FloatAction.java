package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.XrDriver;
import gaiasky.vr.openxr.input.XrControllerDevice;
import org.lwjgl.openxr.XrActionStateFloat;

import static org.lwjgl.openxr.XR10.*;

public class FloatAction extends SingleInputAction<Float> {
    
    private static final XrActionStateFloat state = XrActionStateFloat.calloc().type(XR_TYPE_ACTION_STATE_FLOAT);

    private static final float threshold = 1e-3f;

    public FloatAction(String name, String localizedName, XrControllerDevice device) {
        super(name, localizedName, XR_ACTION_TYPE_FLOAT_INPUT, device);
        currentState = 0f;
    }

    @Override
    public void sync(XrDriver driver) {
        getInfo.action(handle);
        driver.check(xrGetActionStateFloat(driver.xrSession, getInfo, state), "xrGetActionStateFloat");
        this.currentState = state.currentState() < threshold ? 0 : state.currentState();
        this.changedSinceLastSync = state.changedSinceLastSync();
        this.lastChangeTime = state.lastChangeTime();
        this.isActive = state.isActive();
    }
}
