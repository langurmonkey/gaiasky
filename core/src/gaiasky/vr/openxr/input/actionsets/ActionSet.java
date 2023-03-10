package gaiasky.vr.openxr.input.actionsets;

import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.input.actions.Action;
import gaiasky.vr.openxr.input.actions.InputAction;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.List;

import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.openxr.XR10.xrAttachSessionActionSets;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public abstract class ActionSet implements AutoCloseable {


    protected OpenXRDriver driver;
    public final String name;
    public final String localizedName;
    private XrActionSet handle;
    private final int priority;

    public ActionSet(String name, String localizedName, int priority) {
        this.name = name;
        this.localizedName = localizedName;
        this.priority = priority;
    }

    public abstract List<Action> actions();

    public boolean shouldSync() {
        return true;
    }

    public abstract void getDefaultBindings(HashMap<String, List<Pair<Action, String>>> map);

    public void sync(OpenXRDriver driver) {

        for (var action : actions()) {
            if (action instanceof InputAction) {
                ((InputAction) action).sync(driver);
            }
        }
    }

    public final void createHandle(OpenXRDriver driver) {
        try (var stack = stackPush()) {
            // Create action set.
            XrActionSetCreateInfo setCreateInfo = XrActionSetCreateInfo.malloc(stack)
                    .type$Default()
                    .actionSetName(stack.UTF8(name))
                    .localizedActionSetName(stack.UTF8(localizedName))
                    .priority(priority);

            PointerBuffer pp = stack.mallocPointer(1);
            driver.check(xrCreateActionSet(driver.xrInstance, setCreateInfo, pp));
            handle = new XrActionSet(pp.get(0), driver.xrInstance);

            for (var action : actions()) {
                action.createHandle(handle, driver);
            }
        }
    }

    /**
     * Attaches this (and only this) action set to the session in the driver.
     * @param driver The driver.
     * @param stack The memory stack.
     */
    public void attachToSession(OpenXRDriver driver, MemoryStack stack) {
        XrSessionActionSetsAttachInfo attachInfo = XrSessionActionSetsAttachInfo.calloc(stack).set(
                XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO,
                NULL,
                stackPointers(handle));
        driver.check(xrAttachSessionActionSets(driver.xrSession, attachInfo));
    }

    public final XrActionSet getHandle() {
        return handle;
    }

    public final void destroyHandles() {
        if (handle != null) {
            xrDestroyActionSet(handle);
        }
    }

    @Override
    public final void close() {
        destroyHandles();
        for (var action : actions()) {
            action.close();
        }
    }
}
