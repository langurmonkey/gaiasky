package gaiasky.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.AbstractGamepadMappings;
import gaiasky.gui.GamepadMappings;
import gaiasky.gui.IGamepadMappings;
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

    protected static final long AXIS_EVT_DELAY = 250;
    protected static final long AXIS_POLL_DELAY = 400;

    protected static final long BUTTON_POLL_DELAY = 400;

    protected Controller lastControllerUsed = null;
    protected IGamepadMappings mappings;
    protected final EventManager em;
    protected final IntSet pressedKeys;

    protected long lastAxisEvtTime = 0, lastButtonPollTime = 0;

    public AbstractGamepadListener(String mappingsFile) {
        this(AbstractGamepadMappings.readGamepadMappings(mappingsFile));
    }

    public AbstractGamepadListener(IGamepadMappings mappings) {
        this.mappings = mappings;
        this.em = EventManager.instance;
        this.pressedKeys = new IntSet();
        em.subscribe(this, Event.RELOAD_CONTROLLER_MAPPINGS);
    }

    /** Zero-point function for the axes. **/
    protected double applyZeroPoint(double value) {
        return Math.abs(value) >= mappings.getZeroPoint() ? value : 0;
    }

    public IGamepadMappings getMappings() {
        return mappings;
    }

    public void setMappings(IGamepadMappings mappings) {
        this.mappings = mappings;
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
     *
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
    public boolean buttonDown(Controller controller, int buttonCode) {
        addPressedKey(buttonCode);
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        removePressedKey(buttonCode);
        return false;
    }

    public abstract void pollAxis();
    public abstract void pollButtons();

    @Override
    public void update() {
        long now = TimeUtils.millis();
        // AXIS POLL
        if(now - lastAxisEvtTime > AXIS_POLL_DELAY) {
            pollAxis();
            lastAxisEvtTime = now;
        }

        // BUTTON POLL
        if(now - lastButtonPollTime > BUTTON_POLL_DELAY) {
            pollButtons();
            lastButtonPollTime = now;
        }
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
            mappings = AbstractGamepadMappings.readGamepadMappings((String) data[0]);
        }
    }
}
