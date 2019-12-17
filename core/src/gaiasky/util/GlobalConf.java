/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.camera.CameraKeyframeManager;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.ModePopupInfo;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.util.Logger.Log;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.DateFormatFactory.DateType;
import gaiasky.util.format.IDateFormat;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.update.VersionChecker;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the global configuration options
 *
 * @author Toni Sagrista
 */
public class GlobalConf {
    private static final Log logger = Logger.getLogger(GlobalConf.class);

    public static String APPLICATION_NAME = "Gaia Sky";
    public static final String APPLICATION_SHORT_NAME = "gaiasky";
    public static final String WEBPAGE = "https://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky";
    public static final String WEBPAGE_DOWNLOADS = "https://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky/downloads";
    public static final String DOCUMENTATION = "http://gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest";
    public static final String ICON_URL = "https://github.com/langurmonkey/gaiasky/blob/master/assets/icon/gs_064.png?raw=true";
    public static final String AUTHOR_NAME = "Toni Sagrista Selles";
    public static final String AUTHOR_EMAIL = "tsagrista@ari.uni-heidelberg.de";
    public static final String AUTHOR_AFFILIATION = "Heidelberg University, Zentrum fuer Astronomie, Astronomisches Rechen-Institut";

    // Assets location for this instance of Gaia Sky
    // macOS needs fully qualified paths when run as an app (GaiaSky.app), that's why we use the getAbsolutePath() part
    public static final String ASSETS_LOC = (new File(System.getProperty("assets.location") != null ? System.getProperty("assets.location") : ".")).getAbsolutePath();

    // Interface scale factor (for HiDPI)
    public static float UI_SCALE_FACTOR = -1.0f;

    public static void updateScaleFactor(float sf) {
        UI_SCALE_FACTOR = sf;
        logger.debug("GUI scale factor set to " + GlobalConf.UI_SCALE_FACTOR);
    }

    public interface IConf {
    }

    public enum ScreenshotMode {
        simple,
        redraw
    }

    public enum ImageFormat {
        PNG,
        JPG
    }

    public static class ScreenshotConf implements IConf {

        public static final int MIN_SCREENSHOT_SIZE = 50;
        public static final int MAX_SCREENSHOT_SIZE = 25000;

        public int SCREENSHOT_WIDTH;
        public int SCREENSHOT_HEIGHT;
        public String SCREENSHOT_FOLDER;
        public ScreenshotMode SCREENSHOT_MODE;
        public ImageFormat SCREENSHOT_FORMAT;
        public float SCREENSHOT_QUALITY;

        public void initialize(int sCREENSHOT_WIDTH, int sCREENSHOT_HEIGHT, String sCREENSHOT_FOLDER, ScreenshotMode sCREENSHOT_MODE, ImageFormat sCREENSHOT_FORMAT, float sCREENSHOT_QUALITY) {
            SCREENSHOT_WIDTH = sCREENSHOT_WIDTH;
            SCREENSHOT_HEIGHT = sCREENSHOT_HEIGHT;
            SCREENSHOT_FOLDER = sCREENSHOT_FOLDER;
            SCREENSHOT_MODE = sCREENSHOT_MODE;
            SCREENSHOT_FORMAT = sCREENSHOT_FORMAT;
            SCREENSHOT_QUALITY = sCREENSHOT_QUALITY;
        }

        public boolean isSimpleMode() {
            return SCREENSHOT_MODE.equals(ScreenshotMode.simple);
        }

        public boolean isRedrawMode() {
            return SCREENSHOT_MODE.equals(ScreenshotMode.redraw);
        }

    }

    public static class PerformanceConf implements IConf {

        public boolean MULTITHREADING;
        public int NUMBER_THREADS;

        public void initialize(boolean MULTITHREADING, int NUMBER_THREADS) {
            this.MULTITHREADING = MULTITHREADING;
            this.NUMBER_THREADS = NUMBER_THREADS;
        }

        /**
         * Returns the actual number of threads. It accounts for the number of
         * threads being 0 or less, "let the program decide" option, in which
         * case the number of processors is returned
         *
         * @return The number of threads
         */
        public int NUMBER_THREADS() {
            if (NUMBER_THREADS <= 0)
                return Runtime.getRuntime().availableProcessors();
            else
                return NUMBER_THREADS;
        }

    }

    public static class PostprocessConf implements IConf, IObserver {

        public enum Antialias {
            NONE(0),
            FXAA(-1),
            NFAA(-2),
            SSAA(1);

            int aacode;

            Antialias(int aacode) {
                this.aacode = aacode;
            }

            public int getAACode() {
                return this.aacode;
            }

            public boolean isPostProcessAntialias() {
                return this.aacode < 0;
            }
        }

        public Antialias getAntialias(int code) {
            switch (code) {
            case 0:
                return Antialias.NONE;
            case -1:
                return Antialias.FXAA;
            case -2:
                return Antialias.NFAA;
            case 1:
                return Antialias.SSAA;
            default:
                return Antialias.NONE;
            }
        }

        public Antialias POSTPROCESS_ANTIALIAS;
        public float POSTPROCESS_BLOOM_INTENSITY;
        public boolean POSTPROCESS_MOTION_BLUR;
        public boolean POSTPROCESS_LENS_FLARE;
        public boolean POSTPROCESS_LIGHT_SCATTERING;
        public boolean POSTPROCESS_FISHEYE;
        /**
         * Brightness level in [-1..1]. Default is 0.
         **/
        public float POSTPROCESS_BRIGHTNESS;
        /**
         * Contrast level in [0..2]. Default is 1.
         **/
        public float POSTPROCESS_CONTRAST;
        /**
         * Hue level in [0..2]. Default is 1.
         **/
        public float POSTPROCESS_HUE;
        /**
         * Saturation level in [0..2]. Default is 1.
         **/
        public float POSTPROCESS_SATURATION;
        /**
         * Gamma correction value in [0.1..3]
         **/
        public float POSTPROCESS_GAMMA;

        public enum ToneMapping {
            AUTO,
            EXPOSURE,
            ACES,
            UNCHARTED,
            FILMIC,
            NONE
        }

        /**
         * Tone mapping type: automatic, exposure, aces, uncharted, filmic, none
         */
        public ToneMapping POSTPROCESS_TONEMAPPING_TYPE;
        /**
         * Exposure tone mapping value in [0..n]. 0 is disabled.
         */
        public float POSTPROCESS_EXPOSURE;

        public PostprocessConf() {
            EventManager.instance.subscribe(this, Events.BLOOM_CMD, Events.LENS_FLARE_CMD, Events.MOTION_BLUR_CMD, Events.LIGHT_SCATTERING_CMD, Events.FISHEYE_CMD, Events.BRIGHTNESS_CMD, Events.CONTRAST_CMD, Events.HUE_CMD, Events.SATURATION_CMD, Events.GAMMA_CMD, Events.TONEMAPPING_TYPE_CMD, Events.EXPOSURE_CMD);
        }

