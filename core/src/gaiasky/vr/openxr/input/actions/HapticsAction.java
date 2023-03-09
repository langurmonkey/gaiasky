package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import org.lwjgl.openxr.XrActionSet;
import org.lwjgl.openxr.XrHapticActionInfo;
import org.lwjgl.openxr.XrHapticBaseHeader;
import org.lwjgl.openxr.XrHapticVibration;

import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class HapticsAction extends Action {

    public HapticsAction(String name, String localizedName) {
        super(name, localizedName, XR_ACTION_TYPE_VIBRATION_OUTPUT);
    }

    public void sendHapticPulse(OpenXRDriver driver, long duration, float frequency, float amplitude) {
        try (var stack = stackPush()) {
            // Haptic feedback.
            XrHapticActionInfo info = XrHapticActionInfo.calloc(stack)
                    .type$Default()
                    .next(NULL)
                    .action(handle);
            XrHapticVibration vibration = XrHapticVibration.calloc(stack)
                    .type(XR_TYPE_HAPTIC_VIBRATION)
                    .next(NULL)
                    .duration(duration)
                    .frequency(frequency)
                    .amplitude(amplitude);
            XrHapticBaseHeader header = XrHapticBaseHeader.create(vibration.address());
            driver.check(xrApplyHapticFeedback(driver.xrSession, info, header));
        }
    }

}
