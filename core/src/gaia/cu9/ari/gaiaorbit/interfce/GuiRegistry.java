package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;

/**
 * Manages the Graphical User Interfaces of Gaia Sky
 *
 * @author tsagrista
 */
public class GuiRegistry implements IObserver {

    /** Registered GUI array **/
    private static Array<IGui> guis;

    static {
        guis = new Array<IGui>(true, 2);
    }

    /**
     * Render lock object
     */
    public static Object guirenderlock = new Object();

    /** Current GUI object **/
    public static IGui current;
    /** Previous GUI object, if any **/
    public static IGui previous;

    /** Global input multiplexer **/
    private static InputMultiplexer im = null;

    public static void setInputMultiplexer(InputMultiplexer im) {
        GuiRegistry.im = im;
    }

    /**
     * Switches the current GUI with the given one, updating the processors.
     * It also sets the previous GUI to the given value.
     *
     * @param gui      The new GUI
     * @param previous The new previous GUI
     */
    public static void change(IGui gui, IGui previous) {
        if (current != gui) {
            unset(previous);
            set(gui);
        }
    }

    /**
     * Switches the current GUI with the given one, updating the processors
     *
     * @param gui The new gui
     */
    public static void change(IGui gui) {
        if (current != gui) {
            unset();
            set(gui);
        }
    }

    /**
     * Unsets the current GUI and sets it as previous
     */
    public static void unset() {
        unset(current);
    }

    /**
     * Unsets the given GUI and sets it as previous
     *
     * @param gui The GUI
     */
    public static void unset(IGui gui) {
        if (gui != null) {
            unregisterGui(gui);
            im.removeProcessor(gui.getGuiStage());
        }
        previous = gui;
    }

    /**
     * Sets the given GUI as current
     *
     * @param gui The new GUI
     */
    public static void set(IGui gui) {
        if (gui != null) {
            registerGui(gui);
            im.addProcessor(0, gui.getGuiStage());
        }
        current = gui;
    }

    /**
     * Sets the given GUI as previous
     *
     * @param gui The new previous GUI
     */
    public static void setPrevious(IGui gui) {
        previous = gui;
    }

    /**
     * Registers a new GUI
     *
     * @param gui The GUI to register
     */
    public static void registerGui(IGui gui) {
        if (!guis.contains(gui, true))
            guis.add(gui);
    }

    /**
     * Unregisters a GUI
     *
     * @param gui The GUI to unregister
     * @return True if the GUI was unregistered
     */
    public static boolean unregisterGui(IGui gui) {
        return guis.removeValue(gui, true);
    }

    /**
     * Unregisters all GUIs
     *
     * @return True if operation succeeded
     */
    public static boolean unregisterAll() {
        guis.clear();
        return true;
    }

    /**
     * Renders the registered GUIs
     *
     * @param rw The render width
     * @param rh The render height
     */
    public static void render(int rw, int rh) {
        synchronized (guirenderlock) {
            for (int i = 0; i < guis.size; i++) {
                guis.get(i).getGuiStage().getViewport().apply();
                guis.get(i).render(rw, rh);
            }
        }
    }

    /**
     * Updates the registered GUIs
     *
     * @param dt The delta time in seconds
     */
    public static void update(double dt) {
        for (IGui gui : guis)
            gui.update(dt);
    }

    private Skin skin;

    /** Keyframes window **/
    private KeyframesWindow keyframesWindow;

    /**
     * One object to handle observer pattern
     */
    public GuiRegistry(Skin skin) {
        super();
        this.skin = skin;
        // Windows which are visible from any GUI
        EventManager.instance.subscribe(this, Events.SHOW_QUIT_ACTION, Events.SHOW_ABOUT_ACTION, Events.SHOW_PREFERENCES_ACTION, Events.SHOW_KEYFRAMES_WINDOW_ACTION);
    }

    public void dispose(){
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(Events event, Object... data) {
        if (current != null) {
            Stage ui = current.getGuiStage();
            // Treats windows that can appear in any GUI
            switch (event) {
            case SHOW_QUIT_ACTION:
                (new QuitWindow(ui, skin)).show(ui);
                break;
            case SHOW_ABOUT_ACTION:
                (new AboutWindow(ui, skin)).show(ui);
                break;
            case SHOW_PREFERENCES_ACTION:
                (new PreferencesWindow(ui, skin)).show(ui);
                break;
            case SHOW_KEYFRAMES_WINDOW_ACTION:
                if (keyframesWindow == null)
                    keyframesWindow = new KeyframesWindow(ui, skin);
                if (!keyframesWindow.isVisible() || !keyframesWindow.hasParent())
                    keyframesWindow.show(ui);
                break;
            default:
                break;
            }
        }
    }

}
