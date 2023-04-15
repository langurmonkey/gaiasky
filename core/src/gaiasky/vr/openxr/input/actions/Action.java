package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.XrDriver;
import gaiasky.vr.openxr.input.XrControllerDevice;
import gaiasky.vr.openxr.input.XrControllerDevice.DeviceType;
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
    public final int xrActionType;
    // The controller device attached to this pose.
    protected final XrControllerDevice controllerDevice;

    public Action(String name, String localizedName, int type, XrControllerDevice device) {
        this.name = name;
        this.localizedName = localizedName;
        this.xrActionType = type;
        this.controllerDevice = device;
    }

    public void createHandle(XrActionSet actionSet, XrDriver driver) {
        handle = createAction(driver, actionSet, xrActionType);
        if (this instanceof SpaceAwareAction) {
            ((SpaceAwareAction) this).createActionSpace(driver);
        }
    }

    protected XrAction createAction(XrDriver driver, XrActionSet actionSet, int type) {
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

    public XrControllerDevice getControllerDevice() {
        return controllerDevice;
    }

    public DeviceType getDeviceType() {
        return controllerDevice.deviceType;
    }

}