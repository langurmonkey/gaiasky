/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.utils.IntSet;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

import java.nio.file.Files;
import java.nio.file.Path;

public class NaturalControllerListener implements ControllerListener, IObserver, IInputListener {
    private static final Log logger = Logger.getLogger(NaturalControllerListener.class);

    private final NaturalCamera cam;
    private IControllerMappings mappings;
    private final EventManager em;

    private final IntSet pressedKeys;

    public NaturalControllerListener(NaturalCamera cam, String mappingsFile) {
        this.cam = cam;
        this.em = EventManager.instance;
        this.pressedKeys = new IntSet();
        updateControllerMappings(mappingsFile);

        em.subscribe(this, Event.RELOAD_CONTROLLER_MAPPINGS);
    }

    public void addPressedKey(int keycode) {
        pressedKeys.add(keycode);
    }

    public void removePressedKey(int keycode) {
        pressedKeys.remove(keycode);
    }

    public boolean isKeyPressed(int keycode) {
        return pressedKeys.contains(keycode);
    }

    /**
     * Returns true if all keys are pressed
     *
     * @param keys The keys to test
     * @return True if all are pressed
     */
    public boolean allPressed(int... keys) {
        for (int k : keys) {
            if (!pressedKeys.contains(k))
                return false;
        }
        return true;
    }

    /**
     * Returns true if any of the keys are pressed
     *
     * @param keys The keys to test
     * @return True if any is pressed
     */
    public boolean anyPressed(int... keys) {
        for (int k : keys) {
            if (pressedKeys.contains(k))
                return true;
        }
        return false;
    }

    public IControllerMappings getMappings() {
        return mappings;
    }

    public boolean updateControllerMappings(String mappingsFile) {
        if (Files.exists(Path.of(mappingsFile))) {
            mappings = new ControllerMappings(null, Path.of(mappingsFile));
        } else {
            Path internalMappings = Path.of(Settings.ASSETS_LOC).resolve(mappingsFile);
            if(Files.exists(internalMappings)){
                mappings = new ControllerMappings(null, internalMappings);
            }
        }
        return false;
    }

    @Override
    public void connected(Controller controller) {
        logger.info("Controller connected: " + controller.getName());
        em.post(Event.CONTROLLER_CONNECTED_INFO, this, controller.getName());
    }

    @Override
    public void disconnected(Controller controller) {
        logger.info("Controller disconnected: " + controller.getName());
        em.post(Event.CONTROLLER_DISCONNECTED_INFO, this, controller.getName());
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        logger.debug("button down [inputListener/code]: " + controller.getName() + " / " + buttonCode);

        cam.setInputByController(true);

        addPressedKey(buttonCode);

        return true;
    }

    @Override
    public boolean buttonUp(Controller controller, final int buttonCode) {
        logger.debug("button up [inputListener/code]: " + controller.getName() + " / " + buttonCode);

        if (buttonCode == mappings.getButtonX()) {
            em.post(Event.TOGGLE_MINIMAP, this);
        } else if (buttonCode == mappings.getButtonY()) {
            em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.orbits");
        } else if (buttonCode == mappings.getButtonA()) {
            em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.labels");
        } else if (buttonCode == mappings.getButtonB()) {
            em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.asteroids");
        } else if (buttonCode == mappings.getButtonDpadUp()) {
            em.post(Event.STAR_POINT_SIZE_INCREASE_CMD, this);
        } else if (buttonCode == mappings.getButtonDpadDown()) {
            em.post(Event.STAR_POINT_SIZE_DECREASE_CMD, this);
        } else if (buttonCode == mappings.getButtonDpadLeft()) {
            em.post(Event.TIME_STATE_CMD, this, false);
        } else if (buttonCode == mappings.getButtonDpadRight()) {
            em.post(Event.TIME_STATE_CMD, this, true);
        } else if (buttonCode == mappings.getButtonStart()) {
            em.post(Event.SHOW_CONTROLLER_GUI_ACTION, this, cam);
        } else if (buttonCode == mappings.getButtonRstick()) {
            if (cam.getMode().isFocus()) {
                // Set free
                em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FREE_MODE);
            } else {
                // Set focus
                em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
            }
        }
        cam.setInputByController(true);

        removePressedKey(buttonCode);

        return true;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (Math.abs(value) > 0.1)
            logger.debug("axis moved [inputListener/code/value]: " + controller.getName() + " / " + axisCode + " / " + value);

        boolean treated = false;

        // Apply power function to axis reading
        double val = Math.signum(value) * Math.pow(Math.abs(value), mappings.getAxisValuePower());

        if (axisCode == mappings.getAxisLstickH()) {
            if (cam.getMode().isFocus()) {
                cam.setRoll(val * 1e-2 * mappings.getAxisLstickHSensitivity());
            } else {
                // Use this for lateral movement
                cam.setHorizontal(val * mappings.getAxisLstickHSensitivity());
            }
            treated = true;
        } else if (axisCode == mappings.getAxisLstickV()) {
            if (Math.abs(val) < 0.005)
                val = 0;
            cam.setVelocity(-val * mappings.getAxisLstickVSensitivity());
            treated = true;
        } else if (axisCode == mappings.getAxisRstickH()) {
            double valr = (Settings.settings.controls.gamepad.invertX ? -1.0 : 1.0) * val * mappings.getAxisRstickVSensitivity();
            if (cam.getMode().isFocus()) {
                cam.setHorizontal(valr * 0.1);
            } else {
                cam.setYaw(valr * 3e-2);
            }
            treated = true;
        } else if (axisCode == mappings.getAxisRstickV()) {
            double valr = (Settings.settings.controls.gamepad.invertY ? 1.0 : -1.0) * val * mappings.getAxisRstickHSensitivity();
            if (cam.getMode().isFocus()) {
                cam.setVertical(valr * 0.1);
            } else {
                cam.setPitch(valr * 3e-2);
            }
            treated = true;
        } else if (axisCode == mappings.getAxisRT()) {
            double valr = val * 1e-1 * mappings.getAxisRTSensitivity();
            cam.setRoll(valr);
            treated = true;
        } else if (axisCode == mappings.getAxisLT()) {
            double valr = val * 1e-1 * mappings.getAxisLTSensitivity();
            cam.setRoll(-valr);
            treated = true;
        }

        if (treated)
            cam.setInputByController(true);

        return treated;
    }


    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.RELOAD_CONTROLLER_MAPPINGS) {
            updateControllerMappings((String) data[0]);
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

    }
}
