package gaiasky.vr.openxr.input;

import com.badlogic.gdx.math.Vector2;
import gaiasky.vr.openxr.input.actions.Action;

/**
 * Listener for OpenXR events in Gaia Sky.
 */
public interface XrInputListener {

    boolean showUI(boolean value, XrControllerDevice device);
    boolean accept(boolean value, XrControllerDevice device);
    boolean cameraMode(boolean value, XrControllerDevice device);
    boolean rotate(boolean value, XrControllerDevice device);
    boolean move(Vector2 value, XrControllerDevice device);
    boolean select(float value, XrControllerDevice device);
}
