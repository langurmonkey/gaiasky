package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.comp.ViewAngleComparator;
import gaiasky.util.math.Vector3d;
import gaiasky.vr.openvr.VRContext.VRControllerAxes;
import gaiasky.vr.openvr.VRContext.VRControllerButtons;
import gaiasky.vr.openvr.VRContext.VRDevice;
import gaiasky.vr.openvr.VRDeviceListener;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class OpenVRListener implements VRDeviceListener {
    private static final Log logger = Logger.getLogger(OpenVRListener.class);

    /** Count-down timer for selection. How long we need to press the trigger pointing at an object for the selection to go through. **/
    private final static long SELECTION_COUNTDOWN_MS = 1500;

    /** The natural camera **/
    private final NaturalCamera cam;
    /** Focus comparator **/
    private final Comparator<Entity> comp;
    /** Aux vectors **/
    private final Vector3d p0;
    private final Vector3d p1;
    private final FocusView focusView;
    /** Map from VR device to model object **/
    private HashMap<VRDevice, Entity> vrDeviceToModel;
    /** All VR devices that are selecting right now. **/
    private final Set<VRDevice> selecting;
    private long selectingTime = 0;
    private long lastAxisMovedFrame = Long.MIN_VALUE;

    public OpenVRListener(NaturalCamera cam) {
        this.cam = cam;
        this.comp = new ViewAngleComparator<>();
        this.p0 = new Vector3d();
        this.p1 = new Vector3d();
        selecting = new HashSet<>();
        this.focusView = new FocusView();
    }

    private void lazyInit() {
        if (vrDeviceToModel == null) {
            vrDeviceToModel = GaiaSky.instance.getVRDeviceToModel();
        }
    }

    public void connected(VRDevice device) {
        logger.info(device + " connected");
        EventManager.publish(Event.VR_DEVICE_CONNECTED, this, device);
    }

    public void disconnected(VRDevice device) {
        logger.info(device + " disconnected");
        EventManager.publish(Event.VR_DEVICE_DISCONNECTED, this, device);
    }

    public void update() {
        long currentFrame = GaiaSky.instance.frames;
        if (currentFrame - lastAxisMovedFrame > 1) {
            cam.clearVelocityVR();
        }

        updateSelectionCountdown();
    }

    public boolean buttonPressed(VRDevice device, int button) {
        logger.debug("vr button down [device/code]: " + device.toString() + " / " + button);

        lazyInit();

        return true;
    }

    public boolean buttonReleased(VRDevice device, int button) {
        logger.debug("vr button released [device/code]: " + device.toString() + " / " + button);

        lazyInit();

        // Handle buttons.
        if (button == device.mappings.getButtonA() || button == device.mappings.getButtonY()) {
            EventManager.publish(Event.SHOW_VR_UI, this);
        } else if (button == device.mappings.getButtonB() || button == device.mappings.getButtonX()) {
            EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.labels");
        } else if (button == device.mappings.getButtonRstick()) {
            // Change mode from free to focus and vice-versa.
            CameraMode cm = cam.getMode().isFocus() ? CameraMode.FREE_MODE : CameraMode.FOCUS_MODE;
            // Stop.
            cam.clearVelocityVR();

            EventManager.publish(Event.CAMERA_MODE_CMD, this, cm);
        }
        return true;
    }

    private void startSelectionCountdown(VRDevice device) {
        selecting.add(device);
        selectingTime = System.currentTimeMillis();
        EventManager.publish(Event.VR_SELECTING_STATE, this, true, 0d, device);
    }

    private void updateSelectionCountdown() {
        for (var device : selecting) {
            if (device.isButtonPressed(device.mappings.getButtonRT()) || device.isAxisPressed(device.mappings.getAxisRT())) {
                long elapsed = System.currentTimeMillis() - selectingTime;
                // Selection
                double completion = (double) elapsed / (double) SELECTION_COUNTDOWN_MS;
                if (completion >= 1f) {
                    // Select object!
                    select(device);
                    // Finish
                    EventManager.publish(Event.VR_SELECTING_STATE, this, false, completion, device);
                    selecting.remove(device);
                } else {
                    // Keep on.
                    EventManager.publish(Event.VR_SELECTING_STATE, this, selecting.contains(device), completion, device);
                }
            } else {
                // Stop selecting
                EventManager.publish(Event.VR_SELECTING_STATE, this, false, 0d, device);
                selecting.remove(device);
            }
        }

    }

    private void stopSelectionCountdown(VRDevice device) {
        if (selecting.contains(device)) {
            // Stop selecting
            EventManager.publish(Event.VR_SELECTING_STATE, this, false, 0d, device);
            selecting.remove(device);
            long elapsed = System.currentTimeMillis() - selectingTime;
            double completion = (double) elapsed / (double) SELECTION_COUNTDOWN_MS;
            if (completion >= 1f) {
                select(device);
            }
        }
    }

    /**
     * Selects the object pointed by the given device.
     *
     * @param device The VR device.
     */
    private void select(VRDevice device) {
        // Selection
        Entity sm = vrDeviceToModel.get(device);
        if (sm != null && Mapper.vr.has(sm)) {
            var vr = Mapper.vr.get(sm);
            p0.set(vr.beamP0);
            p1.set(vr.beamP2);
            Entity hit = getBestHit(p0, p1);
            if (hit != null) {
                EventManager.publish(Event.FOCUS_CHANGE_CMD, this, hit);
                EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
                device.triggerHapticPulse(Short.MAX_VALUE);
            }
        } else {
            logger.info("Model corresponding to device not found");
        }
    }

    private Array<Entity> getHits(Vector3d p0, Vector3d p1) {
        Array<Entity> l = cam.getScene().findFocusableEntities();
        Array<Entity> hits = new Array<>();

        // Add all hits
        for (Entity s : l) {
            focusView.setEntity(s);
            focusView.addEntityHitRay(p0, p1, cam, hits);
        }

        return hits;
    }

    private Entity getBestHit(Vector3d p0, Vector3d p1) {
        Array<Entity> hits = getHits(p0, p1);
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
    public boolean buttonTouched(VRDevice device, int button) {
        logger.debug("vr button touched [device/code]: " + device.toString() + " / " + button);
        return false;
    }

    @Override
    public boolean buttonUntouched(VRDevice device, int button) {
        logger.debug("vr button untouched [device/code]: " + device.toString() + " / " + button);
        return false;
    }

    @Override
    public boolean axisMoved(VRDevice device, int axis, float valueX, float valueY) {
        logger.debug("axis moved: [device/axis/x/y]: " + device.toString() + " / " + axis + " / " + valueX + " / " + valueY);

        lazyInit();

        boolean selectingDevice = selecting.contains(device);

        Entity sm = vrDeviceToModel.get(device);
        var vr = sm != null ? Mapper.vr.get(sm) : null;
        if (vr != null && !vr.hitUI) {
            if (axis == device.mappings.getAxisRT()) {
                // Trigger in Oculus Rift controllers.
                // Selection.
                if (!selectingDevice && device.isAxisPressed(axis) && !vr.hitUI) {
                    startSelectionCountdown(device);
                } else if (selectingDevice && valueX == 0) {
                    stopSelectionCountdown(device);
                }
            } else if (axis == device.mappings.getAxisRstickV() || axis == device.mappings.getAxisRstickH()) {
                // Joystick for forward/backward movement
                if (cam.getMode().isFocus()) {
                    if (device.isAxisPressed(device.mappings.getAxisRB()) || device.isAxisPressed(device.mappings.getAxisLB()) ) {
                        cam.addRotateMovement(valueX * 0.1, valueY * 0.1, false, false);
                    } else {
                        cam.setVelocityVR(vr.beamP0, vr.beamP1, valueX, valueY);
                        //cam.addRotateMovement(valueX * 0.1, 0, false, false);
                    }
                } else {
                    cam.setVelocityVR(vr.beamP0, vr.beamP1, valueX, valueY);
                }
                lastAxisMovedFrame = GaiaSky.instance.frames;
            }
            return true;
        }
        return false;
    }
}
