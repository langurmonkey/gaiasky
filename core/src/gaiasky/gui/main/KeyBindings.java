/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.UpscaleFilter;
import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static gaiasky.util.Settings.StereoProfile.values;

/**
 * Defines keyboard bindings to actions.
 */
public class KeyBindings {
    // Special keys
    public static final int CTRL_L = Keys.CONTROL_LEFT;
    public static final int SHIFT_L = Keys.SHIFT_LEFT;
    public static final int ALT_L = Keys.ALT_LEFT;
    public static final int[] SPECIAL = new int[]{CTRL_L, SHIFT_L, ALT_L};
    private static final Log logger = Logger.getLogger(KeyBindings.class);
    public static KeyBindings instance;
    private final Map<String, ProgramAction> actions;
    private final Map<TreeSet<Integer>, ProgramAction> mappings;
    private final Map<ProgramAction, Array<TreeSet<Integer>>> mappingsInv;

    /**
     * Creates a key mappings instance.
     */
    private KeyBindings() {
        actions = new HashMap<>();
        mappings = new HashMap<>();
        mappingsInv = new TreeMap<>();
        initDefault();
    }

    public static void initialize() {
        initialize(false);
    }

    public static void initialize(boolean force) {
        if (instance == null || force) {
            instance = new KeyBindings();
        }
    }

