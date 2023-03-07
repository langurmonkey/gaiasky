package gaiasky.vr.openxr.input;

import org.lwjgl.openxr.XrVector2f;

/**
 * Listener for OpenXR events.
 */
public interface OpenXRInputListener {

    boolean buttonA(boolean value);
    boolean buttonB(boolean value);
    boolean buttonTrigger(boolean value);
    boolean buttonThumbstick(boolean value);
    boolean thumbstick(XrVector2f value);
    boolean trigger(float value);
}
