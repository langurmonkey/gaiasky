/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects.CubemapProjections;

import java.time.Instant;
import java.util.*;

/**
 * Contains the key mappings and the actions. This should be persisted somehow
 * in the future.
 *
 * @author Toni Sagrista
 */
public class KeyBindings {
    private Map<TreeSet<Integer>, ProgramAction> mappings;
    private Map<ProgramAction, Array<TreeSet<Integer>>> mappingsInv;

    public static KeyBindings instance;

    public static void initialize() {
        if (instance == null) {
            instance = new KeyBindings();
        }
    }

    /** CONTROL **/
    public static int CTRL_L;
    /** SHIFT **/
    public static int SHIFT_L;
    /** ALT **/
    public static int ALT_L;

    /**
     * Creates a key mappings instance.
     */
    private KeyBindings() {
        mappings = new HashMap<>();
        mappingsInv = new TreeMap<>();
        // Init special keys
        CTRL_L = Keys.CONTROL_LEFT;
        SHIFT_L = Keys.SHIFT_LEFT;
        ALT_L = Keys.ALT_LEFT;
        // For now this will do
        initDefault();
    }

    public Map<TreeSet<Integer>, ProgramAction> getMappings() {
        return mappings;
    }

    public Map<ProgramAction, Array<TreeSet<Integer>>> getMappingsInv(){
        return mappingsInv;
    }

    public Map<TreeSet<Integer>, ProgramAction> getSortedMappings() {
        return GlobalResources.sortByValue(mappings);
    }

    public Map<ProgramAction, Array<TreeSet<Integer>>> getSortedMappingsInv(){
        return mappingsInv;
    }

    private void addMapping(ProgramAction action, int... keyCodes) {
        TreeSet<Integer> keys = new TreeSet<>();
        for (int key : keyCodes) {
            keys.add(key);
        }
        mappings.put(keys, action);

        if(mappingsInv.containsKey(action)){
            mappingsInv.get(action).add(keys);
        } else {
            Array<TreeSet<Integer>> a = new Array<>();
            a.add(keys);
            mappingsInv.put(action, a);
        }
    }

    /**
     * Finds an action given its name
     * @param name The name
     * @return The action if it exists
     */
    public ProgramAction findAction(String name){
        Set<ProgramAction> actions = mappingsInv.keySet();
        for(ProgramAction action : actions){
            if(action.actionName.equals(name))
                return action;
        }
        return null;
    }

