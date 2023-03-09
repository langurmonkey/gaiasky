package gaiasky.vr.openxr.input;

import com.badlogic.gdx.math.Vector2;
import gaiasky.vr.openxr.input.actions.Action;

/**
 * Listener for OpenXR events in Gaia Sky.
 */
public interface OpenXRInputListener {

    boolean showUI(boolean value, Action.DeviceType type);
    boolean accept(boolean value, Action.DeviceType type);
    boolean cameraMode(boolean value, Action.DeviceType type);
    boolean rotate(boolean value, Action.DeviceType type);
    boolean move(Vector2 value, Action.DeviceType type);
    boolean select(float value, Action.DeviceType type);
}
