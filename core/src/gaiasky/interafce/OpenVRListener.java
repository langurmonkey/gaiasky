package gaiasky.interafce;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.StubModel;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.comp.ViewAngleComparator;
import gaiasky.util.math.Vector3d;
import gaiasky.vr.openvr.VRContext.VRControllerAxes;
import gaiasky.vr.openvr.VRContext.VRControllerButtons;
import gaiasky.vr.openvr.VRContext.VRDevice;
import gaiasky.vr.openvr.VRDeviceListener;

import java.util.*;

public class OpenVRListener implements VRDeviceListener {
    private static final Log logger = Logger.getLogger(OpenVRListener.class);

    /** The natural camera **/
    private final NaturalCamera cam;
    /** Focus comparator **/
    private final Comparator<IFocus> comp;
    /** Map from VR device to model object **/
    private HashMap<VRDevice, StubModel> vrDeviceToModel;
    /** Aux vectors **/
    private final Vector3d p0;
    private final Vector3d p1;

    private boolean vrControllerHint = false;
    private boolean vrInfoGui = false;
    private long lastDoublePress = 0l;

    // Selection
    private final long SELECTION_COUNTDOWN_MS = 2000;
    private boolean selecting = false;
    private long selectingTime = 0;
    private VRDevice selectingDevice;

    private long lastAxisMovedFrame = Long.MIN_VALUE;

    private final Set<Integer> pressedButtons;

    public OpenVRListener(NaturalCamera cam) {
        this.cam = cam;
        this.comp = new ViewAngleComparator<>();
        this.p0 = new Vector3d();
        this.p1 = new Vector3d();
        pressedButtons = new HashSet<>();
    }

    private void lazyInit() {
        if (vrDeviceToModel == null)
            vrDeviceToModel = GaiaSky.instance.getVRDeviceToModel();
    }

    public void connected(VRDevice device) {
        logger.info(device + " connected");
        EventManager.publish(Event.VR_DEVICE_CONNECTED, this, device);
    }

    public void disconnected(VRDevice device) {
        logger.info(device + " disconnected");
        EventManager.publish(Event.VR_DEVICE_DISCONNECTED, this, device);
    }

    /**
     * True if only the given button is pressed
     *
     * @param button
     * @return
     */
    private boolean isPressed(int button) {
        return pressedButtons.contains(button);
    }

    public void update() {
        long currentFrame = GaiaSky.instance.frames;
        if (currentFrame - lastAxisMovedFrame > 1) {
            cam.clearVelocityVR();
        }

        updateSelectionCountdown();
    }

    /**
     * Returns true if all given buttons are pressed
     *
     * @param buttons
     * @return
     */
    private boolean arePressed(int... buttons) {
        boolean result = true;
        for (int i = 0; i < buttons.length; i++)
            result = result && pressedButtons.contains(buttons[i]);
        return result;
    }

    public void buttonPressed(VRDevice device, int button) {
        logger.debug("vr button down [device/code]: " + device.toString() + " / " + button);

        lazyInit();
        // Add to pressed
        pressedButtons.add(button);

        // Selection countdown
        if (button == VRControllerButtons.SteamVR_Trigger) {
            // Start countdown
            startSelectionCountdown(device);
        }

        // VR controller hint
        if (arePressed(VRControllerButtons.A, VRControllerButtons.B)) {
            EventManager.publish(Event.DISPLAY_VR_CONTROLLER_HINT_CMD, this, true);
            vrControllerHint = true;
        }
    }

    public void buttonReleased(VRDevice device, int button) {
        logger.debug("vr button released [device/code]: " + device.toString() + " / " + button);

        // Removed from pressed
        pressedButtons.remove(button);

        if (TimeUtils.millis() - lastDoublePress > 250) {
            // Give some time to recover from double press
            lazyInit();
            if (vrControllerHint && !arePressed(VRControllerButtons.A, VRControllerButtons.B)) {
                EventManager.publish(Event.DISPLAY_VR_CONTROLLER_HINT_CMD, this, false);
                vrControllerHint = false;
                lastDoublePress = TimeUtils.millis();
            } else if (button == VRControllerButtons.B) {
                vrInfoGui = !vrInfoGui;
                EventManager.publish(Event.DISPLAY_VR_GUI_CMD, vrInfoGui);
            } else if (button == VRControllerButtons.A) {
                EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.labels");
            } else if (button == VRControllerButtons.SteamVR_Touchpad) {
                // Change mode from free to focus and viceversa
                CameraMode cm = cam.getMode().isFocus() ? CameraMode.FREE_MODE : CameraMode.FOCUS_MODE;
                // Stop
                cam.clearVelocityVR();

                EventManager.publish(Event.CAMERA_MODE_CMD, this, cm);
            }
        }
    }