    /**
     * Gets the keys that trigger the action identified by the given name
     * @param actionName The action name
     * @return The keys
     */
    public TreeSet<Integer> getKeys(String actionName){
        ProgramAction action = findAction(actionName);
        if(action != null){
            Set<Map.Entry<TreeSet<Integer>, ProgramAction>> entries = mappings.entrySet();
            for(Map.Entry<TreeSet<Integer>, ProgramAction> entry : entries){
                if(entry.getValue().equals(action)){
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public String getStringKeys(String actionName){
        TreeSet<Integer> keys = getKeys(actionName);
        StringBuilder sb = new StringBuilder();
        Iterator<Integer> it = keys.descendingIterator();
        while(it.hasNext()){
            sb.append(Keys.toString(it.next()));
            if(it.hasNext())
                sb.append("+");
        }
        return sb.toString();
    }

    /**
     * Initializes the default keyboard mappings. In the future these should be
     * read from a configuration file.
     */
    private void initDefault() {

        // Condition which checks the current GUI is the FullGui
        BooleanRunnable fullGuiCondition = () -> GuiRegistry.current instanceof FullGui;

        // Show about
        final Runnable runnableAbout = () -> EventManager.instance.post(Events.SHOW_ABOUT_ACTION);

        // F1 -> Help dialog
        addMapping(new ProgramAction("action.help", runnableAbout), Keys.F1);

        // h -> Help dialog
        addMapping(new ProgramAction("action.help", runnableAbout), Keys.H);

        // Show quit
        final Runnable runnableQuit = () -> {
            // Quit action
            EventManager.instance.post(Events.QUIT_ACTION);
        };

        // ESCAPE -> Run quit action
        addMapping(new ProgramAction("action.exit", runnableQuit), Keys.ESCAPE);

        // CTRL+Q -> Exit
        //addMapping(new ProgramAction("action.exit", runnableQuit), CTRL_L, Keys.Q);

        // P -> Show preferences dialog
        addMapping(new ProgramAction("action.preferences", () ->
                EventManager.instance.post(Events.SHOW_PREFERENCES_ACTION)), Keys.P);

        // c -> Show play camera dialog
        //addMapping(new ProgramAction("action.playcamera", () ->
        //        EventManager.instance.post(Events.SHOW_PLAYCAMERA_ACTION), fullGuiCondition), Keys.C);

        // SHIFT+O -> Toggle orbits
        addMapping(new ProgramAction("action.toggle/element.orbits", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.orbits", false)), SHIFT_L, Keys.O);

        // SHIFT+P -> Toggle planets
        addMapping(new ProgramAction("action.toggle/element.planets", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.planets", false)), SHIFT_L, Keys.P);

        // SHIFT+M -> Toggle moons
        addMapping(new ProgramAction("action.toggle/element.moons", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.moons", false)), SHIFT_L, Keys.M);

        // SHIFT+S -> Toggle stars
        addMapping(new ProgramAction("action.toggle/element.stars", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.stars", false)), SHIFT_L, Keys.S);

        // SHIFT+T -> Toggle satellites
        addMapping(new ProgramAction("action.toggle/element.satellites", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.satellites", false)), SHIFT_L, Keys.T);

        // SHIFT+A -> Toggle asteroids
        addMapping(new ProgramAction("action.toggle/element.asteroids", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.asteroids", false)), SHIFT_L, Keys.A);

        // SHIFT+L -> Toggle labels
        addMapping(new ProgramAction("action.toggle/element.labels", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.labels", false)), SHIFT_L, Keys.L);

        // SHIFT+C -> Toggle constellations
        addMapping(new ProgramAction("action.toggle/element.constellations", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.constellations", false)), SHIFT_L, Keys.C);

        // SHIFT+B -> Toggle boundaries
        addMapping(new ProgramAction("action.toggle/element.boundaries", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.boundaries", false)), SHIFT_L, Keys.B);

        // SHIFT+Q -> Toggle equatorial
        addMapping(new ProgramAction("action.toggle/element.equatorial", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.equatorial", false)), SHIFT_L, Keys.Q);

        // SHIFT+E -> Toggle ecliptic
        addMapping(new ProgramAction("action.toggle/element.ecliptic", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.ecliptic", false)), SHIFT_L, Keys.E);

        // SHIFT+G -> Toggle galactic
        addMapping(new ProgramAction("action.toggle/element.galactic", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.galactic", false)), SHIFT_L, Keys.G);

        // SHIFT+H -> Toggle meshes
        addMapping(new ProgramAction("action.toggle/element.meshes", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.meshes", false)), SHIFT_L, Keys.H);

        // SHIFT+V -> Toggle clusters
        addMapping(new ProgramAction("action.toggle/element.clusters", () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.clusters", false)), SHIFT_L, Keys.V);

        // Left bracket -> divide speed
        addMapping(new ProgramAction("action.dividetime", () ->
                EventManager.instance.post(Events.TIME_WARP_DECREASE_CMD)), Keys.COMMA);

        // Right bracket -> double speed
        addMapping(new ProgramAction("action.doubletime", () ->
                EventManager.instance.post(Events.TIME_WARP_INCREASE_CMD)), Keys.PERIOD);

        // SPACE -> toggle time
        addMapping(new ProgramAction("action.pauseresume", () -> {
                // Game mode has space bound to 'up'
                if(!GaiaSky.instance.cam.mode.isGame())
                    EventManager.instance.post(Events.TOGGLE_TIME_CMD, null, false);
        }), Keys.SPACE);

        // Plus -> increase limit magnitude
        addMapping(new ProgramAction("action.incmag", () ->
                EventManager.instance.post(Events.LIMIT_MAG_CMD, GlobalConf.runtime.LIMIT_MAG_RUNTIME + 0.1f)), Keys.PLUS);

        // Minus -> decrease limit magnitude
        addMapping(new ProgramAction("action.decmag", () ->
                EventManager.instance.post(Events.LIMIT_MAG_CMD, GlobalConf.runtime.LIMIT_MAG_RUNTIME - 0.1f)), Keys.MINUS);

        // Star -> reset limit mag
        addMapping(new ProgramAction("action.resetmag", () ->
                EventManager.instance.post(Events.LIMIT_MAG_CMD, GlobalConf.data.LIMIT_MAG_LOAD)), Keys.STAR);

        // F11 -> fullscreen
        addMapping(new ProgramAction("action.togglefs", () -> {
            GlobalConf.screen.FULLSCREEN = !GlobalConf.screen.FULLSCREEN;
            EventManager.instance.post(Events.SCREEN_MODE_CMD);
        }), Keys.F11);

        // F4 -> toggle fisheye effect
        addMapping(new ProgramAction("action.fisheye", () ->
                EventManager.instance.post(Events.FISHEYE_CMD, !GlobalConf.postprocess.POSTPROCESS_FISHEYE)), Keys.F4);

        // F5 -> take screenshot
        addMapping(new ProgramAction("action.screenshot", () ->
                EventManager.instance.post(Events.SCREENSHOT_CMD, GlobalConf.screenshot.SCREENSHOT_WIDTH, GlobalConf.screenshot.SCREENSHOT_HEIGHT, GlobalConf.screenshot.SCREENSHOT_FOLDER)), Keys.F5);

        // F6 -> toggle frame output
        addMapping(new ProgramAction("action.toggle/element.frameoutput", () ->
                EventManager.instance.post(Events.FRAME_OUTPUT_CMD, !GlobalConf.frame.RENDER_OUTPUT)), Keys.F6);

        // U -> toggle UI collapse/expand
        addMapping(new ProgramAction("action.toggle/element.controls", () ->
                EventManager.instance.post(Events.GUI_FOLD_CMD), fullGuiCondition), Keys.U);

        // CTRL+K -> toggle cubemap mode
        addMapping(new ProgramAction("action.toggle/element.360", () ->
                EventManager.instance.post(Events.CUBEMAP360_CMD, !GlobalConf.program.CUBEMAP360_MODE, false)), CTRL_L, Keys.K);

        // CTRL+SHIFT+K -> toggle cubemap projection
        addMapping(new ProgramAction("action.toggle/element.projection", () -> {
            int newprojidx = (GlobalConf.program.CUBEMAP_PROJECTION.ordinal() + 1) % CubemapProjections.CubemapProjection.values().length;
            EventManager.instance.post(Events.CUBEMAP_PROJECTION_CMD, CubemapProjections.CubemapProjection.values()[newprojidx]);
        }), CTRL_L, SHIFT_L, Keys.K);

        // CTRL + SHIFT + UP -> increase star point size by 0.5
        addMapping(new ProgramAction("action.starpointsize.inc", () ->
                EventManager.instance.post(Events.STAR_POINT_SIZE_INCREASE_CMD)), CTRL_L, SHIFT_L, Keys.UP);

        // CTRL + SHIFT + DOWN -> decrease star point size by 0.5
        addMapping(new ProgramAction("action.starpointsize.dec", () ->
                EventManager.instance.post(Events.STAR_POINT_SIZE_DECREASE_CMD)), CTRL_L, SHIFT_L, Keys.DOWN);

        // CTRL + SHIFT + R -> reset star point size
        addMapping(new ProgramAction("action.starpointsize.reset", () ->
                EventManager.instance.post(Events.STAR_POINT_SIZE_RESET_CMD)), CTRL_L, SHIFT_L, Keys.R);

        // CTRL + W -> new keyframe
        addMapping(new ProgramAction("action.keyframe", () ->
                EventManager.instance.post(Events.KEYFRAME_ADD)), CTRL_L, Keys.W);

        // Camera modes (NUMBERS)
        for (int i = 7; i <= 16; i++) {
            // Camera mode
            int m = i - 7;
            final CameraMode mode = CameraMode.getMode(m);
            if (mode != null) {
                addMapping(new ProgramAction(mode.toString(), () ->
                        EventManager.instance.post(Events.CAMERA_MODE_CMD, mode)), i);
            }
        }

        // Camera modes (NUM_KEYPAD)
        for (int i = 144; i <= 153; i++) {
            // Camera mode
            int m = i - 144;
            final CameraMode mode = CameraMode.getMode(m);
            if (mode != null) {
                addMapping(new ProgramAction(mode.toString(), () ->
                        EventManager.instance.post(Events.CAMERA_MODE_CMD, mode)), i);
            }
        }

        // CTRL + D -> Toggle debug information
        addMapping(new ProgramAction("action.toggle/element.debugmode", () -> {
            EventManager.instance.post(Events.SHOW_DEBUG_CMD);
        }), CTRL_L, Keys.D);

        // CTRL + F -> Search dialog
        final Runnable runnableSearch = () ->
            EventManager.instance.post(Events.SHOW_SEARCH_ACTION);
        addMapping(new ProgramAction("action.search", runnableSearch, fullGuiCondition), CTRL_L, Keys.F);

        // f -> Search dialog
        addMapping(new ProgramAction("action.search", runnableSearch, fullGuiCondition), Keys.F);

        // / -> Search dialog
        addMapping(new ProgramAction("action.search", runnableSearch, fullGuiCondition), Keys.SLASH);

        // CTRL + SHIFT + O -> Toggle particle fade
        addMapping(new ProgramAction("action.toggle/element.octreeparticlefade", () ->
                EventManager.instance.post(Events.OCTREE_PARTICLE_FADE_CMD, I18n.txt("element.octreeparticlefade"), !GlobalConf.scene.OCTREE_PARTICLE_FADE)), CTRL_L, SHIFT_L, Keys.O);

        // CTRL + S -> Toggle stereoscopic mode
        addMapping(new ProgramAction("action.toggle/element.stereomode", () ->
                EventManager.instance.post(Events.STEREOSCOPIC_CMD, !GlobalConf.program.STEREOSCOPIC_MODE, false)), CTRL_L, Keys.S);

        // CTRL + SHIFT + S -> Switch stereoscopic profile
        addMapping(new ProgramAction("action.switchstereoprofile", () -> {
            int newidx = GlobalConf.program.STEREO_PROFILE.ordinal();
            newidx = (newidx + 1) % StereoProfile.values().length;
            EventManager.instance.post(Events.STEREO_PROFILE_CMD, newidx);
        }), CTRL_L, SHIFT_L, Keys.S);

        // CTRL + P -> Toggle planetarium mode
        addMapping(new ProgramAction("action.toggle/element.planetarium", () ->
                EventManager.instance.post(Events.PLANETARIUM_CMD, !GlobalConf.postprocess.POSTPROCESS_FISHEYE, false)), CTRL_L, Keys.P);

        // CTRL + U -> Toggle clean (no GUI) mode
        addMapping(new ProgramAction("action.toggle/element.cleanmode", () ->
                EventManager.instance.post(Events.DISPLAY_GUI_CMD, I18n.txt("notif.cleanmode"))), CTRL_L, Keys.U);

        // CTRL + G -> Travel to focus object
        addMapping(new ProgramAction("action.gotoobject", () ->
                EventManager.instance.post(Events.GO_TO_OBJECT_CMD)), CTRL_L, Keys.G);

        // CTRL + R -> Reset time to current system time
        addMapping(new ProgramAction("action.resettime", () ->
                EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.now())), CTRL_L, Keys.R);

        // CTRL + SHIFT + G -> Galaxy 2D - 3D
        addMapping(new ProgramAction("action.toggle-element.galaxy3d", () ->
                EventManager.instance.post(Events.GALAXY_3D_CMD, !GlobalConf.scene.GALAXY_3D)), CTRL_L, SHIFT_L, Keys.G);

        // HOME -> Back home
        addMapping(new ProgramAction("action.home", () -> {
            EventManager.instance.post(Events.HOME_CMD);
        }), Keys.HOME);

        // TAB -> Minimap toggle
        addMapping(new ProgramAction("action.toggle/gui.minimap.title", () ->
                EventManager.instance.post(Events.TOGGLE_MINIMAP)), Keys.TAB);


        // ALT_L + T -> Expand/collapse time pane
        addMapping(new ProgramAction("action.expandcollapse.pane/gui.time", () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "TimeComponent")), ALT_L, Keys.T);

        // ALT_L + C -> Expand/collapse camera pane
        addMapping(new ProgramAction("action.expandcollapse.pane/gui.camera", () ->
            EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "CameraComponent")), ALT_L, Keys.C);

        // ALT_L + V -> Expand/collapse visibility pane
        addMapping(new ProgramAction("action.expandcollapse.pane/gui.visibility", () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "VisibilityComponent")), ALT_L, Keys.V);

        // ALT_L + L -> Expand/collapse visual effects pane
        addMapping(new ProgramAction("action.expandcollapse.pane/gui.lighting", () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "VisualEffectsComponent")), ALT_L, Keys.L);

        // ALT_L + D -> Expand/collapse datasets pane
        addMapping(new ProgramAction("action.expandcollapse.pane/gui.dataset.title", () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "DatasetsComponent")), ALT_L, Keys.D);

        // ALT_L + O -> Expand/collapse objects pane
        addMapping(new ProgramAction("action.expandcollapse.pane/gui.objects", () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "ObjectsComponent")), ALT_L, Keys.O);

        // ALT_L + M -> Expand/collapse music pane
        addMapping(new ProgramAction("action.expandcollapse.pane/gui.music", () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "MusicComponent")), ALT_L, Keys.M);

        // CTRL+SHIFT+L -> Toggle mouse capture
        addMapping(new ProgramAction("action.toggle/gui.mousecapture", () ->
                EventManager.instance.post(Events.MOUSE_CAPTURE_TOGGLE)), CTRL_L, SHIFT_L, Keys.L);

    }

    /**
     * A simple program action. It can optionally contain a condition which must
     * evaluate to true for the action to be run.
     *
     * @author Toni Sagrista
     */
    public class ProgramAction implements Runnable, Comparable<ProgramAction> {
        final String actionId;
        final String actionName;
        /**
         * Action to run
         **/
        private final Runnable action;

        /**
         * Condition that must be met
         **/
        private final BooleanRunnable condition;

        ProgramAction(String actionId, Runnable action, BooleanRunnable condition) {
            this.actionId = actionId;
            // Set action name
            String actionName;
            try {
                if (actionId.contains("/")) {
                    String[] actions = actionId.split("/");
                    actionName = I18n.txt(actions[0], I18n.txt(actions[1]));
                } else {
                    actionName = I18n.txt(actionId);
                }
            }catch(MissingResourceException e){
                actionName = actionId;
            }
            this.actionName = actionName;
            this.action = action;
            this.condition = condition;
        }

        ProgramAction(String actionName, Runnable action) {
            this(actionName, action, null);
        }

        @Override
        public void run() {
            // Run if condition not set or condition is met
            if (condition != null) {
                if (condition.run())
                    action.run();
            } else {
                action.run();
            }
        }

        @Override
        public int compareTo(ProgramAction other) {
            return actionName.toLowerCase().compareTo(other.actionName.toLowerCase());
        }

    }

    public interface BooleanRunnable {
        boolean run();
    }

}
