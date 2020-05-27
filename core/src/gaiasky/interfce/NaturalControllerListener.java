/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntSet;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

import java.nio.file.Files;
import java.nio.file.Path;

public class NaturalControllerListener implements ControllerListener, IObserver, IInputListener {
    private static final Log logger = Logger.getLogger(NaturalControllerListener.class);

    NaturalCamera cam;
    IControllerMappings mappings;

    IntSet pressedKeys;

    public NaturalControllerListener(NaturalCamera cam, String mappingsFile) {
        this.cam = cam;
        this.pressedKeys = new IntSet();
        updateControllerMappings(mappingsFile);

        EventManager.instance.subscribe(this, Events.RELOAD_CONTROLLER_MAPPINGS);
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

    public boolean updateControllerMappings(String mappingsFile) {
        // We look for OS-specific mappings for the given inputListener. If not found, it defaults to the base
        String os = SysUtils.getOSFamily();
        int extensionStart = mappingsFile.lastIndexOf('.');
        String pre = mappingsFile.substring(0, extensionStart); //-V6009
        String post = mappingsFile.substring(extensionStart + 1);

        String osMappingsFile = pre + "." + os + "." + post;
        if (Files.exists(Path.of(osMappingsFile))) {
            mappingsFile = osMappingsFile;
            logger.info("Controller mappings file set to " + mappingsFile);
        }

        if (Files.exists(Path.of(mappingsFile)))
            mappings = new ControllerMappings(null, Path.of(mappingsFile));
        else {
            // Defaults to xbox360
            mappings = new XBox360Mappings();
        }
        return false;
    }

    @Override
    public void connected(Controller controller) {
        logger.info("Controller connected: " + controller.getName());
        EventManager.instance.post(Events.CONTROLLER_CONNECTED_INFO, controller.getName());
    }

    @Override
    public void disconnected(Controller controller) {
        logger.info("Controller disconnected: " + controller.getName());
        EventManager.instance.post(Events.CONTROLLER_DISCONNECTED_INFO, controller.getName());
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        if (GlobalConf.controls.DEBUG_MODE) {
            logger.info("button down [inputListener/code]: " + controller.getName() + " / " + buttonCode);
        }

        if (buttonCode == mappings.getButtonX()) {
            cam.setGamepadMultiplier(0.5);
        } else if (buttonCode == mappings.getButtonY()) {
            cam.setGamepadMultiplier(0.1);
        } else if (buttonCode == mappings.getButtonA()) {
            cam.setVelocity(1);
        } else if (buttonCode == mappings.getButtonB()) {
            cam.setVelocity(-1);
        } else if (buttonCode == mappings.getButtonDpadUp()){
            cam.setVertical(1);
        } else if (buttonCode == mappings.getButtonDpadDown()){
            cam.setVertical(-1);
        }
        cam.setInputByController(true);

        addPressedKey(buttonCode);

        return true;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        if (GlobalConf.controls.DEBUG_MODE) {
            logger.info("button up [inputListener/code]: " + controller.getName() + " / " + buttonCode);
        }

        if (buttonCode == mappings.getButtonX()) {
            cam.setGamepadMultiplier(1);
        } else if (buttonCode == mappings.getButtonY()) {
            cam.setGamepadMultiplier(1);
        } else if (buttonCode == mappings.getButtonA()) {
            cam.setVelocity(0);
        } else if (buttonCode == mappings.getButtonB()) {
            cam.setVelocity(0);
        } else if (buttonCode == mappings.getButtonDpadUp()){
            cam.setVertical(0);
        } else if (buttonCode == mappings.getButtonDpadDown()){
            cam.setVertical(0);
        } else if (buttonCode == mappings.getButtonRstick()){
            if(cam.getMode().isFocus()){
                // Set free
                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FREE_MODE);
            } else {
                // Set focus
                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FOCUS_MODE);
            }
        }
        cam.setInputByController(true);

        removePressedKey(buttonCode);

        return true;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (GlobalConf.controls.DEBUG_MODE) {
            if (Math.abs(value) > 0.1)
                logger.info("axis moved [inputListener/code/value]: " + controller.getName() + " / " + axisCode + " / " + value);
        }

        boolean treated = false;

        // y = x^pow
        // http://www.wolframalpha.com/input/?i=y+%3D+sign%28x%29+*+x%5E2+%28x+from+-1+to+1%29}
        double val = Math.signum(value) * Math.abs(Math.pow(value, mappings.getAxisValuePower()));

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
            if (cam.getMode().isFocus()) {
                cam.setHorizontal(val * 0.1 * mappings.getAxisRstickVSensitivity());
            } else {
                cam.setYaw(val * 3e-2 * mappings.getAxisRstickVSensitivity());
            }
            treated = true;
        } else if (axisCode == mappings.getAxisRstickV()) {
            if (cam.getMode().isFocus()) {
                cam.setVertical(val * 0.1 * mappings.getAxisRstickHSensitivity());
            } else {
                cam.setPitch((GlobalConf.controls.INVERT_LOOK_Y_AXIS ? 1.0 : -1.0) * val * 3e-2 * mappings.getAxisRstickHSensitivity());
            }
            treated = true;
        } else if (axisCode == mappings.getAxisRT()) {
            cam.setVelocity((val * mappings.getAxisRTSensitivity() + 1.0) / 2.0);
            treated = true;
        } else if (axisCode == mappings.getAxisLT()) {
            cam.setVelocity(-(val * mappings.getAxisLTSensitivity() + 1.0) / 2.0);
            treated = true;
        }

        if (treated)
            cam.setInputByController(true);

        return treated;
    }

    @Override
    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
        return false;
    }

    @Override
    public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
        return false;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case RELOAD_CONTROLLER_MAPPINGS:
            updateControllerMappings((String) data[0]);
            break;
        default:
            break;
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
