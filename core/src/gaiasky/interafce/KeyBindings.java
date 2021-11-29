/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections.CubemapProjection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;

import static gaiasky.util.Settings.StereoProfile.values;

/**
 * Contains the key mappings and the actions. This should be persisted somehow
 * in the future.
 */
public class KeyBindings {
    private static final Log logger = Logger.getLogger(KeyBindings.class);

    private final Map<String, ProgramAction> actions;
    private final Map<TreeSet<Integer>, ProgramAction> mappings;
    private final Map<ProgramAction, Array<TreeSet<Integer>>> mappingsInv;

    public static KeyBindings instance;

    public static void initialize() {
        if (instance == null) {
            instance = new KeyBindings();
        }
    }

    // Special keys
    public static final int CTRL_L = Keys.CONTROL_LEFT;
    public static final int SHIFT_L = Keys.SHIFT_LEFT;
    public static final int ALT_L = Keys.ALT_LEFT;
    public static final int[] SPECIAL = new int[] { CTRL_L, SHIFT_L, ALT_L };

    /**
     * Creates a key mappings instance.
     */
    private KeyBindings() {
        actions = new HashMap<>();
        mappings = new HashMap<>();
        mappingsInv = new TreeMap<>();
        initDefault();
    }

    public Map<TreeSet<Integer>, ProgramAction> getMappings() {
        return mappings;
    }

    public Map<ProgramAction, Array<TreeSet<Integer>>> getMappingsInv() {
        return mappingsInv;
    }

    public Map<TreeSet<Integer>, ProgramAction> getSortedMappings() {
        return GlobalResources.sortByValue(mappings);
    }

    public Map<ProgramAction, Array<TreeSet<Integer>>> getSortedMappingsInv() {
        return mappingsInv;
    }

    private void addMapping(ProgramAction action, int... keyCodes) {
        TreeSet<Integer> keys = new TreeSet<>();
        for (int key : keyCodes) {
            keys.add(key);
        }
        mappings.put(keys, action);

        if (mappingsInv.containsKey(action)) {
            mappingsInv.get(action).add(keys);
        } else {
            Array<TreeSet<Integer>> a = new Array<>();
            a.add(keys);
            mappingsInv.put(action, a);
        }
    }

    private void addAction(ProgramAction action) {
        actions.put(action.actionId, action);
    }

    /**
     * Finds an action given its name
     *
     * @param name The name
     * @return The action if it exists
     */
    public ProgramAction findAction(String name) {
        Set<ProgramAction> actions = mappingsInv.keySet();
        for (ProgramAction action : actions) {
            if (action.actionId.equals(name))
                return action;
        }
        return null;
    }

