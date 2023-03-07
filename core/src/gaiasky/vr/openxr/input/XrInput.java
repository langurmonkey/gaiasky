package gaiasky.vr.openxr.input;

import gaiasky.vr.openxr.*;
import gaiasky.vr.openxr.input.actions.Action;
import gaiasky.vr.openxr.input.actions.SessionAwareAction;
import gaiasky.vr.openxr.input.actionsets.GuiActionSet;
import gaiasky.vr.openxr.input.actionsets.HandsActionSet;
import gaiasky.vr.openxr.input.actionsets.GaiaSkyActionSet;
import org.lwjgl.openxr.XrActionSuggestedBinding;
import org.lwjgl.openxr.XrInteractionProfileSuggestedBinding;
import org.lwjgl.openxr.XrSessionActionSetsAttachInfo;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPointers;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.openxr.XR10.*;

public final class XrInput {
    public static final HandsActionSet handsActionSet = new HandsActionSet();
    public static final GaiaSkyActionSet vanillaGameplayActionSet = new GaiaSkyActionSet();
    public static final GuiActionSet guiActionSet = new GuiActionSet();

    private static long lastPollTime = 0;

    public static boolean teleport = false;

    private XrInput() {
    }

    //TODO registryify this
    public static void reinitialize(OpenXRDriver driver) throws XrException {

        handsActionSet.createHandle(driver);
        vanillaGameplayActionSet.createHandle(driver);
        guiActionSet.createHandle(driver);

        HashMap<String, List<Pair<Action, String>>> defaultBindings = new HashMap<>();
        handsActionSet.getDefaultBindings(defaultBindings);
        vanillaGameplayActionSet.getDefaultBindings(defaultBindings);
        guiActionSet.getDefaultBindings(defaultBindings);

        try (var stack = stackPush()) {
            for (var entry : defaultBindings.entrySet()) {
                var bindingsSet = entry.getValue();

                XrActionSuggestedBinding.Buffer bindings = XrActionSuggestedBinding.calloc(bindingsSet.size(), stack);

                for (int i = 0; i < bindingsSet.size(); i++) {
                    var binding = bindingsSet.get(i);
                    bindings.get(i).set(binding.getA().getHandle(), driver.getPath(binding.getB()));
                }

                XrInteractionProfileSuggestedBinding suggested_binds = XrInteractionProfileSuggestedBinding.calloc(stack)
                        .set(XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING,
                                NULL,
                                driver.getPath(entry.getKey()), bindings);

                try {
                    driver.check(xrSuggestInteractionProfileBindings(driver.xrInstance, suggested_binds));
                } catch (XrRuntimeException e) {
                    StringBuilder out = new StringBuilder(e.getMessage() + "\ninteractionProfile: " + entry.getKey());
                    for (var pair : bindingsSet) {
                        out.append("\n").append(pair.getB());
                    }
                    throw new XrRuntimeException(e.result, out.toString());
                }
            }

            XrSessionActionSetsAttachInfo attach_info = XrSessionActionSetsAttachInfo.calloc(stack).set(
                    XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO,
                    NULL,
                    stackPointers(vanillaGameplayActionSet.getHandle().address(),
                            guiActionSet.getHandle().address(),
                            handsActionSet.getHandle().address()));
            // Attach the action set we just made to the session
            driver.check(xrAttachSessionActionSets(driver.xrSession, attach_info), "xrAttachSessionActionSets");
        }

        for (Action action : handsActionSet.actions()) {
            if (action instanceof SessionAwareAction) {
                ((SessionAwareAction) action).createHandleSession(driver);
            }
        }
    }

    /**
     * Pre-tick + Pre-render, called once every frame
     */
    public static void pollActions() {
        long time = System.nanoTime();
        if (lastPollTime == 0) {
            lastPollTime = time;
        }


        lastPollTime = time;
    }
}
