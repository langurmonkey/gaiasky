package gaiasky.vr.openxr.input.actions;

import com.badlogic.gdx.math.Vector2;
import gaiasky.vr.openxr.OpenXRDriver;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrActionStateVector2f;

public class Vec2fAction extends SingleInputAction<Vector2> {

    private static final XrActionStateVector2f state = XrActionStateVector2f.calloc().type(XR10.XR_TYPE_ACTION_STATE_VECTOR2F);

    public Vec2fAction(String name) {
        super(name, XR10.XR_ACTION_TYPE_VECTOR2F_INPUT);
        currentState = new Vector2();
    }

    @Override
    public void sync(OpenXRDriver driver) {
        getInfo.action(handle);
        driver.check(XR10.xrGetActionStateVector2f(driver.xrSession, getInfo, state), "xrGetActionStateBoolean");
        this.currentState.x = state.currentState().x();
        this.currentState.y = state.currentState().y();
        this.changedSinceLastSync = state.changedSinceLastSync();
        this.lastChangeTime = state.lastChangeTime();
        this.isActive = state.isActive();
    }
}
