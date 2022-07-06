package gaiasky.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.utils.IntSet;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.ControllerMappings;
import gaiasky.gui.IControllerMappings;
import gaiasky.gui.IInputListener;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Contains some utils common to all gamepad listeners.
 */
public abstract class AbstractGamepadListener implements ControllerListener, IInputListener, IObserver {
    private static final Log logger = Logger.getLogger(AbstractGamepadListener.class);

    protected IControllerMappings mappings;
    protected final EventManager em;
    protected final IntSet pressedKeys;

    public AbstractGamepadListener(String mappingsFile) {
        this.em = EventManager.instance;
        this.pressedKeys = new IntSet();
        updateControllerMappings(mappingsFile);

        em.subscribe(this, Event.RELOAD_CONTROLLER_MAPPINGS);
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
    public void update() {
    }

    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.RELOAD_CONTROLLER_MAPPINGS) {
            updateControllerMappings((String) data[0]);
        }
    }
}
