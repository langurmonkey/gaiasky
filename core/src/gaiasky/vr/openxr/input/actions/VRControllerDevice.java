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
        // The last bit (rotate 40 degrees around x) is due to this:
        // Our controller models are positioned in local space for OpenVR poses.
        // In OpenXR we have two poses: grip and aim. We use aim, which is
        // located at the pointy end of the controller, but its orientation must be rotated a bit to match
        // that of OpenVR.
        // More info: https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#semantic-path-standard-pose-identifiers
        modelInstance.transform.idt().translate(position).rotate(orientation).rotate(1, 0, 0, 40);
    }

    public IntModelInstance getModelInstance() {
        return modelInstance;
    }
}
