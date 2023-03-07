package gaiasky.vr.openxr.input.actionsets;

import gaiasky.util.i18n.I18n;
import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.OpenXRSession;
import gaiasky.vr.openxr.XrException;
import gaiasky.vr.openxr.input.actions.Action;
import gaiasky.vr.openxr.input.actions.InputAction;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrActionSet;
import org.lwjgl.openxr.XrActionSetCreateInfo;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackCallocPointer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public abstract class ActionSet implements AutoCloseable {


    protected OpenXRDriver driver;
    public final String name;
    private XrActionSet handle;
    private int priority;

    public ActionSet(String name, int priority) {
        this.name = name;
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

    public final void createHandle(OpenXRDriver driver) throws XrException {
        try (var stack = stackPush()) {
            String localizedName = "mcxr.actionset." + this.name;
            if (I18n.exists(localizedName)) {
                localizedName = I18n.get(localizedName);
            }

            XrActionSetCreateInfo actionSetCreateInfo = XrActionSetCreateInfo.calloc(stack).set(XR10.XR_TYPE_ACTION_SET_CREATE_INFO, NULL, memUTF8("mcxr." + this.name), memUTF8(I18n.get(localizedName)), priority);
            PointerBuffer pp = stackCallocPointer(1);
            driver.check(XR10.xrCreateActionSet(driver.xrInstance, actionSetCreateInfo, pp));
            handle = new XrActionSet(pp.get(0), driver.xrInstance);

            for (var action : actions()) {
                action.createHandle(handle, driver);
            }
        }
    }

    public final XrActionSet getHandle() {
        return handle;
    }

    public final void destroyHandles() {
        if (handle != null) {
            XR10.xrDestroyActionSet(handle);
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
