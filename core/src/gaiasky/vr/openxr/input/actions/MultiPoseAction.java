package gaiasky.vr.openxr.input.actions;

import gaiasky.util.i18n.I18n;
import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.OpenXRState;
import gaiasky.vr.openxr.XrException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.*;

import java.util.Arrays;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackCallocPointer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public class MultiPoseAction extends Action implements SessionAwareAction, InputAction {

    private static final XrActionStateGetInfo getInfo = XrActionStateGetInfo.calloc().type(XR10.XR_TYPE_ACTION_STATE_GET_INFO);
    private static final XrActionStatePose state = XrActionStatePose.calloc().type(XR10.XR_TYPE_ACTION_STATE_POSE);

    public final int amount;
    public final List<String> subactionPathsStr;
    public boolean[] isActive;
    public XrSpace[] spaces;

    public MultiPoseAction(String name, String[] subactionPathsStr) {
        super(name, XR10.XR_ACTION_TYPE_POSE_INPUT);
        this.subactionPathsStr = Arrays.asList(subactionPathsStr);
        this.amount = subactionPathsStr.length;
        this.isActive = new boolean[amount];
    }

    @Override
    public void createHandle(XrActionSet actionSet, OpenXRDriver driver) {
        try (var stack = stackPush()) {

            var subactionPaths = stack.callocLong(amount);

            for (int i = 0; i < amount; i++) {
                var str = subactionPathsStr.get(i);
                subactionPaths.put(i, driver.getPath(str));
            }

            String localizedName = "gaiasky.action." + this.name;
            if (I18n.exists(localizedName)) {
                localizedName = I18n.get(localizedName);
            }

            XrActionCreateInfo actionCreateInfo = XrActionCreateInfo.calloc(stack).set(
                    XR10.XR_TYPE_ACTION_CREATE_INFO,
                    NULL,
                    memUTF8("gaiasky." + this.name),
                    type,
                    amount,
                    subactionPaths,
                    memUTF8(localizedName)
            );
            PointerBuffer pp = stackCallocPointer(1);
            driver.check(XR10.xrCreateAction(actionSet, actionCreateInfo, pp));
            handle = new XrAction(pp.get(), actionSet);
        }
    }

    @Override
    public void createHandleSession(OpenXRDriver driver) throws XrException {
        try (var stack = stackPush()) {
            spaces = new XrSpace[amount];
            for (int i = 0; i < amount; i++) {
                XrActionSpaceCreateInfo action_space_info = XrActionSpaceCreateInfo.calloc(stack).set(
                        XR10.XR_TYPE_ACTION_SPACE_CREATE_INFO,
                        NULL,
                        handle,
                        driver.getPath(subactionPathsStr.get(i)),
                        OpenXRState.POSE_IDENTITY
                );
                PointerBuffer pp = stackCallocPointer(1);
                driver.check(XR10.xrCreateActionSpace(driver.xrSession, action_space_info, pp), "xrCreateActionSpace");
                spaces[i] = new XrSpace(pp.get(0), driver.xrSession);
            }
        }
    }

    @Override
    public void sync(OpenXRDriver driver) {
        for (int i = 0; i < amount; i++) {
            getInfo.subactionPath(driver.getPath(subactionPathsStr.get(i)));
            getInfo.action(handle);
            driver.check(XR10.xrGetActionStatePose(driver.xrSession, getInfo, state), "xrGetActionStatePose");
            isActive[i] = state.isActive();
        }
    }

    @Override
    public void destroyHandleSession() {
        if (spaces != null) {
            for (var space : spaces) {
                XR10.xrDestroySpace(space);
            }
        }
    }
}
