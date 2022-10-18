/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections.CubemapProjection;
import gaiasky.util.i18n.I18n;

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
     *
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
     *
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
                sb.append(GSKeys.toString(it.next()));
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
                result[i++] = GSKeys.toString(it.next());
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
        final Runnable runnableAbout = () -> EventManager.publish(Event.SHOW_ABOUT_ACTION, this);

        // help dialog
        addAction(new ProgramAction("action.help", runnableAbout, noCleanMode));

        // help dialog
        addAction(new ProgramAction("action.help", runnableAbout, noCleanMode));

        // show quit
        final Runnable runnableQuit = () -> {
            // Quit action
            EventManager.publish(Event.SHOW_QUIT_ACTION, this);
        };

        // run quit action
        addAction(new ProgramAction("action.exit", runnableQuit, noCleanMode));

        // exit
        //addAction(new ProgramAction("action.exit", runnableQuit), CTRL_L, Keys.Q);

        // show preferences dialog
        addAction(new ProgramAction("action.preferences", () -> EventManager.publish(Event.SHOW_PREFERENCES_ACTION, this), noCleanMode));

        // minimap toggle
        addAction(new ProgramAction("action.toggle/gui.minimap.title", () -> EventManager.publish(Event.TOGGLE_MINIMAP, this), noCleanMode));

        // load catalog
        addAction(new ProgramAction("action.loadcatalog", () -> EventManager.publish(Event.SHOW_LOAD_CATALOG_ACTION, this), noCleanMode));

        // show log dialog
        addAction(new ProgramAction("action.log", () -> EventManager.publish(Event.SHOW_LOG_ACTION, this), noCleanMode));

        // show play camera dialog
        //addAction(new ProgramAction("action.playcamera", () ->
        //        EventManager.publish(Events.SHOW_PLAYCAMERA_ACTION), fullGuiCondition), Keys.C);

        // Toggle orbits
        addAction(new ProgramAction("action.toggle/element.orbits", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.orbits")));

        // Toggle planets
        addAction(new ProgramAction("action.toggle/element.planets", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.planets")));

        // Toggle moons
        addAction(new ProgramAction("action.toggle/element.moons", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.moons")));

        // Toggle stars
        addAction(new ProgramAction("action.toggle/element.stars", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.stars"), noGameCondition));

        // Toggle satellites
        addAction(new ProgramAction("action.toggle/element.satellites", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.satellites")));

        // Toggle asteroids
        addAction(new ProgramAction("action.toggle/element.asteroids", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.asteroids"), noGameCondition));

        // Toggle labels
        addAction(new ProgramAction("action.toggle/element.labels", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.labels")));

        // Toggle constellations
        addAction(new ProgramAction("action.toggle/element.constellations", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.constellations"), noGameCondition));

        // Toggle boundaries
        addAction(new ProgramAction("action.toggle/element.boundaries", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.boundaries")));

        // Toggle equatorial
        addAction(new ProgramAction("action.toggle/element.equatorial", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.equatorial")));

        // Toggle ecliptic
        addAction(new ProgramAction("action.toggle/element.ecliptic", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.ecliptic")));

        // Toggle galactic
        addAction(new ProgramAction("action.toggle/element.galactic", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.galactic")));

        // Toggle recgrid
        addAction(new ProgramAction("action.toggle/element.recursivegrid", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.recursivegrid")));

        // toggle meshes
        addAction(new ProgramAction("action.toggle/element.meshes", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.meshes")));

        // toggle clusters
        addAction(new ProgramAction("action.toggle/element.clusters", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.clusters")));

        // divide speed
        addAction(new ProgramAction("action.dividetime", () -> EventManager.publish(Event.TIME_WARP_DECREASE_CMD, this)));

        // double speed
        addAction(new ProgramAction("action.doubletime", () -> EventManager.publish(Event.TIME_WARP_INCREASE_CMD, this)));

        // toggle time
        addAction(new ProgramAction("action.pauseresume", () -> {
            // Game mode has space bound to 'up'
            if (!GaiaSky.instance.cameraManager.mode.isGame())
                EventManager.publish(Event.TIME_STATE_CMD, this, !Settings.settings.runtime.timeOn);
        }));

        // increase field of view
        addAction(new ProgramAction("action.incfov", () -> EventManager.publish(Event.FOV_CHANGED_CMD, this, Settings.settings.scene.camera.fov + 1f), noSlaveProj));

        // decrease field of view
        addAction(new ProgramAction("action.decfov", () -> EventManager.publish(Event.FOV_CHANGED_CMD, this, Settings.settings.scene.camera.fov - 1f), noSlaveProj));

        // fullscreen
        addAction(new ProgramAction("action.togglefs", () -> {
            Settings.settings.graphics.fullScreen.active = !Settings.settings.graphics.fullScreen.active;
            EventManager.publish(Event.SCREEN_MODE_CMD, this);
        }));

        // take screenshot
        addAction(new ProgramAction("action.screenshot", () -> EventManager.publish(Event.SCREENSHOT_CMD, this, Settings.settings.screenshot.resolution[0], Settings.settings.screenshot.resolution[1], Settings.settings.screenshot.location)));

        // toggle frame output
        addAction(new ProgramAction("action.toggle/element.frameoutput", () -> EventManager.publish(Event.FRAME_OUTPUT_CMD, this, !Settings.settings.frame.active)));

        // toggle UI collapse/expand
        addAction(new ProgramAction("action.toggle/element.controls", () -> EventManager.publish(Event.GUI_FOLD_CMD, this), fullGuiCondition, noCleanMode));

        // toggle planetarium mode
        addAction(new ProgramAction("action.toggle/element.planetarium", () -> {
            boolean enable = !Settings.settings.program.modeCubemap.active;
            EventManager.publish(Event.CUBEMAP_CMD, this, enable, CubemapProjection.AZIMUTHAL_EQUIDISTANT);
        }, noPanorama));

        // toggle cubemap mode
        addAction(new ProgramAction("action.toggle/element.360", () -> {
            boolean enable = !Settings.settings.program.modeCubemap.active;
            EventManager.publish(Event.CUBEMAP_CMD, this, enable, CubemapProjection.EQUIRECTANGULAR);
        }, noPlanetarium));

        // toggle cubemap projection
        addAction(new ProgramAction("action.toggle/element.projection", () -> {
            int newProjectionIndex = (Settings.settings.program.modeCubemap.projection.ordinal() + 1) % (CubemapProjection.HAMMER.ordinal() + 3);
            EventManager.publish(Event.CUBEMAP_PROJECTION_CMD, this, CubemapProjection.values()[newProjectionIndex]);
        }));

        // increase star point size by 0.5
        addAction(new ProgramAction("action.starpointsize.inc", () -> EventManager.publish(Event.STAR_POINT_SIZE_INCREASE_CMD, this)));

        // decrease star point size by 0.5
        addAction(new ProgramAction("action.starpointsize.dec", () -> EventManager.publish(Event.STAR_POINT_SIZE_DECREASE_CMD, this)));

        // reset star point size
        addAction(new ProgramAction("action.starpointsize.reset", () -> EventManager.publish(Event.STAR_POINT_SIZE_RESET_CMD, this)));

        // new keyframe
        addAction(new ProgramAction("action.keyframe", () -> EventManager.publish(Event.KEYFRAME_ADD, this)));

        // toggle debug information
        addAction(new ProgramAction("action.toggle/element.debugmode", () -> EventManager.publish(Event.SHOW_DEBUG_CMD, this), noCleanMode));

        // search dialog
        final Runnable runnableSearch = () -> EventManager.publish(Event.SHOW_SEARCH_ACTION, this);
        addAction(new ProgramAction("action.search", runnableSearch, fullGuiCondition, noCleanMode));

        // search dialog
        addAction(new ProgramAction("action.search", runnableSearch, fullGuiCondition, noCleanMode));

        // search dialog
        addAction(new ProgramAction("action.search", runnableSearch, fullGuiCondition, noCleanMode));

        // toggle particle fade
        addAction(new ProgramAction("action.toggle/element.octreeparticlefade", () -> EventManager.publish(Event.OCTREE_PARTICLE_FADE_CMD, this, I18n.msg("element.octreeparticlefade"), !Settings.settings.scene.octree.fade)));

        // toggle stereoscopic mode
        addAction(new ProgramAction("action.toggle/element.stereomode", () -> EventManager.publish(Event.STEREOSCOPIC_CMD, this, !Settings.settings.program.modeStereo.active)));

        // switch stereoscopic profile
        addAction(new ProgramAction("action.switchstereoprofile", () -> {
            int newidx = Settings.settings.program.modeStereo.profile.ordinal();
            newidx = (newidx + 1) % values().length;
            EventManager.publish(Event.STEREO_PROFILE_CMD, this, newidx);
        }));

        // Toggle clean (no GUI) mode
        addAction(new ProgramAction("action.toggle/element.cleanmode", () -> EventManager.publish(Event.DISPLAY_GUI_CMD, this, !Settings.settings.runtime.displayGui, I18n.msg("notif.cleanmode"))));

        // Travel to focus object
        addAction(new ProgramAction("action.gotoobject", () -> EventManager.publish(Event.GO_TO_OBJECT_CMD, this)));

        // Reset time to current system time
        addAction(new ProgramAction("action.resettime", () -> EventManager.publish(Event.TIME_CHANGE_CMD, this, Instant.now())));

        // Back home
        addAction(new ProgramAction("action.home", () -> EventManager.publish(Event.HOME_CMD, this)));

        // Expand/collapse time pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.time", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "TimeComponent"), noCleanMode));

        // Expand/collapse camera pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.camera", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "CameraComponent"), noCleanMode));

        // Expand/collapse visibility pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.visibility", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "VisibilityComponent"), noCleanMode));

        // Expand/collapse visual effects pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.lighting", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "VisualEffectsComponent"), noCleanMode));

        // Expand/collapse datasets pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.dataset.title", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "DatasetsComponent"), noCleanMode));

        // Expand/collapse objects pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.objects", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "ObjectsComponent"), noCleanMode));

        // Expand/collapse bookmarks pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.bookmarks", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "BookmarksComponent"), noCleanMode));

        // Expand/collapse music pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.music", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "MusicComponent"), noCleanMode));

        // Toggle mouse capture
        addAction(new ProgramAction("action.toggle/gui.mousecapture", () -> EventManager.publish(Event.MOUSE_CAPTURE_TOGGLE, this)));

        // Reload UI (debugging)
        addAction(new ProgramAction("action.ui.reload", () -> EventManager.publish(Event.UI_RELOAD_CMD, this, GaiaSky.instance.getGlobalResources())));

        // Configure slave
        addAction(new ProgramAction("action.slave.configure", () -> EventManager.publish(Event.SHOW_SLAVE_CONFIG_ACTION, this), masterWithSlaves));

        // Camera modes
        for (CameraMode mode : CameraMode.values()) {
            addAction(new ProgramAction("camera.full/camera." + mode.toString(), () -> EventManager.publish(Event.CAMERA_MODE_CMD, this, mode)));
        }

        // Controller GUI
        addAction(new ProgramAction("action.controller.gui.in", () -> EventManager.publish(Event.SHOW_CONTROLLER_GUI_ACTION, this)));
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
        logger.info(I18n.msg("notif.kbd.mappings.file.use", customMappings));

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
                    logger.warn(I18n.msg("notif.kbd.mappings.action.notfound", key));
                }
            }
        } catch (Exception e) {
            logger.error(e, I18n.msg("notif.kbd.mappings.error", customMappings));
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
     */
    public static class ProgramAction implements Runnable, Comparable<ProgramAction> {
        final String actionId;
        final String actionName;
        /** Action to run. **/
        private final Runnable action;

        /** Condition that must be met. **/
        private final BooleanRunnable[] conditions;

        ProgramAction(String actionId, Runnable action, BooleanRunnable... conditions) {
            this.actionId = actionId;
            // Set action name
            String actionName;
            try {
                if (actionId.contains("/")) {
                    String[] actions = actionId.split("/");
                    actionName = I18n.msg(actions[0], I18n.msg(actions[1]));
                } else {
                    actionName = I18n.msg(actionId);
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
            if (evaluateConditions()) {
                action.run();
            }
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
