package gaiasky.vr.openxr.input;

import com.badlogic.gdx.math.Vector2;

/**
 * Listener for OpenXR events in Gaia Sky.
 */
public interface OpenXRInputListener {

    boolean showUI(boolean value);
    boolean accept(boolean value);
    boolean cameraMode(boolean value);
    boolean rotate(boolean value);
    boolean move(Vector2 value);
    boolean select(float value);
}