    public Map<TreeSet<Integer>, ProgramAction> getMappings() {
        return mappings;
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
     * Gets the first keys found that trigger the action identified by the given name.
     * If many sets of keys are assigned to the same action, only the first ones are returned.
     *
     * @param actionId The action ID.
     * @return The keys.
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

    /**
     * Gets all the sets of keys that trigger the action identified by the given name.
     *
     * @param actionId The action ID.
     * @return The list of key sets.
     */
    public List<TreeSet<Integer>> getAllKeys(String actionId) {
        List<TreeSet<Integer>> result = new ArrayList<>();
        ProgramAction action = findAction(actionId);
        if (action != null) {
            Set<Map.Entry<TreeSet<Integer>, ProgramAction>> entries = mappings.entrySet();
            for (Map.Entry<TreeSet<Integer>, ProgramAction> entry : entries) {
                if (entry.getValue().equals(action)) {
                    result.add(entry.getKey());
                }
            }
            return result;
        }
        return null;
    }

    public String getStringKeys(String actionId) {
        var r = getStringKeys(actionId, "+", false);
        if (r != null && r.length > 0) {
            return r[0];
        }
        return null;
    }

    public String[] getStringKeys(String actionId, boolean allSets) {
        return getStringKeys(actionId, "+", allSets);
    }

    public String[] getStringKeys(String actionId, String join, boolean allSets) {
        if (allSets) {
            var keySets = getAllKeys(actionId);
            if (keySets != null && !keySets.isEmpty()) {
                String[] result = new String[keySets.size()];
                int i = 0;
                for (TreeSet<Integer> keys : keySets) {
                    result[i] = getStringKeys(keys, join);
                    i++;
                }
                return result;
            } else {
                return null;
            }
        } else {
            var keys = getKeys(actionId);
            var s = getStringKeys(keys, join);
            if (s != null) {
                return new String[]{s};
            } else {
                return null;
            }
        }
    }

    private String getStringKeys(TreeSet<Integer> keys, String join) {
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
        BooleanRunnable noCleanMode = () -> Settings.settings.runtime.displayGui || GaiaSky.instance.getCliArgs().externalView;
        // Condition that checks that panorama mode is off
        BooleanRunnable noPanorama = () -> !(Settings.settings.program.modeCubemap.active && Settings.settings.program.modeCubemap.projection.isPanorama());
        // Condition that checks that planetarium mode is off
        BooleanRunnable noPlanetarium = () -> !(Settings.settings.program.modeCubemap.active && Settings.settings.program.modeCubemap.projection.isPlanetarium());
        // Condition that checks that ortho-sphere view mode is off
        BooleanRunnable noOrthoSphere = () -> !(Settings.settings.program.modeCubemap.active && Settings.settings.program.modeCubemap.projection.isOrthosphere());
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

        // show preferences dialog
        addAction(new ProgramAction("action.preferences", () -> EventManager.publish(Event.SHOW_PREFERENCES_ACTION, this), noCleanMode));

        // minimap toggle
        addAction(new ProgramAction("action.toggle/gui.minimap.title", () -> EventManager.publish(Event.MINIMAP_TOGGLE_CMD, this), noCleanMode));

        // console toggle
        addAction(new ProgramAction("action.toggle/gui.console.title", () -> EventManager.publish(Event.CONSOLE_CMD, this), noCleanMode));

        // console command
        addAction(new ProgramAction("action.console", () -> EventManager.publish(Event.CONSOLE_CMD, this, true), noCleanMode));

        // load catalog
        addAction(new ProgramAction("action.loadcatalog", () -> EventManager.publish(Event.SHOW_LOAD_CATALOG_ACTION, this), noCleanMode));

        // play camera path
        addAction(new ProgramAction("action.playcamera", () -> EventManager.publish(Event.SHOW_PLAYCAMERA_CMD, this), noCleanMode));

        // show log dialog
        addAction(new ProgramAction("action.log", () -> EventManager.publish(Event.SHOW_LOG_CMD, this), noCleanMode));

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

        // Toggle meshes
        addAction(new ProgramAction("action.toggle/element.meshes", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.meshes")));

        // Toggle clusters
        addAction(new ProgramAction("action.toggle/element.clusters", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.clusters")));

        // Toggle keyframes
        addAction(new ProgramAction("action.toggle/element.keyframes", () -> EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.keyframes")));

        // Divide time warp
        addAction(new ProgramAction("action.dividetime", () -> EventManager.publish(Event.TIME_WARP_DECREASE_CMD, this), 500L));

        // Double time warp
        addAction(new ProgramAction("action.doubletime", () -> EventManager.publish(Event.TIME_WARP_INCREASE_CMD, this), 500L));

        // Reset time warp to 1
        addAction(new ProgramAction("action.time.warp.reset", () -> EventManager.publish(Event.TIME_WARP_CMD, this, 1d)));

        // Toggle time
        addAction(new ProgramAction("action.pauseresume", () -> {
            // Game mode has space bound to 'up'
            if (!GaiaSky.instance.cameraManager.mode.isGame())
                EventManager.publish(Event.TIME_STATE_CMD, this, !Settings.settings.runtime.timeOn);
        }));

        // Increase field of view
        addAction(new ProgramAction("action.incfov", () -> EventManager.publish(Event.FOV_CMD, this, Settings.settings.scene.camera.fov + 1f), noSlaveProj));

        // Decrease field of view
        addAction(new ProgramAction("action.decfov", () -> EventManager.publish(Event.FOV_CMD, this, Settings.settings.scene.camera.fov - 1f), noSlaveProj));

        // Fullscreen
        addAction(new ProgramAction("action.togglefs", () -> {
            Settings.settings.graphics.fullScreen.active = !Settings.settings.graphics.fullScreen.active;
            EventManager.publish(Event.SCREEN_MODE_CMD, this);
        }));

        // Take screenshot
        addAction(new ProgramAction("action.screenshot", () -> EventManager.publish(Event.SCREENSHOT_CMD, this, Settings.settings.screenshot.resolution[0], Settings.settings.screenshot.resolution[1], Settings.settings.screenshot.location)));

        // Save cubemap faces
        addAction(new ProgramAction("action.screenshot.cubemap", () -> EventManager.publish(Event.SCREENSHOT_CUBEMAP_CMD, this, Settings.settings.screenshot.location)));

        // Toggle frame output
        addAction(new ProgramAction("action.toggle/element.frameoutput", () -> EventManager.publish(Event.FRAME_OUTPUT_CMD, this, !Settings.settings.frame.active)));

        // Toggle UI collapse/expand
        addAction(new ProgramAction("action.toggle/element.controls", () -> EventManager.publish(Event.GUI_FOLD_CMD, this), fullGuiCondition, noCleanMode));

        // Toggle planetarium mode
        addAction(new ProgramAction("action.toggle/element.planetarium", () -> {
            boolean enable = !Settings.settings.program.modeCubemap.active || !Settings.settings.program.modeCubemap.isPlanetariumOn();
            EventManager.publish(Event.CUBEMAP_CMD, this, enable, CubemapProjection.AZIMUTHAL_EQUIDISTANT);
        }, noPanorama, noOrthoSphere));

        // Toggle planetarium projection
        addAction(new ProgramAction("action.toggle/element.planetarium.projection", () -> {
            if (Settings.settings.program.modeCubemap.isPlanetariumOn()) {
                int newProjectionIndex = Settings.settings.program.modeCubemap.projection.getNextPlanetariumProjection().ordinal();
                EventManager.publish(Event.PLANETARIUM_PROJECTION_CMD, this, CubemapProjection.values()[newProjectionIndex]);
            }
        }, noPanorama, noOrthoSphere));

        // Toggle cubemap mode
        addAction(new ProgramAction("action.toggle/element.360", () -> {
            boolean enable = !Settings.settings.program.modeCubemap.active || !Settings.settings.program.modeCubemap.isPanoramaOn();
            EventManager.publish(Event.CUBEMAP_CMD, this, enable, CubemapProjection.EQUIRECTANGULAR);
        }, noPlanetarium, noOrthoSphere));

        // Toggle cubemap projection
        addAction(new ProgramAction("action.toggle/element.projection", () -> {
            if (Settings.settings.program.modeCubemap.isPanoramaOn()) {
                int newProjectionIndex = Settings.settings.program.modeCubemap.projection.getNextPanoramaProjection().ordinal();
                EventManager.publish(Event.CUBEMAP_PROJECTION_CMD, this, CubemapProjection.values()[newProjectionIndex]);
            }
        }, noPlanetarium, noOrthoSphere));

        // Toggle orthosphere mode
        addAction(new ProgramAction("action.toggle/element.orthosphere", () -> {
            boolean enable = !Settings.settings.program.modeCubemap.active || !Settings.settings.program.modeCubemap.isOrthosphereOn();
            EventManager.publish(Event.CUBEMAP_CMD, this, enable, CubemapProjection.ORTHOSPHERE);
        }, noPlanetarium, noPanorama));

        // Toggle orthosphere profile
        addAction(new ProgramAction("action.toggle/element.orthosphere.profile", () -> {
            if (Settings.settings.program.modeCubemap.isOrthosphereOn()) {
                int newProfileIndex = Settings.settings.program.modeCubemap.projection.getNextOrthosphereProfile().ordinal();
                EventManager.publish(Event.CUBEMAP_PROJECTION_CMD, this, CubemapProjection.values()[newProfileIndex]);
            }
        }));

        // Increase star point size by 0.5
        addAction(new ProgramAction("action.starpointsize.inc", () -> EventManager.publish(Event.STAR_POINT_SIZE_INCREASE_CMD, this)));

        // Decrease star point size by 0.5
        addAction(new ProgramAction("action.starpointsize.dec", () -> EventManager.publish(Event.STAR_POINT_SIZE_DECREASE_CMD, this)));

        // Reset star point size
        addAction(new ProgramAction("action.starpointsize.reset", () -> EventManager.publish(Event.STAR_POINT_SIZE_RESET_CMD, this)));

        // New keyframe
        addAction(new ProgramAction("action.keyframe", () -> EventManager.publish(Event.KEYFRAME_ADD, this)));

        // Toggle debug information
        addAction(new ProgramAction("action.toggle/element.debugmode", () -> EventManager.publish(Event.SHOW_DEBUG_CMD, this), noCleanMode));

        // Search dialog
        final Runnable runnableSearch = () -> EventManager.publish(Event.SHOW_SEARCH_ACTION, this);
        addAction(new ProgramAction("action.search", runnableSearch, fullGuiCondition, noCleanMode));

        // Toggle particle fade
        addAction(new ProgramAction("action.toggle/element.octreeparticlefade", () -> EventManager.publish(Event.OCTREE_PARTICLE_FADE_CMD, this, !Settings.settings.scene.octree.fade)));

        // Toggle stereoscopic mode
        addAction(new ProgramAction("action.toggle/element.stereomode", () -> EventManager.publish(Event.STEREOSCOPIC_CMD, this, !Settings.settings.program.modeStereo.active)));

        // Switch stereoscopic profile
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
        addAction(new ProgramAction("action.expandcollapse.pane/gui.lighting", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "VisualSettingsComponent"), noCleanMode));

        // Expand/collapse datasets pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.dataset.title", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "DatasetsComponent"), noCleanMode));

        // Expand/collapse bookmarks pane
        addAction(new ProgramAction("action.expandcollapse.pane/gui.bookmarks", () -> EventManager.publish(Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, this, "BookmarksComponent"), noCleanMode));

        // Toggle mouse capture
        addAction(new ProgramAction("action.toggle/gui.mousecapture", () -> EventManager.publish(Event.MOUSE_CAPTURE_TOGGLE, this)));

        // Reload UI (debugging)
        addAction(new ProgramAction("action.ui.reload", () -> EventManager.publish(Event.UI_RELOAD_CMD, this, GaiaSky.instance.getGlobalResources())));

        // Re-compile post-process shaders (debugging)
        addAction(new ProgramAction("action.shaders.reload", () -> EventManager.publish(Event.SHADER_RELOAD_CMD, this)));

        // Configure slave
        addAction(new ProgramAction("action.slave.configure", () -> EventManager.publish(Event.SHOW_SLAVE_CONFIG_ACTION, this), masterWithSlaves));

        // Camera modes
        for (CameraMode mode : CameraMode.values()) {
            addAction(new ProgramAction("camera.full/camera." + mode.toString(), () -> EventManager.publish(Event.CAMERA_MODE_CMD, this, mode)));
        }

        // Toggle camera mode
        addAction(new ProgramAction("action.toggle/camera.mode", () -> {
            CameraMode oldMode = GaiaSky.instance.getCameraManager().getMode();
            CameraMode newMode = CameraMode.values()[(oldMode.ordinal() + 1) % CameraMode.values().length];
            EventManager.publish(Event.CAMERA_MODE_CMD, this, newMode);
        }));

        // Toggle cinematic camera behaviour
        addAction(new ProgramAction("action.toggle/camera.cinematic", () -> EventManager.publish(Event.CAMERA_CINEMATIC_CMD, this, !Settings.settings.scene.camera.cinematic)));

        // Empty action, press to speed up camera
        addAction(new ProgramAction("action.camera.speedup", () -> {
        }));

        // Controller GUI
        addAction(new ProgramAction("action.controller.gui.in", () -> EventManager.publish(Event.SHOW_CONTROLLER_GUI_ACTION, this)));

        // Debug upscale filter
        addAction(new ProgramAction("action.upscale", () -> {
            var filter = Settings.settings.postprocess.upscaleFilter;
            var newFilter = UpscaleFilter.values()[(filter.ordinal() + 1) % UpscaleFilter.values().length];
            EventManager.publish(Event.UPSCALE_FILTER_CMD, this, newFilter);
            logger.info("Upscaling filter: " + newFilter);
        }));
    }

    private void initMappings() {
        final String mappingsFileName = "keyboard.mappings";
        // Check if keyboard.mappings file exists in data folder and version is greater or equal to ours, otherwise overwrite it.
        Path customMappings = SysUtils.getDefaultMappingsDir().resolve(mappingsFileName);
        Path defaultMappings = Paths.get(Settings.ASSETS_LOC, SysUtils.getMappingsDirName(), mappingsFileName);
        if (!Files.exists(customMappings)) {
            // Mappings file does not exist, just copy it.
            overwriteMappingsFile(defaultMappings, customMappings, false);
        } else {
            // Check versions.
            var customVersionStr = TextUtils.readFirstLine(customMappings);
            var defaultVersionStr = TextUtils.readFirstLine(defaultMappings);
            if (customVersionStr.isEmpty() || !customVersionStr.get().startsWith("#v")) {
                // We have no version in local file, overwrite.
                overwriteMappingsFile(defaultMappings, customMappings, true);
            } else if (defaultVersionStr.isPresent()) {
                var customVersion = Parser.parseInt(customVersionStr.get().substring(2));
                var defaultVersion = Parser.parseInt(defaultVersionStr.get().substring(2));

                if (defaultVersion > customVersion) {
                    // Our version is greater, overwrite.
                    overwriteMappingsFile(defaultMappings, customMappings, true);
                }
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

    /**
     * Copies the file src to the file to, optionally making a backup.
     *
     * @param src    The source file.
     * @param dst    The destination file.
     * @param backup Whether to create a backup of dst if it exists.
     */
    private void overwriteMappingsFile(Path src, Path dst, boolean backup) {
        assert src != null && src.toFile().exists() && src.toFile().isFile() && src.toFile().canRead() : I18n.msg("error.file.exists.readable", src != null ? src.getFileName().toString() : "null");
        assert dst != null : I18n.msg("notif.null.not", "dest");
        if (backup && dst.toFile().exists() && dst.toFile().canRead()) {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hhmmss");
            String strDate = dateFormat.format(date);

            var backupName = dst.getFileName().toString() + "." + strDate;
            Path backupFile = dst.getParent().resolve(backupName);
            // Copy.
            try {
                Files.copy(dst, backupFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info(I18n.msg("notif.file.backup", backupFile));
            } catch (IOException e) {
                logger.error(e);
            }
        }
        // Actually copy file.
        try {
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            logger.info(I18n.msg("notif.file.update", dst.toString()));
            if (backup) {
                EventManager.publishWaitUntilConsumer(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.file.overriden.backup", dst.toString()), -1f);
            } else {
                EventManager.publishWaitUntilConsumer(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.file.overriden", dst.toString()), -1f);
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private Array<Pair<String, String>> readMappingsFile(Path file) throws IOException {
        Array<Pair<String, String>> result = new Array<>();
        InputStream is = Files.newInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                String[] strPair = line.split("=");
                result.add(new Pair<>(strPair[0].trim(), strPair[1].trim()));
            }
        }
        reader.close();
        return result;
    }

    public interface BooleanRunnable {
        boolean run();
    }

    /**
     * A simple program action. It can optionally contain a condition which must
     * evaluate to true for the action to be run.
     */
    public static class ProgramAction implements Runnable, Comparable<ProgramAction> {
        /**
         * Contains the maximum amount of time between the key down and key up events.
         **/
        public final long maxKeyDownTimeMs;
        final String actionId;
        public final String actionName;
        /**
         * Action to run.
         **/
        private final Runnable action;

        /**
         * Condition that must be met.
         **/
        private final BooleanRunnable[] conditions;

        ProgramAction(String actionId, Runnable action, long maxKeyDownTimeMs, BooleanRunnable... conditions) {
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
            this.maxKeyDownTimeMs = maxKeyDownTimeMs;
        }

        ProgramAction(String actionId, Runnable action, BooleanRunnable... conditions) {
            this(actionId, action, 10000L, conditions);
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
            if (conditions != null) {
                for (BooleanRunnable br : conditions) {
                    result = result && br.run();
                }
            }
            return result;
        }

        @Override
        public int compareTo(ProgramAction other) {
            return actionName.toLowerCase(Locale.ROOT).compareTo(other.actionName.toLowerCase(Locale.ROOT));
        }

    }

}