    private void startSelectionCountdown(VRDevice device) {
        selecting = true;
        selectingTime = System.currentTimeMillis();
        selectingDevice = device;
        EventManager.publish(Event.VR_SELECTING_STATE, this, true, 0d);
    }

    private void updateSelectionCountdown() {
        if (selecting) {
            if (isPressed(VRControllerButtons.SteamVR_Trigger)) {
                long elapsed = System.currentTimeMillis() - selectingTime;
                double completion = (double) elapsed / (double) SELECTION_COUNTDOWN_MS;
                if (completion >= 1f) {
                    // Select object!
                    select(selectingDevice);
                    selecting = false;
                    selectingDevice = null;
                }
                EventManager.publish(Event.VR_SELECTING_STATE, this, selecting, completion);
            } else {
                // Stop selecting
                selecting = false;
                selectingDevice = null;
                EventManager.publish(Event.VR_SELECTING_STATE, this, false, 0d);
            }
        }
    }

    /**
     * Selects the object pointed by the given device.
     *
     * @param device
     */
    private void select(VRDevice device) {
        // Selection
        StubModel sm = vrDeviceToModel.get(device);
        if (sm != null) {
            p0.set(sm.getBeamP0());
            p1.set(sm.getBeamP1());
            IFocus hit = getBestHit(p0, p1);
            if (hit != null) {
                EventManager.publish(Event.FOCUS_CHANGE_CMD, this, hit);
                EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
            }
        } else {
            logger.info("Model corresponding to device not found");
        }
    }

    private Array<IFocus> getHits(Vector3d p0, Vector3d p1) {
        Array<IFocus> l = GaiaSky.instance.getFocusableEntities();

        Array<IFocus> hits = new Array<>();

        Iterator<IFocus> it = l.iterator();
        // Add all hits
        while (it.hasNext()) {
            IFocus s = it.next();
            s.addHit(p0, p1, cam, hits);
        }

        return hits;
    }

    private IFocus getBestHit(Vector3d p0, Vector3d p1) {
        Array<IFocus> hits = getHits(p0, p1);
        if (hits.size != 0) {
            // Sort using distance
            hits.sort(comp);
            // Get closest
            return hits.get(hits.size - 1);
        }
        return null;
    }

    @Override
    public void event(int code) {
        logger.debug("Unhandled event: " + code);
    }

    @Override
    public void buttonTouched(VRDevice device, int button) {
        logger.debug("vr button touched [device/code]: " + device.toString() + " / " + button);
    }

    @Override
    public void buttonUntouched(VRDevice device, int button) {
        logger.debug("vr button untouched [device/code]: " + device.toString() + " / " + button);
    }

    @Override
    public void axisMoved(VRDevice device, int axis, float valueX, float valueY) {
        logger.debug("axis moved: [device/axis/x/y]: " + device.toString() + " / " + axis + " / " + valueX + " / " + valueY);

        lazyInit();

        StubModel sm;
        switch (axis) {
        case VRControllerAxes.Axis1:
            // Forward
            //StubModel sm = vrDeviceToModel.get(device);
            //if (sm != null) {
            //    // Direct direction
            //   cam.setVelocityVR(sm.getBeamP0(), sm.getBeamP1(), valueX);
            //}
            break;
        case VRControllerAxes.Axis2:
            // Backward
            //sm = vrDeviceToModel.get(device);
            //if (sm != null) {
            //    // Invert direction
            //    cam.setVelocityVR(sm.getBeamP0(), sm.getBeamP1(), -valueX);
            //}
            break;
        case VRControllerAxes.Axis0:
            // Joystick for forward/backward movement
            sm = vrDeviceToModel.get(device);
            if (sm != null) {
                if (cam.getMode().isFocus()) {
                    if (pressedButtons.contains(VRControllerButtons.Axis2)) {
                        cam.addRotateMovement(valueX * 0.1, valueY * 0.1, false, false);
                    } else {
                        cam.setVelocityVR(sm.getBeamP0(), sm.getBeamP1(), valueX, valueY);
                    }
                } else {
                    cam.setVelocityVR(sm.getBeamP0(), sm.getBeamP1(), valueX, valueY);
                }
            }
            lastAxisMovedFrame = GaiaSky.instance.frames;

            break;
        }

    }
}
