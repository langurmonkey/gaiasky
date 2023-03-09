package gaiasky.vr.openxr.input.actions;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.XrHelper;
import org.joml.Matrix4f;
import org.lwjgl.openxr.XrPosef;

/**
 * Represents a single VR controller device, and keeps its pose (position and
 * orientation) up to date. It also holds the model.
 */
public class VRControllerDevice {
    public boolean active = false;
    public Action.DeviceType deviceType = Action.DeviceType.Right;
    private boolean initialized = false;
    public Matrix4f transform = new Matrix4f();
    public Vector3 position = new Vector3();
    public Quaternion orientation = new Quaternion();
    public IntModelInstance modelInstance;

    public void initialize(OpenXRDriver driver) {
        var model = XrHelper.loadRenderModel(driver, this);
        modelInstance = new IntModelInstance(model);
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isActive() {
        return active;
    }

    public void setFromPose(XrPosef pose) {
        var pos = pose.position$();
        var ori = pose.orientation();
        position.set(pos.x(), pos.y(), pos.z());
        orientation.set(ori.x(), ori.y(), ori.z(), ori.w());
        transform.translationRotateScaleInvert(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w, 1, 1, 1);
        // Set model instance transform.
        transform.get(modelInstance.transform.val);
    }

    public IntModelInstance getModelInstance() {
        return modelInstance;
    }
}
