package gaiasky.vr.openxr.input;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.vr.openxr.XrDriver;
import gaiasky.vr.openxr.XrHelper;
import gaiasky.vr.openxr.input.actions.*;
import org.lwjgl.openxr.XrPosef;

/**
 * Represents a single VR controller device, and keeps its pose (position and
 * orientation) up to date. It also holds the model.
 */
public class XrControllerDevice {

    /**
     * Reflects the source device of this action. Either left or right.
     */
    public enum DeviceType {
        Left,
        Right;

        public boolean isLeft() {
            return this == Left;
        }

        public boolean isRight() {
            return this == Right;
        }
    }

    public final DeviceType deviceType;
    public boolean active = false;
    private boolean initialized = false;

    public Vector3 position = new Vector3();
    public Quaternion orientation = new Quaternion();
    public IntModelInstance modelInstance;

    // Actions
    public BoolAction showUi, accept, cameraMode;
    public FloatAction select;
    public Vec2fAction move;
    public PoseAction pose;
    public HapticsAction haptics;

    public XrControllerDevice(DeviceType type) {
        this.deviceType = type;
    }

    public void initialize(XrDriver driver) {
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

    /**
     * Sends a haptic pulse to this device.
     *
     * @param driver      The XR driver.
     * @param nanoseconds The length of the pulse in nanoseconds.
     * @param frequency   The frequency in Hz.
     * @param amplitude   The amplitude in [0,1].
     */
    public void sendHapticPulse(XrDriver driver, long nanoseconds, float frequency, float amplitude) {
        if (haptics != null) {
            haptics.sendHapticPulse(driver, nanoseconds, frequency, amplitude);
        }
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

    public void processListener(XrInputListener listener) {
        processShowUIAction(showUi, listener);
        processCameraModeAction(cameraMode, listener);
        processAcceptAction(accept, listener);
        processSelectAction(select, listener);
        processMoveAction(move, listener);
    }

    private void processShowUIAction(BoolAction action, XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.showUI(action.currentState, action.getControllerDevice());
        }
    }

    private void processCameraModeAction(BoolAction action, XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.cameraMode(action.currentState, action.getControllerDevice());
        }
    }

    private void processAcceptAction(BoolAction action, XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.accept(action.currentState, action.getControllerDevice());
        }
    }

    private void processSelectAction(FloatAction action, XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.select(action.currentState, action.getControllerDevice());
        }
    }

    private void processMoveAction(Vec2fAction action, XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.move(action.currentState, action.getControllerDevice());
        }
    }
}
