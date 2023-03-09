package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.XrAction;
import org.lwjgl.openxr.XrActionCreateInfo;
import org.lwjgl.openxr.XrActionSet;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.openxr.XR10.xrCreateAction;
import static org.lwjgl.openxr.XR10.xrDestroyAction;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class Action implements AutoCloseable {

    protected XrAction handle;
    public final String name;
    public final String localizedName;
    public final int type;

    public Action(String name, String localizedName, int type) {
        this.name = name;
        this.localizedName = localizedName;
        this.type = type;
    }

    public void createHandle(XrActionSet actionSet, OpenXRDriver driver) {
        handle = createAction(driver, actionSet, type);
    }

    protected XrAction createAction(OpenXRDriver driver, XrActionSet actionSet, int type) {
        try (MemoryStack stack = stackPush()) {
            // Create action.
            XrActionCreateInfo createInfo = XrActionCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .actionName(stack.UTF8(name))
                    .localizedActionName(stack.UTF8(localizedName))
                    .countSubactionPaths(0)
                    .actionType(type);

            PointerBuffer pp = stack.mallocPointer(1);
            driver.check(xrCreateAction(actionSet, createInfo, pp));
            return new XrAction(pp.get(0), actionSet);
        }
    }

    public XrAction getHandle() {
        return handle;
    }

    public void destroyHandle() {
        xrDestroyAction(handle);
        if (this instanceof SpaceAwareAction) {
            ((SpaceAwareAction) this).destroyActionSpace();
        }
    }

    @Override
    public void close() {
        destroyHandle();
    }
}