    /**
     * Gets the keys that trigger the action identified by the given name
     *
     * @param actionId The action ID
     * @return The keys
     */
    public TreeSet<Integer> getKeys(String actionId) {
        ProgramAction action = findAction(actionId);
        if (action != null) {
            Set<Map.Entry<TreeSet<Integer>, ProgramAction>> entries = mappings.entrySet();
            for (Map.Entry<TreeSet<Integer>, ProgramAction> entry : entries) {
                if (entry.getValue().equals(action)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    public String getStringKeys(String actionId) {
        return getStringKeys(actionId, "+");
    }

    public String getStringKeys(String actionId, String join) {
        TreeSet<Integer> keys = getKeys(actionId);
        if (keys != null) {
            StringBuilder sb = new StringBuilder();
            Iterator<Integer> it = keys.descendingIterator();
            while (it.hasNext()) {
                sb.append(Keys.toString(it.next()));
                if (it.hasNext())
                    sb.append(join);
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    public String[] getStringArrayKeys(String actionId) {
        TreeSet<Integer> keys = getKeys(actionId);
        if (keys != null) {
            String[] result = new String[keys.size()];
            Iterator<Integer> it = keys.descendingIterator();
            int i = 0;
            while (it.hasNext()) {
                result[i++] = Keys.toString(it.next());
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Initializes the default keyboard mappings. In the future these should be
     * read from a configuration file.
     */
    private void initDefault() {
        initActions();
        initMappings();
    }

    private void initActions() {
        /*
         * INITIALISE ACTIONS
         */

        // Condition which checks the current GUI is the FullGui
        BooleanRunnable fullGuiCondition = () -> GaiaSky.instance.getGuiRegistry().current instanceof FullGui;
        // Condition that checks the current camera is not Game
        BooleanRunnable noGameCondition = () -> !GaiaSky.instance.getCameraManager().getMode().isGame();
        // Condition that checks the GUI is visible (no clean mode)
        BooleanRunnable noCleanMode = () -> Settings.settings.runtime.displayGui || GaiaSky.instance.externalView;
        // Condition that checks that panorama mode is off
        BooleanRunnable noPanorama = () -> !(Settings.settings.program.modeCubemap.active && Settings.settings.program.modeCubemap.projection.isPanorama());
        // Condition that checks that planetarium mode is off
        BooleanRunnable noPlanetarium = () -> !(Settings.settings.program.modeCubemap.active && Settings.settings.program.modeCubemap.projection.isPlanetarium());
        // Condition that checks that we are not a slave with a special projection
        BooleanRunnable noSlaveProj = () -> !SlaveManager.projectionActive();
        // Condition that checks that we are a master and have slaves
        BooleanRunnable masterWithSlaves = MasterManager::hasSlaves;

        // about action
        final Runnable runnableAbout = () -> EventManager.instance.post(Events.SHOW_ABOUT_ACTION);

        // help dialog
        addAction(new ProgramAction("action.help", runnableAbout, noCleanMode));

        // help dialog
        addAction(new ProgramAction("action.help", runnableAbout, noCleanMode));

        // show quit
        final Runnable runnableQuit = () -> {
            // Quit action
            EventManager.instance.post(Events.SHOW_QUIT_ACTION);
        };

        // run quit action
        addAction(new ProgramAction("action.exit", runnableQuit, noCleanMode));

        // exit
        //addAction(new ProgramAction("action.exit", runnableQuit), CTRL_L, Keys.Q);

        // show preferences dialog
        addAction(new ProgramAction("action.preferences", () -> EventManager.instance.post(Events.SHOW_PREFERENCES_ACTION), noCleanMode));

        // minimap toggle
        addAction(new ProgramAction("action.toggle/gui.minimap.title", () -> EventManager.instance.post(Events.TOGGLE_MINIMAP), noCleanMode));

        // load catalog
        addAction(new ProgramAction("action.loadcatalog", () -> EventManager.instance.post(Events.SHOW_LOAD_CATALOG_ACTION), noCleanMode));

        // show log dialog
        addAction(new ProgramAction("action.log", () -> EventManager.instance.post(Events.SHOW_LOG_ACTION), noCleanMode));

        // show play camera dialog
        //addAction(new ProgramAction("action.playcamera", () ->
        //        EventManager.instance.post(Events.SHOW_PLAYCAMERA_ACTION), fullGuiCondition), Keys.C);

        // Toggle orbits
        addAction(new ProgramAction("action.toggle/element.orbits", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.orbits", false)));

        // Toggle planets
        addAction(new ProgramAction("action.toggle/element.planets", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.planets", false)));

        // Toggle moons
        addAction(new ProgramAction("action.toggle/element.moons", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.moons", false)));

        // Toggle stars
        addAction(new ProgramAction("action.toggle/element.stars", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.stars", false), noGameCondition));

        // Toggle satellites
        addAction(new ProgramAction("action.toggle/element.satellites", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.satellites", false)));

        // Toggle asteroids
        addAction(new ProgramAction("action.toggle/element.asteroids", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.asteroids", false), noGameCondition));

        // Toggle labels
        addAction(new ProgramAction("action.toggle/element.labels", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.labels", false)));

        // Toggle constellations
        addAction(new ProgramAction("action.toggle/element.constellations", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.constellations", false), noGameCondition));

        // Toggle boundaries
        addAction(new ProgramAction("action.toggle/element.boundaries", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.boundaries", false)));

        // Toggle equatorial
        addAction(new ProgramAction("action.toggle/element.equatorial", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.equatorial", false)));

        // Toggle ecliptic
        addAction(new ProgramAction("action.toggle/element.ecliptic", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.ecliptic", false)));

        // Toggle galactic
        addAction(new ProgramAction("action.toggle/element.galactic", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.galactic", false)));

        // Toggle recgrid
        addAction(new ProgramAction("action.toggle/element.recursivegrid", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.recursivegrid", false)));

        // toggle meshes
        addAction(new ProgramAction("action.toggle/element.meshes", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.meshes", false)));

        // toggle clusters
        addAction(new ProgramAction("action.toggle/element.clusters", () -> EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.clusters", false)));

        // divide speed
        addAction(new ProgramAction("action.dividetime", () -> EventManager.instance.post(Events.TIME_WARP_DECREASE_CMD)));

        // double speed
        addAction(new ProgramAction("action.doubletime", () -> EventManager.instance.post(Events.TIME_WARP_INCREASE_CMD)));

        // toggle time
        addAction(new ProgramAction("action.pauseresume", () -> {
            // Game mode has space bound to 'up'
            if (!GaiaSky.instance.cameraManager.mode.isGame())
                EventManager.instance.post(Events.TIME_STATE_CMD, !Settings.settings.runtime.timeOn, false);
        }));

        // increase field of view
        addAction(new ProgramAction("action.incfov", () -> EventManager.instance.post(Events.FOV_CHANGED_CMD, Settings.settings.scene.camera.fov + 1f, false), noSlaveProj));

        // decrease field of view
        addAction(new ProgramAction("action.decfov", () -> EventManager.instance.post(Events.FOV_CHANGED_CMD, Settings.settings.scene.camera.fov - 1f, false), noSlaveProj));

        // fullscreen
        addAction(new ProgramAction("action.togglefs", () -> {
            Settings.settings.graphics.fullScreen.active = !Settings.settings.graphics.fullScreen.active;
            EventManager.instance.post(Events.SCREEN_MODE_CMD);
        }));

        // take screenshot
        addAction(new ProgramAction("action.screenshot", () -> EventManager.instance.post(Events.SCREENSHOT_CMD, Settings.settings.screenshot.resolution[0], Settings.settings.screenshot.resolution[1], Settings.settings.screenshot.location)));

        // toggle frame output
        addAction(new ProgramAction("action.toggle/element.frameoutput", () -> EventManager.instance.post(Events.FRAME_OUTPUT_CMD, !Settings.settings.frame.active)));

        // toggle UI collapse/expand
        addAction(new ProgramAction("action.toggle/element.controls", () -> EventManager.instance.post(Events.GUI_FOLD_CMD), fullGuiCondition, noCleanMode));

        // toggle planetarium mode
        addAction(new ProgramAction("action.toggle/element.planetarium", () -> {
            boolean enable = !Settings.settings.program.modeCubemap.active;
            EventManager.instance.post(Events.CUBEMAP_CMD, enable, CubemapProjection.FISHEYE, false);
        }, noPanorama));

        // toggle cubemap mode
        addAction(new ProgramAction("action.toggle/element.360", () -> {
            boolean enable = !Settings.settings.program.modeCubemap.active;
            EventManager.instance.post(Events.CUBEMAP_CMD, enable, CubemapProjection.EQUIRECTANGULAR, false);
        }, noPlanetarium));

        // toggle cubemap projection
        addAction(new ProgramAction("action.toggle/element.projection", () -> {
            int newprojidx = (Settings.settings.program.modeCubemap.projection.ordinal() + 1) % (CubemapProjection.HAMMER.ordinal() + 1);
            EventManager.instance.post(Events.CUBEMAP_PROJECTION_CMD, CubemapProjection.values()[newprojidx]);
        }));

        // increase star point size by 0.5
        addAction(new ProgramAction("action.starpointsize.inc", () -> EventManager.instance.post(Events.STAR_POINT_SIZE_INCREASE_CMD)));

        // decrease star point size by 0.5
        addAction(new ProgramAction("action.starpointsize.dec", () -> EventManager.instance.post(Events.STAR_POINT_SIZE_DECREASE_CMD)));

        // reset star point size
        addAction(new ProgramAction("action.starpointsize.reset", () -> EventManager.instance.post(Events.STAR_POINT_SIZE_RESET_CMD)));

        // new keyframe
        addAction(new ProgramAction("action.keyframe", () -> EventManager.instance.post(Events.KEYFRAME_ADD)));

        // toggle debug information
        addAction(new ProgramAction("action.toggle/element.debugmode", () -> EventManager.instance.post(Events.SHOW_DEBUG_CMD), noCleanMode));

        // search dialog
        final Runnable runnableSearch = () -> EventManager.instance.post(Events.SHOW_SEARCH_ACTION);
        addAction(new ProgramAction("action.search", runnableSearch, fullGuiCondition, noCleanMode));

        // search dialog
        addAction(new ProgramAction("action.search", runnableSearch, fullGuiCondition, noCleanMode));

        // search dialog
        addAction(new ProgramAction("action.search", runnableSearch, fullGuiCondition, noCleanMode));

        // toggle particle fade
        addAction(new ProgramAction("action.toggle/element.octreeparticlefade", () -> EventManager.instance.post(Events.OCTREE_PARTICLE_FADE_CMD, I18n.txt("element.octreeparticlefade"), !Settings.settings.scene.octree.fade)));

        // toggle stereoscopic mode
        addAction(new ProgramAction("action.toggle/element.stereomode", () -> EventManager.instance.post(Events.STEREOSCOPIC_CMD, !Settings.settings.program.modeStereo.active, false)));

        // switch stereoscopic profile
        addAction(new ProgramAction("action.switchstereoprofile", () -> {
            int newidx = Settings.settings.program.modeStereo.profile.ordinal();
            newidx = (newidx + 1) % values().length;
            EventManager.instance.post(Events.STEREO_PROFILE_CMD, newidx);
        }));

        // Toggle clean (no GUI) mode
        addAction(new ProgramAction("action.toggle/element.cleanmode", () -> EventManager.instance.post(Events.DISPLAY_GUI_CMD, !Settings.settings.runtime.displayGui, I18n.txt("notif.cleanmode"))));

        // Travel to focus object
        addAction(new ProgramAction("action.gotoobject", () -> EventManager.instance.post(Events.GO_TO_OBJECT_CMD)));

        // Reset time to current system time
        addAction(new ProgramAction("action.resettime", () -> EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.now())));

        // Back home
        addAction(new ProgramAction("action.home", () -> EventManager.instance.post(Events.HOME_CMD)));

        // Expand/collapse time pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.time", () -> EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "TimeComponent"), noCleanMode));

        // Expand/collapse camera pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.camera", () -> EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "CameraComponent"), noCleanMode));

        // Expand/collapse visibility pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.visibility", () -> EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "VisibilityComponent"), noCleanMode));

        // Expand/collapse visual effects pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.lighting", () -> EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "VisualEffectsComponent"), noCleanMode));

        // Expand/collapse datasets pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.dataset.title", () -> EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "DatasetsComponent"), noCleanMode));

        // Expand/collapse objects pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.objects", () -> EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "ObjectsComponent"), noCleanMode));

        // Expand/collapse bookmarks pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.bookmarks", () -> EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "BookmarksComponent"), noCleanMode));

        // Expand/collapse music pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.music", () -> EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "MusicComponent"), noCleanMode));

        // Toggle mouse capture
        addAction(new ProgramAction("action.toggle/gui.mousecapture", () -> EventManager.instance.post(Events.MOUSE_CAPTURE_TOGGLE)));

        // Reload UI (debugging)
        addAction(new ProgramAction("action.ui.reload", () -> EventManager.instance.post(Events.UI_RELOAD_CMD, GaiaSky.instance.getGlobalResources())));

        // Configure slave
        addAction(new ProgramAction("action.slave.configure", () -> EventManager.instance.post(Events.SHOW_SLAVE_CONFIG_ACTION), masterWithSlaves));

        // Camera modes
        for (CameraMode mode : CameraMode.values()) {
            addAction(new ProgramAction("camera.full/camera." + mode.toString(), () -> EventManager.instance.post(Events.CAMERA_MODE_CMD, mode)));
        }

        // Controller GUI
        addAction(new ProgramAction("action.controller.gui.in", () -> EventManager.instance.post(Events.SHOW_CONTROLLER_GUI_ACTION)));
    }

    private void initMappings() {
        final String mappingsFileName = "keyboard.mappings";
        // Check if keyboard.mappings file exists in data folder, otherwise copy it there
        Path customMappings = SysUtils.getDefaultMappingsDir().resolve(mappingsFileName);
        Path defaultMappings = Paths.get(Settings.settings.ASSETS_LOC, SysUtils.getMappingsDirName(), mappingsFileName);
        if (!Files.exists(customMappings)) {
            try {
                Files.copy(defaultMappings, customMappings, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        logger.info(I18n.txt("notif.kbd.mappings.file.use", customMappings));

        try {
            Array<Pair<String, String>> mappings = readMappingsFile(customMappings);

            for (Pair<String, String> mapping : mappings) {
                String key = mapping.getFirst();

                ProgramAction action = actions.get(key);
                if (action != null) {
                    // Parse keys
                    String[] keyMappings = mapping.getSecond().trim().split("\\s+");
                    int[] keyCodes = new int[keyMappings.length];
                    for (int i = 0; i < keyMappings.length; i++) {
                        keyCodes[i] = GSKeys.valueOf(keyMappings[i]);
                    }
                    addMapping(action, keyCodes);

                } else {
                    logger.warn(I18n.txt("notif.kbd.mappings.action.notfound", key));
                }
            }

        } catch (Exception e) {
            logger.error(e, I18n.txt("notif.kbd.mappings.error", customMappings));
        }

    }

    private Array<Pair<String, String>> readMappingsFile(Path file) throws IOException {
        Array<Pair<String, String>> result = new Array<>();
        InputStream is = Files.newInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                String[] strPair = line.split("=");
                result.add(new Pair<>(strPair[0].trim(), strPair[1].trim()));
            }
        }
        return result;
    }

    /**
     * A simple program action. It can optionally contain a condition which must
     * evaluate to true for the action to be run.
     *
 
     */
    public static class ProgramAction implements Runnable, Comparable<ProgramAction> {
        final String actionId;
        final String actionName;
        /**
         * Action to run
         **/
        private final Runnable action;

        /**
         * Condition that must be met
         **/
        private final BooleanRunnable[] conditions;

        ProgramAction(String actionId, Runnable action, BooleanRunnable... conditions) {
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
            } catch (MissingResourceException e) {
                actionName = actionId;
            }
            this.actionName = actionName;
            this.action = action;
            this.conditions = conditions;
        }

        @Override
        public void run() {
            if (evaluateConditions())
                action.run();
        }

        /**
         * Evaluates conditions with a logical AND
         *
         * @return The result from evaluation
         */
        private boolean evaluateConditions() {
            boolean result = true;
            if (conditions != null && conditions.length > 0) {
                for (BooleanRunnable br : conditions) {
                    result = result && br.run();
                }
            }
            return result;
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
