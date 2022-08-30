package gaiasky.input;

import com.badlogic.gdx.Input.Keys;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.view.SpacecraftView;
import gaiasky.scenegraph.camera.SpacecraftCamera;
import gaiasky.util.Settings;

/**
 * Keyboard and mouse input listener for the spacecraft mode.
 */
public class SpacecraftMouseKbdListener extends AbstractMouseKbdListener {

    private final SpacecraftCamera cam;

    public SpacecraftMouseKbdListener(SpacecraftCamera spacecraftCamera, GestureListener listener) {
        super(listener, spacecraftCamera);
        this.cam = spacecraftCamera;
    }

    @Override
    public boolean keyDown(int keycode) {
        SpacecraftView sc = cam.getSpacecraftView();
        if (sc != null && Settings.settings.runtime.inputEnabled) {
            double step = 0.01;
            switch (keycode) {
            case Keys.W:
                // power 1
                sc.setCurrentEnginePower(sc.currentEnginePower() + step);
                EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, false);
                break;
            case Keys.S:
                // power -1
                sc.setCurrentEnginePower(sc.currentEnginePower() - step);
                EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, false);
                break;
            case Keys.A:
                // roll 1
                sc.setRollPower(sc.getRollPower() + step);
                EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, false);
                break;
            case Keys.D:
                // roll -1
                sc.setRollPower(sc.getRollPower() - step);
                EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                break;
            case Keys.DOWN:
                // pitch 1
                sc.setPitchPower(sc.getPitchPower() + step);
                EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                break;
            case Keys.UP:
                // pitch -1
                sc.setPitchPower(sc.getPitchPower() - step);
                EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                break;
            case Keys.LEFT:
                // yaw 1
                sc.setYawPower(sc.getYawPower() + step);
                EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                break;
            case Keys.RIGHT:
                // yaw -1
                sc.setYawPower(sc.getYawPower() - step);
                EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                break;
            default:
                break;
            }
        }
        return false;

    }

    @Override
    public boolean keyUp(int keycode) {
        SpacecraftView sc = cam.getSpacecraftView();
        if (sc != null && sc.getEntity() != null && Settings.settings.runtime.inputEnabled) {
            switch (keycode) {
            case Keys.W:
            case Keys.S:
                // power 0
                sc.setCurrentEnginePower(0);
                break;
            case Keys.D:
            case Keys.A:
                // roll 0
                sc.setRollPower(0);
                break;
            case Keys.UP:
            case Keys.DOWN:
                // pitch 0
                sc.setPitchPower(0);
                break;
            case Keys.RIGHT:
            case Keys.LEFT:
                // yaw 0
                sc.setYawPower(0);
                break;
            case Keys.L:
                // level spaceship
                EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, true);
                break;
            case Keys.K:
                // stop spaceship
                EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, true);
                break;
            case Keys.PAGE_UP:
                // Increase thrust factor
                sc.increaseThrustFactorIndex(true);
                break;
            case Keys.PAGE_DOWN:
                // Decrease thrust length
                sc.decreaseThrustFactorIndex(true);
                break;
            default:
                break;
            }
        }
        return false;

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
