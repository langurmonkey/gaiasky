package gaiasky.vr.openxr.input.actions;

import gaiasky.util.i18n.I18n;
import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.XrException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.*;

import static org.lwjgl.system.MemoryStack.stackCallocPointer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public abstract class SingleInputAction<T> extends Action implements InputAction {

    protected static final XrActionStateGetInfo getInfo = XrActionStateGetInfo.calloc().type(XR10.XR_TYPE_ACTION_STATE_GET_INFO);

    public T currentState;
    public boolean changedSinceLastSync;
    public long lastChangeTime;
    public boolean isActive;

    public SingleInputAction(String name, int type) {
        super(name, type);
    }

    @Override
    public void createHandle(XrActionSet actionSet, OpenXRDriver driver) throws XrException {
        try (var stack = stackPush()) {
            String localizedName = "mcxr.action." + this.name;
            if (I18n.exists(localizedName)) {
                localizedName = I18n.msg(localizedName);
            }

            XrActionCreateInfo actionCreateInfo = XrActionCreateInfo.calloc(stack).set(
                    XR10.XR_TYPE_ACTION_CREATE_INFO,
                    NULL,
                    memUTF8("mcxr." + this.name),
                    type,
                    0,
                    null,
                    memUTF8(localizedName)
            );
            PointerBuffer pp = stackCallocPointer(1);
            driver.check(XR10.xrCreateAction(actionSet, actionCreateInfo, pp));
            handle = new XrAction(pp.get(), actionSet);
        }
    }
}