        public void initialize(Antialias POSTPROCESS_ANTIALIAS, float POSTPROCESS_BLOOM_INTENSITY, boolean POSTPROCESS_MOTION_BLUR, boolean POSTPROCESS_LENS_FLARE, boolean POSTPROCESS_LIGHT_SCATTERING, boolean POSTPROCESS_FISHEYE, float POSTPROCESS_BRIGHTNESS, float POSTPROCESS_CONTRAST, float POSTPROCESS_HUE, float POSTPROCESS_SATURATION, float POSTPROCESS_GAMMA, ToneMapping POSTPROCESS_TONEMAPPING_TYPE, float POSTPROCESS_EXPOSURE) {
            this.POSTPROCESS_ANTIALIAS = POSTPROCESS_ANTIALIAS;
            this.POSTPROCESS_BLOOM_INTENSITY = POSTPROCESS_BLOOM_INTENSITY;
            this.POSTPROCESS_MOTION_BLUR = POSTPROCESS_MOTION_BLUR;
            this.POSTPROCESS_LENS_FLARE = POSTPROCESS_LENS_FLARE;
            this.POSTPROCESS_LIGHT_SCATTERING = POSTPROCESS_LIGHT_SCATTERING;
            this.POSTPROCESS_FISHEYE = POSTPROCESS_FISHEYE;
            this.POSTPROCESS_BRIGHTNESS = POSTPROCESS_BRIGHTNESS;
            this.POSTPROCESS_CONTRAST = POSTPROCESS_CONTRAST;
            this.POSTPROCESS_HUE = POSTPROCESS_HUE;
            this.POSTPROCESS_SATURATION = POSTPROCESS_SATURATION;
            this.POSTPROCESS_GAMMA = POSTPROCESS_GAMMA;
            this.POSTPROCESS_TONEMAPPING_TYPE = POSTPROCESS_TONEMAPPING_TYPE;
            this.POSTPROCESS_EXPOSURE = POSTPROCESS_EXPOSURE;
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case BLOOM_CMD:
                POSTPROCESS_BLOOM_INTENSITY = (float) data[0];
                break;
            case LENS_FLARE_CMD:
                POSTPROCESS_LENS_FLARE = (Boolean) data[0];
                break;
            case LIGHT_SCATTERING_CMD:
                POSTPROCESS_LIGHT_SCATTERING = (Boolean) data[0];
                break;
            case MOTION_BLUR_CMD:
                POSTPROCESS_MOTION_BLUR = (Boolean) data[0];
                break;
            case FISHEYE_CMD:
                POSTPROCESS_FISHEYE = (Boolean) data[0];

                // Post a message to the screen
                if (POSTPROCESS_FISHEYE) {
                    ModePopupInfo mpi = new ModePopupInfo();
                    mpi.title = "Planetarium mode";
                    mpi.header = "You have entered Planetarium mode!";
                    mpi.addMapping("Back to normal mode", "CTRL", "P");
                    mpi.addMapping("Switch planetarium mode type", "CTRL", "SHIFT", "P");

                    EventManager.instance.post(Events.MODE_POPUP_CMD, mpi, 120f);
                }
                break;
            case BRIGHTNESS_CMD:
                POSTPROCESS_BRIGHTNESS = MathUtils.clamp((float) data[0], Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS);
                break;
            case CONTRAST_CMD:
                POSTPROCESS_CONTRAST = MathUtils.clamp((float) data[0], Constants.MIN_CONTRAST, Constants.MAX_CONTRAST);
                break;
            case HUE_CMD:
                POSTPROCESS_HUE = MathUtils.clamp((float) data[0], Constants.MIN_HUE, Constants.MAX_HUE);
                break;
            case SATURATION_CMD:
                POSTPROCESS_SATURATION = MathUtils.clamp((float) data[0], Constants.MIN_SATURATION, Constants.MAX_SATURATION);
                break;
            case GAMMA_CMD:
                POSTPROCESS_GAMMA = MathUtils.clamp((float) data[0], Constants.MIN_GAMMA, Constants.MAX_GAMMA);
                break;
            case TONEMAPPING_TYPE_CMD:
                ToneMapping newTM;
                if (data[0] instanceof String) {
                    newTM = ToneMapping.valueOf(((String) data[0]).toUpperCase());
                } else {
                    newTM = (ToneMapping) data[0];
                }
                POSTPROCESS_TONEMAPPING_TYPE = newTM;
                break;
            case EXPOSURE_CMD:
                POSTPROCESS_EXPOSURE = MathUtilsd.clamp((float) data[0], Constants.MIN_EXPOSURE, Constants.MAX_EXPOSURE);
                break;
            default:
                break;
            }
        }

    }

    public static class ControlsConf implements IConf {
        public String CONTROLLER_MAPPINGS_FILE;
        public boolean INVERT_LOOK_Y_AXIS;
        public boolean DEBUG_MODE;
        /**
         * Controller name blacklist. Check out names in the preferences window.
         **/
        public String[] CONTROLLER_BLACKLIST;

        public ControlsConf() {

        }

        public void initialize(String cONTROLLER_MAPPINGS_FILE, boolean iNVERT_LOOK_Y_AXIS, boolean dEBUG_MODE, String[] cONTROLLER_BLACKLIST) {
            CONTROLLER_MAPPINGS_FILE = cONTROLLER_MAPPINGS_FILE;
            INVERT_LOOK_Y_AXIS = iNVERT_LOOK_Y_AXIS;
            DEBUG_MODE = dEBUG_MODE;
            CONTROLLER_BLACKLIST = cONTROLLER_BLACKLIST;

        }

        public boolean isControllerBlacklisted(String controllerName) {
            if (CONTROLLER_BLACKLIST == null || CONTROLLER_BLACKLIST.length == 0) {
                return false;
            } else {
                for (String cn : CONTROLLER_BLACKLIST) {
                    if (controllerName.equalsIgnoreCase(cn))
                        return true;
                }
            }
            return false;
        }

        public void addControllerListener(ControllerListener listener) {
            Array<Controller> controllers = Controllers.getControllers();
            for (Controller controller : controllers) {
                if (!isControllerBlacklisted(controller.getName())) {
                    // Prevent duplicates
                    controller.removeListener(listener);
                    // Add
                    controller.addListener(listener);
                }
            }
        }

        public void removeControllerListener(ControllerListener listener) {
            Array<Controller> controllers = Controllers.getControllers();
            for (Controller controller : controllers) {
                if (!isControllerBlacklisted(controller.getName())) {
                    controller.removeListener(listener);
                }
            }
        }
    }

    /**
     * Runtime configuration values, which are never persisted.
     *
     * @author Toni Sagrista
     */
    public static class RuntimeConf implements IConf, IObserver {
        /** Whether the connection to OpenVR has succeeded and the context has been created **/
        public boolean OPENVR = false;
        public boolean OVR = false;
        public boolean DISPLAY_GUI;
        public boolean UPDATE_PAUSE;
        public boolean TIME_ON;
        /**
         * Whether we use the RealTimeClock or the GlobalClock
         **/
        public boolean REAL_TIME;
        public boolean INPUT_ENABLED;
        public boolean RECORD_CAMERA;
        public boolean RECORD_KEYFRAME_CAMERA;
        public float LIMIT_MAG_RUNTIME;
        /**
         * Whether octree drawing is active or not
         **/
        public boolean DRAW_OCTREE;
        public boolean RELATIVISTIC_ABERRATION = false;
        public boolean GRAVITATIONAL_WAVES = false;

        public boolean DISPLAY_VR_GUI = false;

        public RuntimeConf() {
            EventManager.instance.subscribe(this, Events.LIMIT_MAG_CMD, Events.INPUT_ENABLED_CMD, Events.DISPLAY_GUI_CMD, Events.TOGGLE_UPDATEPAUSE, Events.TIME_STATE_CMD, Events.RECORD_CAMERA_CMD, Events.GRAV_WAVE_START, Events.GRAV_WAVE_STOP, Events.DISPLAY_VR_GUI_CMD);
        }

        public void initialize(boolean dISPLAY_GUI, boolean uPDATE_PAUSE, boolean tIME_ON, boolean iNPUT_ENABLED, boolean rECORD_CAMERA, float lIMIT_MAG_RUNTIME, boolean rEAL_TIME, boolean dRAW_OCTREE) {
            DISPLAY_GUI = dISPLAY_GUI;
            UPDATE_PAUSE = uPDATE_PAUSE;
            TIME_ON = tIME_ON;
            INPUT_ENABLED = iNPUT_ENABLED;
            RECORD_CAMERA = rECORD_CAMERA;
            LIMIT_MAG_RUNTIME = lIMIT_MAG_RUNTIME;
            REAL_TIME = rEAL_TIME;
            DRAW_OCTREE = dRAW_OCTREE;
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case LIMIT_MAG_CMD:
                LIMIT_MAG_RUNTIME = (float) data[0];
                AbstractRenderSystem.POINT_UPDATE_FLAG = true;
                break;

            case INPUT_ENABLED_CMD:
                INPUT_ENABLED = (boolean) data[0];
                break;

            case DISPLAY_GUI_CMD:
                DISPLAY_GUI = (boolean) data[0];
                break;
            case DISPLAY_VR_GUI_CMD:
                if (data.length > 1) {
                    Boolean val = (Boolean) data[1];
                    DISPLAY_VR_GUI = val;
                } else {
                    DISPLAY_VR_GUI = !DISPLAY_VR_GUI;
                }
                break;
            case TOGGLE_UPDATEPAUSE:
                UPDATE_PAUSE = !UPDATE_PAUSE;
                EventManager.instance.post(Events.UPDATEPAUSE_CHANGED, UPDATE_PAUSE);
                break;
            case TIME_STATE_CMD:
                toggleTimeOn((Boolean) data[0]);
                break;
            case RECORD_CAMERA_CMD:
                toggleRecord((Boolean) data[0]);
                break;
            case GRAV_WAVE_START:
                GRAVITATIONAL_WAVES = true;
                break;
            case GRAV_WAVE_STOP:
                GRAVITATIONAL_WAVES = false;
                break;
            default:
                break;

            }

        }

        /**
         * Toggles the time
         */
        public void toggleTimeOn(Boolean timeOn) {
            if (timeOn != null) {
                TIME_ON = timeOn;
            } else {
                TIME_ON = !TIME_ON;
            }
        }

        /**
         * Toggles the record camera
         */
        public void toggleRecord(Boolean rec) {
            if (rec != null) {
                RECORD_CAMERA = rec;
            } else {
                RECORD_CAMERA = !RECORD_CAMERA;
            }
        }

    }

    /**
     * Holds the configuration for the output frame subsystem and the camera
     * recording.
     *
     * @author Toni Sagrista
     */
    public static class FrameConf implements IConf, IObserver {
        public static final int MIN_FRAME_SIZE = 50;
        public static final int MAX_FRAME_SIZE = 25000;

        /**
         * The width of the image frames
         **/
        public int RENDER_WIDTH;
        /**
         * The height of the image frames
         **/
        public int RENDER_HEIGHT;
        /**
         * The number of images per second to produce
         **/
        public int RENDER_TARGET_FPS;
        /**
         * The target FPS when recording the camera
         **/
        public int CAMERA_REC_TARGET_FPS;
        /**
         * Automatically activate frame output system when playing camera file
         **/
        public boolean AUTO_FRAME_OUTPUT_CAMERA_PLAY;
        /**
         * The output folder
         **/
        public String RENDER_FOLDER;
        /**
         * The prefix for the image files
         **/
        public String RENDER_FILE_NAME;
        /**
         * Should we write the simulation time to the images?
         **/
        public boolean RENDER_SCREENSHOT_TIME;
        /**
         * Whether the frame system is activated or not
         **/
        public boolean RENDER_OUTPUT = false;
        /**
         * The frame output screenshot mode
         **/
        public ScreenshotMode FRAME_MODE;
        /**
         * Format
         **/
        public ImageFormat FRAME_FORMAT;
        /**
         * Quality, in case format is JPG
         **/
        public float FRAME_QUALITY;

        /**
         * Path type of camera position
         **/
        public CameraKeyframeManager.PathType KF_PATH_TYPE_POSITION;
        /**
         * Path type of camera orientation
         **/
        public CameraKeyframeManager.PathType KF_PATH_TYPE_ORIENTATION;

        public FrameConf() {
            EventManager.instance.subscribe(this, Events.CONFIG_FRAME_OUTPUT_CMD, Events.FRAME_OUTPUT_CMD);
        }

        public boolean isSimpleMode() {
            return FRAME_MODE.equals(ScreenshotMode.simple);
        }

        public boolean isRedrawMode() {
            return FRAME_MODE.equals(ScreenshotMode.redraw);
        }

        public void initialize(int rENDER_WIDTH, int rENDER_HEIGHT, int rENDER_TARGET_FPS, int cAMERA_REC_TARGET_FPS, boolean aUTO_FRAME_OUTPUT_CAMERA_PLAY, String rENDER_FOLDER, String rENDER_FILE_NAME, boolean rENDER_SCREENSHOT_TIME, boolean rENDER_OUTPUT, ScreenshotMode fRAME_MODE, ImageFormat fRAME_FORMAT, float fRAME_QUALITY, CameraKeyframeManager.PathType kF_PATH_TYPE_POSITION, CameraKeyframeManager.PathType kF_PATH_TYPE_ORIENTATION) {
            RENDER_WIDTH = rENDER_WIDTH;
            RENDER_HEIGHT = rENDER_HEIGHT;
            RENDER_TARGET_FPS = rENDER_TARGET_FPS;
            CAMERA_REC_TARGET_FPS = cAMERA_REC_TARGET_FPS;
            AUTO_FRAME_OUTPUT_CAMERA_PLAY = aUTO_FRAME_OUTPUT_CAMERA_PLAY;
            RENDER_FOLDER = rENDER_FOLDER;
            RENDER_FILE_NAME = rENDER_FILE_NAME;
            RENDER_SCREENSHOT_TIME = rENDER_SCREENSHOT_TIME;
            RENDER_OUTPUT = rENDER_OUTPUT;
            FRAME_MODE = fRAME_MODE;
            FRAME_FORMAT = fRAME_FORMAT;
            FRAME_QUALITY = fRAME_QUALITY;
            KF_PATH_TYPE_ORIENTATION = kF_PATH_TYPE_ORIENTATION;
            KF_PATH_TYPE_POSITION = kF_PATH_TYPE_POSITION;
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case CONFIG_FRAME_OUTPUT_CMD:
                boolean updateFrameSize = RENDER_WIDTH != (int) data[0] || RENDER_HEIGHT != (int) data[1];
                RENDER_WIDTH = (int) data[0];
                RENDER_HEIGHT = (int) data[1];
                RENDER_TARGET_FPS = (int) data[2];
                RENDER_FOLDER = (String) data[3];
                RENDER_FILE_NAME = (String) data[4];

                if (updateFrameSize) {
                    EventManager.instance.post(Events.FRAME_SIZE_UDPATE, RENDER_WIDTH, RENDER_HEIGHT);
                }
                break;
            case FRAME_OUTPUT_MODE_CMD:
                Object newMode = data[0];
                ScreenshotMode mode = null;
                if (newMode instanceof String) {
                    try {
                        mode = ScreenshotMode.valueOf((String) newMode);
                    } catch (IllegalArgumentException e) {
                        logger.error("Given value is not a representation of ScreenshotMode (simple|redraw): '" + newMode + "'");
                    }
                } else {
                    mode = (ScreenshotMode) newMode;
                }
                if (mode != null) {
                    FRAME_MODE = mode;
                }
                break;
            case FRAME_OUTPUT_CMD:
                RENDER_OUTPUT = (Boolean) data[0];
                // Flush buffer if needed
                if (!RENDER_OUTPUT && GaiaSky.instance != null) {
                    EventManager.instance.post(Events.FLUSH_FRAMES);
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * Holds all configuration values related to data.
     *
     * @author Toni Sagrista
     */
    public static class DataConf implements IConf {

        /**
         * Location of the data folder. Usually within the '.gaiasky' folder in the user's home directory in Windows
         * and macOS, or in ~/.local/share/gaiasky in Linux
         **/
        public String DATA_LOCATION;

        /**
         * The json data file in case of local data source
         **/
        public String OBJECTS_JSON_FILES;

        /**
         * The json file with the catalogue(s) to load
         **/
        public Array<String> CATALOG_JSON_FILES;

        /**
         * High accuracy positions for planets and moon - use all terms of
         * VSOP87 and moon algorithms
         **/
        public boolean HIGH_ACCURACY_POSITIONS;
        /**
         * Limit magnitude used for loading stars. All stars above this
         * magnitude will not even be loaded by the sandbox.
         **/
        public float LIMIT_MAG_LOAD;
        /**
         * Whether to use the real attitude of Gaia or the NSL approximation
         **/
        public boolean REAL_GAIA_ATTITUDE;

        /** Location of the skybox within the data folder (data/tex/skybox/[...]/) **/
        public String SKYBOX_LOCATION;

        public void initialize(String dATA_LOCATION, Array<String> cATALOG_JSON_FILES, String oBJECTS_JSON_FILE, float lIMIT_MAG_LOAD, boolean rEAL_GAIA_ATTITUDE, boolean hIGH_ACCURACY_POSITIONS, String sKYBOX_LOCATION) {

            DATA_LOCATION = dATA_LOCATION;
            CATALOG_JSON_FILES = cATALOG_JSON_FILES;
            OBJECTS_JSON_FILES = oBJECTS_JSON_FILE;
            LIMIT_MAG_LOAD = lIMIT_MAG_LOAD;
            REAL_GAIA_ATTITUDE = rEAL_GAIA_ATTITUDE;
            HIGH_ACCURACY_POSITIONS = hIGH_ACCURACY_POSITIONS;
            SKYBOX_LOCATION = sKYBOX_LOCATION;
        }

        public void initialize(Array<String> cATALOG_JSON_FILES, String oBJECTS_JSON_FILE, boolean dATA_SOURCE_LOCAL, float lIMIT_MAG_LOAD) {
            this.CATALOG_JSON_FILES = cATALOG_JSON_FILES;
            this.OBJECTS_JSON_FILES = oBJECTS_JSON_FILE;
            this.LIMIT_MAG_LOAD = lIMIT_MAG_LOAD;
        }

        public void initialize(Array<String> cATALOG_JSON_FILES, String dATA_JSON_FILE, boolean dATA_SOURCE_LOCAL, float lIMIT_MAG_LOAD, boolean rEAL_GAIA_ATTITUDE) {
            this.CATALOG_JSON_FILES = cATALOG_JSON_FILES;
            this.OBJECTS_JSON_FILES = dATA_JSON_FILE;
            this.LIMIT_MAG_LOAD = lIMIT_MAG_LOAD;
            this.REAL_GAIA_ATTITUDE = rEAL_GAIA_ATTITUDE;
        }

        public String dataFile(String path) {
            String pth = path.replace('*', 'X');
            if (Paths.get(pth).isAbsolute()) {
                // Absolute path, just leave it
                return path.replaceAll("\\\\", "/");
            } else {
                // Relative path, just remove leading 'data/' and prepend data location
                if (path.startsWith("data/")) {
                    path = path.substring(5);
                }
                Path p = Paths.get(DATA_LOCATION);
                return (p.toString() + File.separator + path).replaceAll("\\\\", "/");
            }
        }

        public FileHandle dataFileHandle(String path) {
            return new FileHandle(dataFile(path));
        }
    }

    public static class ScreenConf implements IConf {

        public int SCREEN_WIDTH;
        public int SCREEN_HEIGHT;
        public float BACKBUFFER_SCALE = 1f;
        public int BACKBUFFER_WIDTH;
        public int BACKBUFFER_HEIGHT;
        public int FULLSCREEN_WIDTH;
        public int FULLSCREEN_HEIGHT;
        public boolean FULLSCREEN;
        public boolean RESIZABLE;
        public boolean VSYNC;
        public int LIMIT_FPS;
        public boolean SCREEN_OUTPUT = true;

        public void initialize(int sCREEN_WIDTH, int sCREEN_HEIGHT, int fULLSCREEN_WIDTH, int fULLSCREEN_HEIGHT, boolean fULLSCREEN, boolean rESIZABLE, boolean vSYNC, boolean sCREEN_OUTPUT, int lIMIT_FPS) {
            SCREEN_WIDTH = sCREEN_WIDTH;
            SCREEN_HEIGHT = sCREEN_HEIGHT;
            BACKBUFFER_WIDTH = sCREEN_WIDTH;
            BACKBUFFER_HEIGHT = sCREEN_HEIGHT;
            FULLSCREEN_WIDTH = fULLSCREEN_WIDTH;
            FULLSCREEN_HEIGHT = fULLSCREEN_HEIGHT;
            FULLSCREEN = fULLSCREEN;
            RESIZABLE = rESIZABLE;
            VSYNC = vSYNC;
            SCREEN_OUTPUT = sCREEN_OUTPUT;
            LIMIT_FPS = lIMIT_FPS;
        }

        public int getScreenWidth() {
            return FULLSCREEN ? FULLSCREEN_WIDTH : SCREEN_WIDTH;
        }

        public int getScreenHeight() {
            return FULLSCREEN ? FULLSCREEN_HEIGHT : SCREEN_HEIGHT;
        }

        public void resize(int w, int h) {
            if (FULLSCREEN) {
                FULLSCREEN_WIDTH = w;
                FULLSCREEN_HEIGHT = h;
            } else {
                SCREEN_WIDTH = w;
                SCREEN_HEIGHT = h;
            }
        }

    }

    public static class ProgramConf implements IConf, IObserver {

        public enum StereoProfile {
            /**
             * Left image -> left eye, distortion
             **/
            VR_HEADSET,
            /**
             * Left image -> left eye, distortion
             **/
            HD_3DTV_HORIZONTAL,
            /**
             * Top-bottom
             **/
            HD_3DTV_VERTICAL,
            /**
             * Left image -> right eye, no distortion
             **/
            CROSSEYE,
            /**
             * Left image -> left eye, no distortion
             **/
            PARALLEL_VIEW,
            /**
             * Red-cyan anaglyphic 3D mode
             **/
            ANAGLYPHIC;

            public boolean isHorizontal() {
                return this.equals(VR_HEADSET) || this.equals(HD_3DTV_HORIZONTAL) || this.equals(CROSSEYE) || this.equals(PARALLEL_VIEW);
            }

            public boolean isVertical() {
                return this.equals(HD_3DTV_VERTICAL);
            }

            public boolean isAnaglyphic() {
                return this.equals(ANAGLYPHIC);
            }

            public boolean correctAspect() {
                return !this.equals(HD_3DTV_HORIZONTAL) && !this.equals(ANAGLYPHIC);
            }
        }

        /**
         * In a client-server configuration, this instance of Gaia Sky acts as a slave and
         * receives the state over the network if this is set to true
         */
        public boolean NET_SLAVE = false;
        /**
         * In a client-server configuration, this instance of Gaia Sky acts as a master and
         * sends the state over the network to the slaves if this is set to true
         */
        public boolean NET_MASTER = false;
        /**
         * List of slave URL locations. Only relevant if {{@link #NET_MASTER} is true
         */
        public List<String> NET_MASTER_SLAVES;

        /**
         * If this is a slave, this entry contains the path to the MPCDI configuration file
         * for this instance
         */
        public String NET_SLAVE_CONFIG;
        /**
         * Slave yaw angle (turn head right), in degrees
         */
        public float NET_SLAVE_YAW;
        /**
         * Slave pitch angle (turn head up), in degrees
         */
        public float NET_SLAVE_PITCH;
        /**
         * Slave roll angle (rotate head cw), in degrees
         */
        public float NET_SLAVE_ROLL;
        /** Warp PFM file **/
        public String NET_SLAVE_WARP;
        /** Blend PNG file **/
        public String NET_SLAVE_BLEND;

        public boolean SHOW_DEBUG_INFO;

        // Update checker
        public static long VERSION_CHECK_INTERVAL_MS = 1 * 24 * 60 * 60 * 1000;
        public Instant VERSION_LAST_TIME;
        public String VERSION_LAST_VERSION;
        public String VERSION_CHECK_URL;

        public String DATA_DESCRIPTOR_URL;
        public String UI_THEME;
        public String SCRIPT_LOCATION;
        public String LAST_OPEN_LOCATION;
        public int REST_PORT;
        public String LOCALE;
        public boolean DISPLAY_HUD;
        public boolean DISPLAY_POINTER_COORDS;
        public boolean DISPLAY_MINIMAP;
        public float MINIMAP_SIZE;
        public boolean MINIMAP_IN_WINDOW = false;
        public boolean CUBEMAP_MODE;
        public float PLANETARIUM_APERTURE;
        /**
         * Cubemap projection
         **/
        public CubemapProjections.CubemapProjection CUBEMAP_PROJECTION = CubemapProjections.CubemapProjection.EQUIRECTANGULAR;
        public boolean STEREOSCOPIC_MODE;
        /** Eye separation in stereoscopic mode in meters **/
        public float STEREOSCOPIC_EYE_SEPARATION_M = 1f;
        /** This controls the side of the images in the stereoscopic mode **/
        public StereoProfile STEREO_PROFILE = StereoProfile.VR_HEADSET;
        /**
         * Whether to display the dataset dialog at startup or not
         **/
        public boolean DISPLAY_DATASET_DIALOG;

        public ProgramConf() {
            EventManager.instance.subscribe(this, Events.STEREOSCOPIC_CMD, Events.STEREO_PROFILE_CMD, Events.CUBEMAP_CMD, Events.CUBEMAP_PROJECTION_CMD, Events.SHOW_MINIMAP_ACTION, Events.TOGGLE_MINIMAP, Events.PLANETARIUM_APERTURE_CMD);
        }

        public void initialize(boolean sHOW_DEBUG_INFO, Instant lAST_CHECKED, String lAST_VERSION, String vERSION_CHECK_URL, String dATA_DESCRIPTOR_URL, String uI_THEME, String sCRIPT_LOCATION, int rEST_PORT, String lOCALE, boolean sTEREOSCOPIC_MODE, StereoProfile sTEREO_PROFILE, boolean cUBEMAP360_MODE, boolean dISPLAY_HUD, boolean dISPLAY_POINTER_COORDS, boolean dISPLAY_DATASET_DIALOG, boolean nET_MASTER, boolean nET_SLAVE, List<String> nET_MASTER_SLAVES, String nET_SLAVE_CONFIG,
                float nET_SLAVE_YAW, float nET_SLAVE_PITCH, float nET_SLAVE_ROLL, String nET_SLAVE_WARP, String nET_SLAVE_BLEND, String lAST_OPEN_LOCATION, boolean dISPLAY_MINIMAP, float mINIMAP_SIZE, float pLANETARIUM_APERTURE) {
            SHOW_DEBUG_INFO = sHOW_DEBUG_INFO;
            VERSION_LAST_TIME = lAST_CHECKED;
            VERSION_LAST_VERSION = lAST_VERSION;
            VERSION_CHECK_URL = vERSION_CHECK_URL;
            DATA_DESCRIPTOR_URL = dATA_DESCRIPTOR_URL;
            UI_THEME = uI_THEME;
            SCRIPT_LOCATION = sCRIPT_LOCATION;
            REST_PORT = rEST_PORT;
            LOCALE = lOCALE;
            STEREOSCOPIC_MODE = sTEREOSCOPIC_MODE;
            STEREO_PROFILE = sTEREO_PROFILE;
            CUBEMAP_MODE = cUBEMAP360_MODE;
            DISPLAY_HUD = dISPLAY_HUD;
            DISPLAY_POINTER_COORDS = dISPLAY_POINTER_COORDS;
            DISPLAY_DATASET_DIALOG = dISPLAY_DATASET_DIALOG;
            NET_MASTER = nET_MASTER;
            NET_SLAVE = nET_SLAVE;
            NET_MASTER_SLAVES = nET_MASTER_SLAVES;
            NET_SLAVE_CONFIG = nET_SLAVE_CONFIG;
            NET_SLAVE_PITCH = nET_SLAVE_PITCH;
            NET_SLAVE_YAW = nET_SLAVE_YAW;
            NET_SLAVE_ROLL = nET_SLAVE_ROLL;
            NET_SLAVE_WARP = nET_SLAVE_WARP;
            NET_SLAVE_BLEND = nET_SLAVE_BLEND;
            LAST_OPEN_LOCATION = lAST_OPEN_LOCATION;
            DISPLAY_MINIMAP = dISPLAY_MINIMAP;
            MINIMAP_SIZE = mINIMAP_SIZE;
            PLANETARIUM_APERTURE = pLANETARIUM_APERTURE;
        }

        public void initialize(boolean sHOW_DEBUG_INFO, String uI_THEME, String lOCALE, boolean sTEREOSCOPIC_MODE, StereoProfile sTEREO_PROFILE) {
            SHOW_DEBUG_INFO = sHOW_DEBUG_INFO;
            UI_THEME = uI_THEME;
            LOCALE = lOCALE;
            STEREOSCOPIC_MODE = sTEREOSCOPIC_MODE;
            STEREO_PROFILE = sTEREO_PROFILE;
        }

        public String getLastCheckedString() {
            IDateFormat df = DateFormatFactory.getFormatter(I18n.locale, DateType.DATETIME);
            return df.format(VERSION_LAST_TIME);
        }

        public boolean isStereoHalfWidth() {
            return STEREOSCOPIC_MODE && STEREO_PROFILE.correctAspect();
        }

        public boolean isStereoFullWidth() {
            return !isStereoHalfWidth();
        }

        public boolean isStereoHalfViewport() {
            return STEREOSCOPIC_MODE && STEREO_PROFILE != StereoProfile.ANAGLYPHIC;
        }

        /**
         * Returns whether the UI theme is in night mode
         *
         * @return
         */
        public boolean isUINightMode() {
            return UI_THEME.contains("night");
        }

        /**
         * Gets the name of the UI theme without the "-x2" suffix
         *
         * @return The base UI theme name
         */
        public String getUIThemeBase() {
            if (UI_THEME.endsWith("-x2")) {
                return UI_THEME.substring(0, UI_THEME.length() - 3);
            } else {
                return UI_THEME;
            }
        }

        public boolean isHiDPITheme() {
            return UI_THEME.endsWith("-x2");
        }

        public String getNetName() {
            if (NET_MASTER)
                return " MASTER";
            else if (NET_SLAVE)
                return " SLAVE";
            return "";
        }

        public boolean isMaster() {
            return NET_MASTER;
        }

        public boolean isSlave() {
            return NET_SLAVE;
        }

        /**
         * Checks whether the MPCDI configuration file for this slave is set
         * @return Whether the MPCDI file for this slave is set
         */
        public boolean isSlaveMPCDIPresent(){
            return NET_SLAVE_CONFIG != null && !NET_SLAVE_CONFIG.isEmpty();
        }

        /**
         * Checks whether the slave is configured directly in the properties file
         * of Gaia Sky
         * @return Whether the slave is configured in the properties file
         */
        public boolean areSlaveConfigPropertiesPresent(){
            return !Float.isNaN(NET_SLAVE_YAW) && !Float.isNaN(NET_SLAVE_PITCH) && !Float.isNaN(NET_SLAVE_ROLL);
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case STEREOSCOPIC_CMD:
                if (!GaiaSky.instance.cam.mode.isGaiaFov()) {
                    boolean stereomode = (Boolean) data[0];
                    STEREOSCOPIC_MODE = stereomode;
                    if (STEREOSCOPIC_MODE && CUBEMAP_MODE) {
                        CUBEMAP_MODE = false;
                        EventManager.instance.post(Events.DISPLAY_GUI_CMD, true, I18n.bundle.get("notif.cleanmode"));
                    }
                }
                break;
            case STEREO_PROFILE_CMD:
                STEREO_PROFILE = StereoProfile.values()[(Integer) data[0]];
                break;
            case CUBEMAP_CMD:
                CUBEMAP_MODE = (Boolean) data[0];
                if (CUBEMAP_MODE) {
                    CUBEMAP_PROJECTION = (CubemapProjections.CubemapProjection) data[1];

                    // Post a message to the screen
                    ModePopupInfo mpi = new ModePopupInfo();
                    if (CUBEMAP_PROJECTION.isPanorama()) {
                        mpi.title = "Panorama mode";
                        mpi.header = "You have entered Panorama mode!";
                        mpi.addMapping("Back to normal mode", "CTRL", "K");
                        mpi.addMapping("Switch projection type", "CTRL", "SHIFT", "K");
                    } else if (CUBEMAP_PROJECTION.isPlanetarium()) {
                        mpi.title = "Planetarium mode";
                        mpi.header = "You have entered Planetarium mode!";
                        mpi.addMapping("Back to normal mode", "CTRL", "P");
                    }

                    EventManager.instance.post(Events.MODE_POPUP_CMD, mpi, 120f);
                }
                break;
            case CUBEMAP_PROJECTION_CMD:
                CUBEMAP_PROJECTION = (CubemapProjections.CubemapProjection) data[0];
                logger.info("Cubemap projection set to " + CUBEMAP_PROJECTION.toString());
                break;
            case SHOW_MINIMAP_ACTION:
                boolean show = (Boolean) data[0];
                DISPLAY_MINIMAP = show;
                break;
            case TOGGLE_MINIMAP:
                DISPLAY_MINIMAP = !DISPLAY_MINIMAP;
                break;
            case PLANETARIUM_APERTURE_CMD:
                PLANETARIUM_APERTURE = (float) data[0];
                break;
            default:
                break;
            }
        }

    }

    public static class SpacecraftConf implements IConf {
        public float SC_RESPONSIVENESS;
        public boolean SC_VEL_TO_DIRECTION;
        public float SC_HANDLING_FRICTION;
        public boolean SC_SHOW_AXES;

        public void initialize(float sC_RESPONSIVENESS, boolean sC_VEL_TO_DIRECTION, float sC_HANDLING_FRICTION, boolean sC_SHOW_AXES) {
            this.SC_RESPONSIVENESS = sC_RESPONSIVENESS;
            this.SC_VEL_TO_DIRECTION = sC_VEL_TO_DIRECTION;
            this.SC_HANDLING_FRICTION = sC_HANDLING_FRICTION;
            this.SC_SHOW_AXES = sC_SHOW_AXES;
        }
    }

    public static class VersionConf implements IConf {
        public String version;
        public int versionNumber;
        public Instant buildtime;
        public String builder;
        public String system;
        public String build;

        public void initialize(String version, Instant buildtime, String builder, String system, String build) {
            this.version = version;
            this.versionNumber = VersionChecker.stringToVersionNumber(version);
            this.buildtime = buildtime;
            this.builder = builder;
            this.system = system;
            this.build = build;
        }

        @Override
        public String toString() {
            return version;
        }
    }

    /**
     * Contains preferences and attributes which define the scene
     */
    public static class SceneConf implements IConf, IObserver {

        /**
         * The type of elevation representation if elevation textures
         * are present
         */
        public enum ElevationType {
            TESSELLATION,
            PARALLAX_MAPPING,
            NONE;

            public boolean isTessellation() {
                return this.equals(TESSELLATION);
            }

            public boolean isParallaxMapping() {
                return this.equals(PARALLAX_MAPPING);
            }

            public boolean isNone() {
                return this.equals(NONE);
            }
        }

        /**
         * Graphics quality setting
         */
        public enum GraphicsQuality {
            LOW("gui.gquality.low", "-low", 1024, 512),
            NORMAL("gui.gquality.normal", "-med", 2048, 1024),
            HIGH("gui.gquality.high", "-high", 4096, 2048),
            ULTRA("gui.gquality.ultra", "-ultra", 8192, 4096);

            public String key;
            public String suffix;
            public int texWidthTarget, texHeightTarget;

            GraphicsQuality(String key, String suffix, int texWidthTarget, int texHeightTarget) {
                this.key = key;
                this.suffix = suffix;
                this.texWidthTarget = texWidthTarget;
                this.texHeightTarget = texHeightTarget;
            }

            public boolean isAtLeast(GraphicsQuality gq) {
                return this.ordinal() >= gq.ordinal();
            }

            public boolean isAtMost(GraphicsQuality gq) {
                return this.ordinal() <= gq.ordinal();
            }

            public boolean isLow() {
                return this.equals(LOW);
            }

            public boolean isNormal() {
                return this.equals(NORMAL);
            }

            public boolean isHigh() {
                return this.equals(HIGH);
            }

            public boolean isUltra() {
                return this.equals(ULTRA);
            }

            public int getGlowNLights() {
                if (isLow()) {
                    return 10;
                } else if (isNormal()) {
                    return 20;
                } else if (isHigh()) {
                    return 30;
                } else if (isUltra()) {
                    return 40;
                }
                return 20;
            }
        }

        public String STARTUP_OBJECT;
        public long OBJECT_FADE_MS;
        public double AMBIENT_LIGHT;
        public float CAMERA_FOV;
        public double CAMERA_SPEED;
        public double TURNING_SPEED;
        public double ROTATION_SPEED;
        public boolean FREE_CAMERA_TARGET_MODE_ON;
        public int CAMERA_SPEED_LIMIT_IDX;
        public double CAMERA_SPEED_LIMIT;
        public boolean FOCUS_LOCK;
        public boolean FOCUS_LOCK_ORIENTATION;
        public boolean CINEMATIC_CAMERA;
        public float LABEL_SIZE_FACTOR;
        public float LABEL_NUMBER_FACTOR;
        /**
         * Visibility of component types
         **/
        public boolean[] VISIBILITY;

        /**
         * Display galaxy as 3D object or as a 2D texture
         **/
        public boolean GALAXY_3D;

        /**
         * Shadows enabled or disabled
         **/
        public boolean SHADOW_MAPPING;
        /**
         * Max number of objects with shadows
         **/
        public int SHADOW_MAPPING_N_SHADOWS;
        /**
         * Resolution of the shadow map
         **/
        public int SHADOW_MAPPING_RESOLUTION;

        /**
         * Factor to apply to the length of the proper motion vectors
         **/
        public float PM_LEN_FACTOR;
        /**
         * This governs the number of proper motion vectors to display
         **/
        public float PM_NUM_FACTOR;

        /**
         * Overrides the maximum number of proper motion vectors that are allowed to show
         */
        public long N_PM_STARS;

        /**
         * Color mode for velocity vectors
         * <ul>
         * <li>0 - direction</li>
         * <li>1 - length</li>
         * <li>2 - has radial velocity</li>
         * <li>3 - redshift (sun)</li>
         * <li>4 - redshift (camera)</li>
         * <li>5 - single color</li>
         * </ul>
         */
        public int PM_COLOR_MODE;

        /**
         * Whether to show arrow caps or not
         */
        public boolean PM_ARROWHEADS;

        public boolean STAR_COLOR_TRANSIT;
        public boolean ONLY_OBSERVED_STARS;
        public boolean COMPUTE_GAIA_SCAN;
        /**
         * Whether to use the general line renderer or a faster GPU-based
         * approach
         * <ul>
         * <li>0 - Sorted lines (see LINE_RENDERER) - slower but looks
         * better</li>
         * <li>1 - GPU VBO - faster but looks a bit worse</li>
         * </ul>
         */
        public int ORBIT_RENDERER;
        /**
         * The line render system: 0 - normal, 1 - shader
         **/
        public int LINE_RENDERER;
        /**
         * The graphics quality
         **/
        public GraphicsQuality GRAPHICS_QUALITY;

        /**
         * Lazy texture initialisation - textures are loaded only if needed
         **/
        public boolean LAZY_TEXTURE_INIT;

        /**
         * Initializes meshes lazily
         **/
        public boolean LAZY_MESH_INIT;

        /**
         * How to represent elevation, if elevation textures present:
         * <ul>
         * <li>Tessellation</li>
         * <li>Parallax mapping</li>
         * <li>None</li>
         * </ul>
         */
        public ElevationType ELEVATION_TYPE;

        /** Elevation multiplier **/
        public double ELEVATION_MULTIPLIER;

        /** Quality of tessellation **/
        public double TESSELLATION_QUALITY;

        /** Whether to show the focus crosshair **/
        public boolean CROSSHAIR_FOCUS;
        /** Closest object crosshair **/
        public boolean CROSSHAIR_CLOSEST;
        /** Home object crosshair **/
        public boolean CROSSHAIR_HOME;

        /**
         * Resolution of each of the faces in the cubemap which will be mapped
         * to a equirectangular projection for the 360 mode.
         */
        public int CUBEMAP_FACE_RESOLUTION;

        public double STAR_THRESHOLD_NONE;
        public double STAR_THRESHOLD_POINT;
        public double STAR_THRESHOLD_QUAD;

        public float STAR_MIN_OPACITY;
        public float POINT_ALPHA_MAX;

        /**
         * Size of stars rendered as point primitives
         **/
        public float STAR_POINT_SIZE;
        /**
         * Fallback value
         **/
        public float STAR_POINT_SIZE_BAK;
        /**
         * Brightness of stars
         */
        public double STAR_BRIGHTNESS;
        /**
         * This value is used to modulate the star brightness as in final_brightness = brightness^power
         */
        public float STAR_BRIGHTNESS_POWER;

        /**
         * Particle fade in/out flag for octree-backed catalogs. WARNING: This
         * implies particles are sent to GPU at each cycle
         **/
        public boolean OCTREE_PARTICLE_FADE;

        /**
         * Angle [rad] above which we start painting stars in octant with fade
         * in
         **/
        public float OCTANT_THRESHOLD_0;
        /**
         * Angle [rad] below which we paint stars in octant with fade out. Above
         * this angle, inner stars are painted with full brightness
         **/
        public float OCTANT_THRESHOLD_1;

        /**
         * In the case of multifile LOD datasets (such as DR2+), this setting contains
         * the maximum number of stars loaded at a time. If the number of loaded stars
         * surpasses this setting, the system will start looking for the best candidates
         * to be unloaded and start unloading data. Should not be set too low, and this should
         * be balanced with the dataset and the draw distance.
         */
        public long MAX_LOADED_STARS;

        /** Distance scaling factor in desktop mode **/
        public double DIST_SCALE_DESKTOP;
        /** Distance scaling factor in VR mode **/
        public double DIST_SCALE_VR;

        public SceneConf() {
            EventManager.instance.subscribe(this, Events.TOGGLE_VISIBILITY_CMD, Events.FOCUS_LOCK_CMD, Events.ORIENTATION_LOCK_CMD, Events.STAR_BRIGHTNESS_CMD, Events.PM_LEN_FACTOR_CMD, Events.PM_NUM_FACTOR_CMD, Events.PM_COLOR_MODE_CMD, Events.PM_ARROWHEADS_CMD, Events.FOV_CHANGED_CMD, Events.CAMERA_SPEED_CMD, Events.ROTATION_SPEED_CMD, Events.TURNING_SPEED_CMD, Events.SPEED_LIMIT_CMD, Events.TRANSIT_COLOUR_CMD, Events.ONLY_OBSERVED_STARS_CMD, Events.COMPUTE_GAIA_SCAN_CMD, Events.OCTREE_PARTICLE_FADE_CMD, Events.STAR_POINT_SIZE_CMD, Events.STAR_POINT_SIZE_INCREASE_CMD, Events.STAR_POINT_SIZE_DECREASE_CMD, Events.STAR_POINT_SIZE_RESET_CMD, Events.STAR_MIN_OPACITY_CMD, Events.AMBIENT_LIGHT_CMD, Events.GALAXY_3D_CMD, Events.CROSSHAIR_FOCUS_CMD, Events.CROSSHAIR_CLOSEST_CMD, Events.CROSSHAIR_HOME_CMD, Events.CAMERA_CINEMATIC_CMD, Events.CUBEMAP_RESOLUTION_CMD, Events.LABEL_SIZE_CMD, Events.ELEVATION_MUTLIPLIER_CMD, Events.ELEVATION_TYPE_CMD, Events.TESSELLATION_QUALITY_CMD);
        }

        public void initialize(String sTARTUP_OBJECT, GraphicsQuality gRAPHICS_QUALITY, long oBJECT_FADE_MS, float sTAR_BRIGHTNESS, float sTAR_BRIGHTNESS_POWER, float aMBIENT_LIGHT, float cAMERA_FOV, float cAMERA_SPEED, float tURNING_SPEED, float rOTATION_SPEED, int cAMERA_SPEED_LIMIT_IDX, boolean fOCUS_LOCK, boolean fOCUS_LOCK_ORIENTATION, float lABEL_SIZE_FACTOR, float lABEL_NUMBER_FACTOR, boolean[] vISIBILITY, int oRBIT_RENDERER, int lINE_RENDERER, double sTAR_TH_ANGLE_NONE,
                double sTAR_TH_ANGLE_POINT, double sTAR_TH_ANGLE_QUAD, float pOINT_ALPHA_MIN, float pOINT_ALPHA_MAX, boolean oCTREE_PARTICLE_FADE, float oCTANT_TH_ANGLE_0, float oCTANT_TH_ANGLE_1, float pM_NUM_FACTOR, float pM_LEN_FACTOR, long n_PM_STARS, int pM_COLOR_MODE, boolean pM_ARROWHEADS, float sTAR_POINT_SIZE, boolean gALAXY_3D, int cUBEMAP_FACE_RESOLUTION, boolean cROSSHAIR_FOCUS, boolean cROSSHAIR_CLOSEST, boolean cROSSHAIR_HOME, boolean cINEMATIC_CAMERA, boolean lAZY_TEXTURE_INIT,
                boolean lAZY_MESH_INIT, boolean fREE_CAMERA_TARGET_MODE_ON, boolean sHADOW_MAPPING, int sHADOW_MAPPING_N_SHADOWS, int sHADOW_MAPPING_RESOLUTION, long mAX_LOADED_STARS, ElevationType eLEVATION_TYPE, double eLEVATION_MULTIPLIER, double tESSELLATION_QUALITY, double dIST_SCALE_DESKTOP, double dIST_SCALE_VR) {
            STARTUP_OBJECT = sTARTUP_OBJECT;
            GRAPHICS_QUALITY = gRAPHICS_QUALITY;
            OBJECT_FADE_MS = oBJECT_FADE_MS;
            STAR_BRIGHTNESS = sTAR_BRIGHTNESS;
            STAR_BRIGHTNESS_POWER = sTAR_BRIGHTNESS_POWER;
            AMBIENT_LIGHT = aMBIENT_LIGHT;
            CAMERA_FOV = cAMERA_FOV;
            CAMERA_SPEED = cAMERA_SPEED;
            TURNING_SPEED = tURNING_SPEED;
            ROTATION_SPEED = rOTATION_SPEED;
            FREE_CAMERA_TARGET_MODE_ON = fREE_CAMERA_TARGET_MODE_ON;
            CAMERA_SPEED_LIMIT_IDX = cAMERA_SPEED_LIMIT_IDX;
            this.updateSpeedLimit();
            FOCUS_LOCK = fOCUS_LOCK;
            FOCUS_LOCK_ORIENTATION = fOCUS_LOCK_ORIENTATION;
            LABEL_SIZE_FACTOR = lABEL_SIZE_FACTOR;
            LABEL_NUMBER_FACTOR = lABEL_NUMBER_FACTOR;
            VISIBILITY = vISIBILITY;
            ORBIT_RENDERER = oRBIT_RENDERER;
            LINE_RENDERER = lINE_RENDERER;
            STAR_THRESHOLD_NONE = sTAR_TH_ANGLE_NONE;
            STAR_THRESHOLD_POINT = sTAR_TH_ANGLE_POINT;
            STAR_THRESHOLD_QUAD = sTAR_TH_ANGLE_QUAD;
            STAR_MIN_OPACITY = pOINT_ALPHA_MIN;
            POINT_ALPHA_MAX = pOINT_ALPHA_MAX;
            OCTREE_PARTICLE_FADE = oCTREE_PARTICLE_FADE;
            OCTANT_THRESHOLD_0 = oCTANT_TH_ANGLE_0;
            OCTANT_THRESHOLD_1 = oCTANT_TH_ANGLE_1;
            PM_NUM_FACTOR = pM_NUM_FACTOR;
            PM_LEN_FACTOR = pM_LEN_FACTOR;
            N_PM_STARS = n_PM_STARS;
            PM_COLOR_MODE = pM_COLOR_MODE;
            PM_ARROWHEADS = pM_ARROWHEADS;
            STAR_POINT_SIZE = sTAR_POINT_SIZE;
            STAR_POINT_SIZE_BAK = STAR_POINT_SIZE;
            GALAXY_3D = gALAXY_3D;
            CUBEMAP_FACE_RESOLUTION = cUBEMAP_FACE_RESOLUTION;
            CROSSHAIR_FOCUS = cROSSHAIR_FOCUS;
            CROSSHAIR_CLOSEST = cROSSHAIR_CLOSEST;
            CROSSHAIR_HOME = cROSSHAIR_HOME;
            CINEMATIC_CAMERA = cINEMATIC_CAMERA;
            LAZY_TEXTURE_INIT = lAZY_TEXTURE_INIT;
            LAZY_MESH_INIT = lAZY_MESH_INIT;
            SHADOW_MAPPING = sHADOW_MAPPING;
            SHADOW_MAPPING_N_SHADOWS = sHADOW_MAPPING_N_SHADOWS;
            SHADOW_MAPPING_RESOLUTION = sHADOW_MAPPING_RESOLUTION;
            MAX_LOADED_STARS = mAX_LOADED_STARS;
            ELEVATION_TYPE = eLEVATION_TYPE;
            ELEVATION_MULTIPLIER = eLEVATION_MULTIPLIER;
            TESSELLATION_QUALITY = tESSELLATION_QUALITY;
            DIST_SCALE_DESKTOP = dIST_SCALE_DESKTOP;
            DIST_SCALE_VR = dIST_SCALE_VR;
        }

        public void updateSpeedLimit() {
            switch (CAMERA_SPEED_LIMIT_IDX) {
            case 0:
                // 100 km/h is 0.027 km/s
                CAMERA_SPEED_LIMIT = 0.0277777778 * Constants.KM_TO_U;
                break;
            case 1:
                CAMERA_SPEED_LIMIT = 0.5 * Constants.C * Constants.M_TO_U;
                break;
            case 2:
                CAMERA_SPEED_LIMIT = 0.8 * Constants.C * Constants.M_TO_U;
                break;
            case 3:
                CAMERA_SPEED_LIMIT = 0.9 * Constants.C * Constants.M_TO_U;
                break;
            case 4:
                CAMERA_SPEED_LIMIT = 0.99 * Constants.C * Constants.M_TO_U;
                break;
            case 5:
                CAMERA_SPEED_LIMIT = 0.99999 * Constants.C * Constants.M_TO_U;
                break;
            case 6:
                CAMERA_SPEED_LIMIT = Constants.C * Constants.M_TO_U;
                break;
            case 7:
                CAMERA_SPEED_LIMIT = 2.0 * Constants.C * Constants.M_TO_U;
                break;
            case 8:
                // 10 c
                CAMERA_SPEED_LIMIT = 10.0 * Constants.C * Constants.M_TO_U;
                break;
            case 9:
                // 1000 c
                CAMERA_SPEED_LIMIT = 1000.0 * Constants.C * Constants.M_TO_U;
                break;
            case 10:
                CAMERA_SPEED_LIMIT = 1.0 * Constants.AU_TO_U;
                break;
            case 11:
                CAMERA_SPEED_LIMIT = 10.0 * Constants.AU_TO_U;
                break;
            case 12:
                CAMERA_SPEED_LIMIT = 1000.0 * Constants.AU_TO_U;
                break;
            case 13:
                CAMERA_SPEED_LIMIT = 10000.0 * Constants.AU_TO_U;
                break;
            case 14:
                CAMERA_SPEED_LIMIT = Constants.PC_TO_U;
                break;
            case 15:
                CAMERA_SPEED_LIMIT = 2.0 * Constants.PC_TO_U;
                break;
            case 16:
                // 10 pc/s
                CAMERA_SPEED_LIMIT = 10.0 * Constants.PC_TO_U;
                break;
            case 17:
                // 1000 pc/s
                CAMERA_SPEED_LIMIT = 1000.0 * Constants.PC_TO_U;
                break;
            case 18:
                // No limit
                CAMERA_SPEED_LIMIT = -1;
                break;

            }
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case TOGGLE_VISIBILITY_CMD:
                String key = (String) data[0];
                Boolean state = null;
                if (data.length > 2) {
                    state = (Boolean) data[2];
                }
                ComponentType ct = ComponentType.getFromKey(key);
                if (ct != null) {
                    VISIBILITY[ct.ordinal()] = (state != null ? state : !VISIBILITY[ct.ordinal()]);
                }
                break;
            case TRANSIT_COLOUR_CMD:
                STAR_COLOR_TRANSIT = (boolean) data[1];
                break;
            case ONLY_OBSERVED_STARS_CMD:
                ONLY_OBSERVED_STARS = (boolean) data[1];
                break;
            case COMPUTE_GAIA_SCAN_CMD:
                COMPUTE_GAIA_SCAN = (boolean) data[1];
                break;
            case FOCUS_LOCK_CMD:
                FOCUS_LOCK = (boolean) data[1];
                break;
            case ORIENTATION_LOCK_CMD:
                FOCUS_LOCK_ORIENTATION = (boolean) data[1];
                break;
            case AMBIENT_LIGHT_CMD:
                AMBIENT_LIGHT = (float) data[0];
                break;
            case STAR_BRIGHTNESS_CMD:
                STAR_BRIGHTNESS = Math.max(0.01f, (float) data[0]);
                break;
            case FOV_CHANGED_CMD:
                if (!SlaveManager.projectionActive()) {
                    boolean checkMax = data.length == 1 || (boolean) data[1];
                    CAMERA_FOV = MathUtilsd.clamp(((Float) data[0]).intValue(), Constants.MIN_FOV, checkMax ? Constants.MAX_FOV : 179);
                }
                break;
            case PM_NUM_FACTOR_CMD:
                PM_NUM_FACTOR = MathUtilsd.clamp((float) data[0], Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR);
                break;
            case PM_LEN_FACTOR_CMD:
                PM_LEN_FACTOR = MathUtilsd.clamp((float) data[0], Constants.MIN_PM_LEN_FACTOR, Constants.MAX_PM_LEN_FACTOR);
                break;
            case PM_COLOR_MODE_CMD:
                PM_COLOR_MODE = MathUtilsd.clamp((int) data[0], 0, 5);
                break;
            case PM_ARROWHEADS_CMD:
                PM_ARROWHEADS = (boolean) data[0];
                break;

            case CAMERA_SPEED_CMD:
                CAMERA_SPEED = (float) data[0];
                break;
            case ROTATION_SPEED_CMD:
                ROTATION_SPEED = (float) data[0];
                break;
            case TURNING_SPEED_CMD:
                TURNING_SPEED = (float) data[0];
                break;
            case SPEED_LIMIT_CMD:
                CAMERA_SPEED_LIMIT_IDX = (Integer) data[0];
                updateSpeedLimit();
                break;
            case OCTREE_PARTICLE_FADE_CMD:
                OCTREE_PARTICLE_FADE = (boolean) data[1];
                break;
            case STAR_POINT_SIZE_CMD:
                STAR_POINT_SIZE = (float) data[0];
                break;
            case STAR_POINT_SIZE_INCREASE_CMD:
                float size = Math.min(STAR_POINT_SIZE + Constants.STEP_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE);
                EventManager.instance.post(Events.STAR_POINT_SIZE_CMD, size, false);
                break;
            case STAR_POINT_SIZE_DECREASE_CMD:
                size = Math.max(STAR_POINT_SIZE - Constants.STEP_STAR_POINT_SIZE, Constants.MIN_STAR_POINT_SIZE);
                EventManager.instance.post(Events.STAR_POINT_SIZE_CMD, size, false);
                break;
            case STAR_POINT_SIZE_RESET_CMD:
                STAR_POINT_SIZE = STAR_POINT_SIZE_BAK;
                break;
            case STAR_MIN_OPACITY_CMD:
                STAR_MIN_OPACITY = (float) data[0];
                break;
            case GALAXY_3D_CMD:
                GALAXY_3D = (boolean) data[0];
                break;
            case CROSSHAIR_FOCUS_CMD:
                CROSSHAIR_FOCUS = (boolean) data[0];
                break;
            case CROSSHAIR_CLOSEST_CMD:
                CROSSHAIR_CLOSEST = (boolean) data[0];
                break;
            case CROSSHAIR_HOME_CMD:
                CROSSHAIR_HOME = (boolean) data[0];
                break;
            case CAMERA_CINEMATIC_CMD:
                CINEMATIC_CAMERA = (boolean) data[0];
                break;
            case CUBEMAP_RESOLUTION_CMD:
                CUBEMAP_FACE_RESOLUTION = (int) data[0];
                break;
            case LABEL_SIZE_CMD:
                LABEL_SIZE_FACTOR = MathUtilsd.clamp((float) data[0], Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE);
                break;
            case ELEVATION_MUTLIPLIER_CMD:
                ELEVATION_MULTIPLIER = MathUtilsd.clamp((float) data[0], Constants.MIN_ELEVATION_MULT, Constants.MAX_ELEVATION_MULT);
                break;
            case ELEVATION_TYPE_CMD:
                ELEVATION_TYPE = (ElevationType) data[0];
                break;
            case TESSELLATION_QUALITY_CMD:
                TESSELLATION_QUALITY = (float) data[0];
                break;
            default:
                break;
            }

        }

        public boolean isNormalLineRenderer() {
            return LINE_RENDERER == 0;
        }

        public boolean isQuadLineRenderer() {
            return LINE_RENDERER == 1;
        }
    }

    public static List<IConf> configurations;

    public static FrameConf frame;
    public static ScreenConf screen;
    public static ProgramConf program;
    public static DataConf data;
    public static SceneConf scene;
    public static RuntimeConf runtime;
    public static ScreenshotConf screenshot;
    public static PerformanceConf performance;
    public static PostprocessConf postprocess;
    public static ControlsConf controls;
    public static SpacecraftConf spacecraft;
    public static VersionConf version;

    static boolean initialized = false;

    public GlobalConf() {
        super();
    }

    public static boolean initialized() {
        return initialized;
    }

    /**
     * Initialises the properties
     */
    public static void initialize(VersionConf vc, ProgramConf pc, SceneConf sc, DataConf dc, RuntimeConf rc, PostprocessConf ppc, PerformanceConf pfc, FrameConf fc, ScreenConf scrc, ScreenshotConf shc, ControlsConf cc, SpacecraftConf scc) throws Exception {
        if (!initialized) {
            if (configurations == null) {
                configurations = new ArrayList<IConf>();
            }

            version = vc;
            program = pc;
            scene = sc;
            data = dc;
            runtime = rc;
            postprocess = ppc;
            performance = pfc;
            frame = fc;
            screenshot = shc;
            screen = scrc;
            controls = cc;
            spacecraft = scc;

            configurations.add(program);
            configurations.add(scene);
            configurations.add(data);
            configurations.add(runtime);
            configurations.add(postprocess);
            configurations.add(performance);
            configurations.add(frame);
            configurations.add(screenshot);
            configurations.add(screen);
            configurations.add(controls);
            configurations.add(spacecraft);

            initialized = true;
        }

    }

    public static String getShortApplicationName() {
        return APPLICATION_SHORT_NAME + program.getNetName() + " " + version.version + " (" + version.build + ")";
    }

    public static String getSuperShortApplicationName() {
        return APPLICATION_NAME + " " + version.version;
    }

}
