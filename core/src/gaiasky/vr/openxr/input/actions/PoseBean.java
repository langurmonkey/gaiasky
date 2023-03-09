package gaiasky.vr.openxr.input.actions;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * Represents the pose of a single device, typically a VR controller.
 */
public class PoseBean {
    public boolean active = false;
    public boolean left = false;
    public Vector3 position = new Vector3();
    public Quaternion orientation = new Quaternion();
}
