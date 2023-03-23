package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.comp.ViewAngleComparator;
import gaiasky.util.math.Vector3d;
import gaiasky.vr.openxr.XrDriver;
import gaiasky.vr.openxr.input.XrControllerDevice;
import gaiasky.vr.openxr.input.XrInputListener;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpenXRListener implements XrInputListener, IObserver {
    private static final Log logger = Logger.getLogger(OpenXRListener.class);

    /** Count-down timer for selection. How long we need to press the trigger pointing at an object for the selection to go through. **/
    private final static long SELECTION_COUNTDOWN_MS = 1500;

    /** The natural camera. **/
    private final NaturalCamera cam;
    /** The OpenXR driver. **/
    private XrDriver driver;
    /** Focus comparator **/
    private final Comparator<Entity> comp;
    /** Aux vectors **/
    private final Vector3d p0;
    private final Vector3d p1;
    private final FocusView focusView;
    /** Map from VR device to model object **/
    private Map<XrControllerDevice, Entity> xrControllerToModel;
    /** All VR devices that are selecting right now. **/
    private final Set<XrControllerDevice> selecting;
    private long selectingTime = 0;
    private long lastAxisMovedFrame = Long.MIN_VALUE;

    public OpenXRListener(NaturalCamera cam) {
        this.cam = cam;
        this.comp = new ViewAngleComparator<>();
        this.p0 = new Vector3d();
        this.p1 = new Vector3d();
        selecting = new HashSet<>();
        this.focusView = new FocusView();

        EventManager.instance.subscribe(this, Event.VR_DRIVER_LOADED);
    }

    private void lazyInit() {
        if (xrControllerToModel == null) {
            xrControllerToModel = GaiaSky.instance.sceneRenderer.getXRControllerToModel();
        }
    }

    public void update() {
        long currentFrame = GaiaSky.instance.frames;
        if (currentFrame - lastAxisMovedFrame > 1) {
            cam.clearVelocityVR();
        }

        updateSelectionCountdown();
    }

    private void startSelectionCountdown(XrControllerDevice device) {
        selecting.add(device);
        selectingTime = System.currentTimeMillis();
        EventManager.publish(Event.VR_SELECTING_STATE, this, true, 0d, device);
    }

    private void updateSelectionCountdown() {
        for (var device : selecting) {
            if (device.select.currentState > 0) {
                long elapsed = System.currentTimeMillis() - selectingTime;
                // Selection
                double completion = (double) elapsed / (double) SELECTION_COUNTDOWN_MS;
                if (completion >= 1f) {
                    // Select object!
                    doSelection(device);
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

    private void stopSelectionCountdown(XrControllerDevice device) {
        if (selecting.contains(device)) {
            // Stop selecting
            EventManager.publish(Event.VR_SELECTING_STATE, this, false, 0d, device);
            selecting.remove(device);
            long elapsed = System.currentTimeMillis() - selectingTime;
            double completion = (double) elapsed / (double) SELECTION_COUNTDOWN_MS;
            if (completion >= 1f) {
                doSelection(device);
            }
        }
    }

    /**
     * Selects the object pointed by the given device.
     *
     * @param device The VR device.
     */
    private void doSelection(XrControllerDevice device) {
        // Selection
        lazyInit();
        Entity sm = xrControllerToModel.get(device);
        if (sm != null && Mapper.vr.has(sm)) {
            var vr = Mapper.vr.get(sm);
            p0.set(vr.beamP0);
            p1.set(vr.beamP2);
            Entity hit = getBestHit(p0, p1);
            if (hit != null) {
                EventManager.publish(Event.FOCUS_CHANGE_CMD, this, hit);
                EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
                // Trigger haptic pulse on the device.
                device.sendHapticPulse(driver, 300_000_000L, 150, 1);
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
    public boolean showUI(boolean value, XrControllerDevice device) {
        // On release.
        if (!value) {
            EventManager.publish(Event.SHOW_VR_UI, this);
            return true;
        }
        return false;
    }

    @Override
    public boolean accept(boolean value, XrControllerDevice device) {
        return false;
    }

    @Override
    public boolean cameraMode(boolean value, XrControllerDevice device) {
        // On release.
        if (!value) {
            // Change mode from free to focus and vice-versa.
            CameraMode cm = cam.getMode().isFocus() ? CameraMode.FREE_MODE : CameraMode.FOCUS_MODE;
            // Stop.
            cam.clearVelocityVR();

            EventManager.publish(Event.CAMERA_MODE_CMD, this, cm);
            return true;
        }
        return false;
    }

    @Override
    public boolean rotate(boolean value, XrControllerDevice device) {
        return false;
    }

    @Override
    public boolean move(Vector2 value, XrControllerDevice device) {
        // Joystick for forward/backward movement
        float valueX = value.x;
        float valueY = value.y;
        lazyInit();
        Entity sm = xrControllerToModel.get(device);
        var vr = sm != null ? Mapper.vr.get(sm) : null;
        if (vr != null) {
            if (cam.getMode().isFocus()) {
                cam.setVelocityVR(vr.beamP0, vr.beamP1, valueX, valueY);
            } else {
                cam.setVelocityVR(vr.beamP0, vr.beamP1, valueX, valueY);
            }
        }
        lastAxisMovedFrame = GaiaSky.instance.frames;
        return false;
    }

    @Override
    public boolean select(float value, XrControllerDevice device) {
        // Selection.
        lazyInit();
        Entity sm = xrControllerToModel.get(device);
        var vr = sm != null ? Mapper.vr.get(sm) : null;
        boolean selectingDevice = selecting.contains(device);
        if (!selectingDevice && value != 0f && !vr.hitUI) {
            startSelectionCountdown(device);
        } else if (selectingDevice && value == 0f) {
            stopSelectionCountdown(device);
        }
        return false;
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.VR_DRIVER_LOADED) {
            this.driver = (XrDriver) data[0];
        }
    }
}
