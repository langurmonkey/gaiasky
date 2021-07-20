/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.desktop.GaiaSkyDesktop;
import gaiasky.desktop.util.camera.CameraKeyframeManager;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.*;
import gaiasky.util.GlobalConf.*;
import gaiasky.util.GlobalConf.PostprocessConf.Antialias;
import gaiasky.util.GlobalConf.ProgramConf.OriginType;
import gaiasky.util.GlobalConf.ProgramConf.StereoProfile;
import gaiasky.util.GlobalConf.SceneConf.ElevationType;
import gaiasky.util.GlobalConf.SceneConf.GraphicsQuality;
import gaiasky.util.Logger.Log;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.IDateFormat;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections.CubemapProjection;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.parse.Parser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Properties;

/**
 * Desktop GlobalConf initializer, where the configuration comes from a
 * global.properties file (global.vr.properties in case of VR).
 */
public class DesktopConfInit extends ConfInit {
    private static final Log logger = Logger.getLogger(DesktopConfInit.class);

    private CommentedProperties p;
    private Properties vp;
    private final IDateFormat df = DateFormatFactory.getFormatter("dd/MM/yyyy HH:mm:ss");

    public DesktopConfInit(boolean vr) {
        super();
        try {
            String propsFileProperty = System.getProperty("properties.file");
            if (propsFileProperty == null || propsFileProperty.isEmpty()) {
                propsFileProperty = initConfigFile(false, vr);
            }

            File confFile = new File(propsFileProperty);
            InputStream fis = new FileInputStream(confFile);

            // This should work for the normal execution
            InputStream vis = GaiaSkyDesktop.class.getResourceAsStream("/version");
            if (vis == null) {
                // In case of running in 'developer' mode
                vis = new FileInputStream(GlobalConf.ASSETS_LOC + File.separator + "dummyversion");
            }
            vp = new Properties();
            vp.load(vis);

            p = new CommentedProperties();
            p.load(fis);

        } catch (Exception e) {
            logger.error(e);
        }
    }

    public DesktopConfInit(InputStream fis, InputStream vis) {
        super();
        try {
            vp = new Properties();
            vp.load(vis);

            p = new CommentedProperties();
            p.load(fis);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void initialiseProperties(File confFile) {
        try {
            InputStream fis = new FileInputStream(confFile);
            p = new CommentedProperties();
            p.load(fis);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    public void initGlobalConf() throws Exception {
        String ARCH = System.getProperty("sun.arch.data.model");

        // VERSION CONF
        VersionConf versionConf = new VersionConf();
        String versionStr = vp.getProperty("version");
        DateTimeFormatter mydf = DateTimeFormatter.ofPattern("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
        Instant buildtime = null;
        try {
            buildtime = LocalDateTime.parse(vp.getProperty("buildtime"), mydf).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            mydf = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH);
            try {
                buildtime = LocalDateTime.parse(vp.getProperty("buildtime"), mydf).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e1) {
                logger.error(e1);
            }
        }
        versionConf.initialize(versionStr, buildtime, vp.getProperty("builder"), vp.getProperty("system"), vp.getProperty("build"));

        // PERFORMANCE CONF
        PerformanceConf performanceConf = new PerformanceConf();
        boolean MULTITHREADING = Parser.parseBoolean(p.getProperty("global.conf.multithreading"));
        String propNumthreads = p.getProperty("global.conf.numthreads");
        int NUMBER_THREADS = Parser.parseInt((propNumthreads == null || propNumthreads.isEmpty()) ? "0" : propNumthreads);
        performanceConf.initialize(MULTITHREADING, NUMBER_THREADS);

        // POSTPROCESS CONF
        PostprocessConf postprocessConf = new PostprocessConf();
        Antialias POSTPROCESS_ANTIALIAS = postprocessConf.getAntialias(Parser.parseInt(p.getProperty("postprocess.antialiasing")));
        float POSTPROCESS_BLOOM_INTENSITY = Parser.parseFloat(p.getProperty("postprocess.bloom.intensity"));
        float POSTPROCESS_UNSHARPMASK_FACTOR = Parser.parseFloat(p.getProperty("postprocess.unsharpmask.factor", "0.3"));
        boolean POSTPROCESS_MOTION_BLUR = Parser.parseFloat(p.getProperty("postprocess.motionblur")) > 0;
        boolean POSTPROCESS_LENS_FLARE = Parser.parseBoolean(p.getProperty("postprocess.lensflare"));
        boolean POSTPROCESS_LIGHT_SCATTERING = Parser.parseBoolean(p.getProperty("postprocess.lightscattering", "false"));
        boolean POSTPROCESS_FISHEYE = Parser.parseBoolean(p.getProperty("postprocess.fisheye", "false"));
        float POSTPROCESS_BRIGHTNESS = Parser.parseFloat(p.getProperty("postprocess.brightness", "0"));
        float POSTPROCESS_CONTRAST = Parser.parseFloat(p.getProperty("postprocess.contrast", "1"));
        float POSTPROCESS_HUE = Parser.parseFloat(p.getProperty("postprocess.hue", "1"));
        float POSTPROCESS_SATURATION = Parser.parseFloat(p.getProperty("postprocess.saturation", "1"));
        float POSTPROCESS_GAMMA = Parser.parseFloat(p.getProperty("postprocess.gamma", "1"));
        PostprocessConf.ToneMapping POSTPROCESS_TONEMAPPING_TYPE = PostprocessConf.ToneMapping.valueOf(p.getProperty("postprocess.tonemapping.type", "auto").toUpperCase());
        float POSTPROCESS_EXPOSURE = Parser.parseFloat(p.getProperty("postprocess.exposure", "0"));
        postprocessConf.initialize(POSTPROCESS_ANTIALIAS, POSTPROCESS_BLOOM_INTENSITY, POSTPROCESS_UNSHARPMASK_FACTOR, POSTPROCESS_MOTION_BLUR, POSTPROCESS_LENS_FLARE, POSTPROCESS_LIGHT_SCATTERING, POSTPROCESS_FISHEYE, POSTPROCESS_BRIGHTNESS, POSTPROCESS_CONTRAST, POSTPROCESS_HUE, POSTPROCESS_SATURATION, POSTPROCESS_GAMMA, POSTPROCESS_TONEMAPPING_TYPE, POSTPROCESS_EXPOSURE);

        // RUNTIME CONF
        RuntimeConf runtimeConf = new RuntimeConf();
        runtimeConf.initialize(true, false, false, true, false, false, false);

        // DATA CONF
        DataConf dataConf = new DataConf();

        String DATA_LOCATION = p.getProperty("data.location");
        if (DATA_LOCATION == null || DATA_LOCATION.isEmpty())
            DATA_LOCATION = SysUtils.getLocalDataDir().toAbsolutePath().toString().replaceAll("\\\\", "/");

        String CATALOG_JSON_FILE_SEQUENCE = p.getProperty("data.json.catalog", "");
        Array<String> CATALOG_JSON_FILES = new Array<>();
        if (CATALOG_JSON_FILE_SEQUENCE != null && !CATALOG_JSON_FILE_SEQUENCE.isEmpty())
            CATALOG_JSON_FILES.addAll(CATALOG_JSON_FILE_SEQUENCE.split(File.pathSeparator));

        String OBJECTS_JSON_FILE = p.getProperty("data.json.objects");

        boolean REAL_GAIA_ATTITUDE = Parser.parseBoolean(p.getProperty("data.attitude.real"));
        boolean HIGH_ACCURACY_POSITIONS = Parser.parseBoolean(p.getProperty("data.highaccuracy.positions", "false"));

        String SKYBOX_LOCATION = p.getProperty("data.skybox.location", "data/tex/skybox/stars/");

        dataConf.initialize(DATA_LOCATION, CATALOG_JSON_FILES, OBJECTS_JSON_FILE, REAL_GAIA_ATTITUDE, HIGH_ACCURACY_POSITIONS, SKYBOX_LOCATION);

        // PROGRAM CONF
        ProgramConf programConf = new ProgramConf();
        String LOCALE = p.getProperty("program.locale");

        boolean SHOW_DEBUG_INFO = Parser.parseBoolean(p.getProperty("program.debuginfo"));
        Instant LAST_CHECKED;
        try {
            LAST_CHECKED = df.parse(p.getProperty("program.lastchecked"));
        } catch (Exception e) {
            LAST_CHECKED = null;
        }
        String LAST_VERSION = p.getProperty("program.lastversion", "0.0.0");
        String VERSION_CHECK_URL = p.getProperty("program.url.versioncheck");
        String DATA_DESCRIPTOR_URL = p.getProperty("program.url.data.descriptor");
        String MIRROR_URL = p.getProperty("program.url.data.mirror");
        String UI_THEME = p.getProperty("program.ui.theme");
        float externalUIScale = Parser.parseFloat(p.getProperty("program.ui.scale", "1.0"));
        float UI_SCALE = MathUtilsd.lint(externalUIScale, Constants.UI_SCALE_MIN, Constants.UI_SCALE_MAX, Constants.UI_SCALE_INTERNAL_MIN, Constants.UI_SCALE_INTERNAL_MAX);

        String SCRIPT_LOCATION = p.getProperty("program.scriptlocation").isEmpty() ? System.getProperty("user.dir") + File.separatorChar + "scripts" : p.getProperty("program.scriptlocation");
        SCRIPT_LOCATION = SCRIPT_LOCATION.replaceAll("\\\\", "/");
        int REST_PORT = Parser.parseInt(p.getProperty("program.restport", "-1"));

        boolean STEREOSCOPIC_MODE = Parser.parseBoolean(p.getProperty("program.stereoscopic"));
        StereoProfile STEREO_PROFILE = StereoProfile.values()[Parser.parseInt(p.getProperty("program.stereoscopic.profile"))];
        boolean CUBEMAP_MODE = Parser.parseBoolean(p.getProperty("program.cubemap", "false"));
        CubemapProjection CUBEMAP_PROJECTION = CubemapProjection.valueOf(p.getProperty("program.cubemap.projection", "equirectangular").toUpperCase());
        int CUBEMAP_FACE_RESOLUTION = Parser.parseInt(p.getProperty("program.cubemap.face.resolution", "1500"));
        float PLANETARIUM_APERTURE = Parser.parseFloat(p.getProperty("program.planetarium.aperture", "180.0"));
        float PLANETARIUM_ANGLE = Parser.parseFloat(p.getProperty("program.planetarium.angle", "50.0"));
        boolean DISPLAY_HUD = Parser.parseBoolean(p.getProperty("program.display.hud", "false"));
        boolean DISPLAY_POINTER_COORDS = Parser.parseBoolean(p.getProperty("program.pointer.coords.display", "true"));
        boolean DISPLAY_MINIMAP = Parser.parseBoolean(p.getProperty("program.display.minimap", "true"));
        float MINIMAP_SIZE = MathUtilsd.clamp(Parser.parseFloat(p.getProperty("program.minimap.size", "220.0")), Constants.MIN_MINIMAP_SIZE, Constants.MAX_MINIMAP_SIZE);
        boolean NET_MASTER = Parser.parseBoolean(p.getProperty("program.net.master", "false"));
        boolean NET_SLAVE = Parser.parseBoolean(p.getProperty("program.net.slave", "false"));
        String NET_SLAVE_CONFIG = p.getProperty("program.net.slave.config", "");
        float NET_SLAVE_YAW = Parser.parseFloat(p.getProperty("program.net.slave.yaw", "NaN"));
        float NET_SLAVE_PITCH = Parser.parseFloat(p.getProperty("program.net.slave.pitch", "NaN"));
        float NET_SLAVE_ROLL = Parser.parseFloat(p.getProperty("program.net.slave.roll", "NaN"));
        String NET_SLAVE_WARP = p.getProperty("program.net.slave.warp", "");
        String NET_SLAVE_BLEND = p.getProperty("program.net.slave.blend", "");
        String LAST_FOLDER_LOCATION = p.getProperty("program.last.filesystem.location");
        boolean EXIT_CONFIRMATION = Parser.parseBoolean(p.getProperty("program.exit.confirmation", "true"));

        // Pointer guides
        boolean DISPLAY_POINTER_GUIDES = Parser.parseBoolean(p.getProperty("program.pointer.guides.display", "false"));
        float[] POINTER_GUIDES_COLOR = Parser.parseFloatArray(p.getProperty("program.pointer.guides.color", "[1.0,1.0,1.0,0.3]"));
        float POINTER_GUIDES_WIDTH = Parser.parseFloat(p.getProperty("program.pointer.guides.width", "1.5"));

        // Recursive grid origin
        OriginType RECURSIVE_GRID_ORIGIN = OriginType.valueOf(p.getProperty("program.recursivegrid.origin", "focus").toUpperCase());
        boolean RECURSIVE_GRID_ORIGIN_LINES = Parser.parseBoolean(p.getProperty("program.recursivegrid.origin.lines", "true"));

        // Safe graphics mode
        boolean SAFE_GRAPHICS_MODE = Parser.parseBoolean(p.getProperty("program.safe.graphics.mode", "false"));
        // Show hidden
        boolean FILE_CHOOSER_SHOW_HIDDEN = Parser.parseBoolean(p.getProperty("program.filechooser.showhidden", "false"));

        LinkedList<String> NET_MASTER_SLAVES = null;
        if (NET_MASTER) {
            NET_MASTER_SLAVES = new LinkedList<>();
            String value;
            for (int i = 0; (value = p.getProperty("program.net.master.slaves." + i)) != null; i++) {
                NET_MASTER_SLAVES.add(value);
            }
        }

        programConf.initialize(SHOW_DEBUG_INFO, LAST_CHECKED, LAST_VERSION, VERSION_CHECK_URL, DATA_DESCRIPTOR_URL, UI_THEME, UI_SCALE, SCRIPT_LOCATION, REST_PORT, LOCALE, STEREOSCOPIC_MODE, STEREO_PROFILE, CUBEMAP_MODE, CUBEMAP_PROJECTION, CUBEMAP_FACE_RESOLUTION, DISPLAY_HUD, DISPLAY_POINTER_COORDS, NET_MASTER, NET_SLAVE, NET_MASTER_SLAVES, NET_SLAVE_CONFIG, NET_SLAVE_YAW, NET_SLAVE_PITCH, NET_SLAVE_ROLL, NET_SLAVE_WARP, NET_SLAVE_BLEND, LAST_FOLDER_LOCATION, DISPLAY_MINIMAP, MINIMAP_SIZE, PLANETARIUM_APERTURE, PLANETARIUM_ANGLE, DISPLAY_POINTER_GUIDES, POINTER_GUIDES_COLOR, POINTER_GUIDES_WIDTH, RECURSIVE_GRID_ORIGIN, RECURSIVE_GRID_ORIGIN_LINES, EXIT_CONFIRMATION, MIRROR_URL, SAFE_GRAPHICS_MODE, FILE_CHOOSER_SHOW_HIDDEN);

        // SCENE CONF
        String gc = p.getProperty("scene.graphics.quality");
        GraphicsQuality GRAPHICS_QUALITY;
        try {
            // Use ordinal integer
            int quality = Parser.parseIntException(gc);
            GRAPHICS_QUALITY = GraphicsQuality.values()[quality % GraphicsQuality.values().length];
        } catch (NumberFormatException e) {
            // Use string
            GRAPHICS_QUALITY = GraphicsQuality.valueOf(gc.toUpperCase());
        }
        String STARTUP_OBJECT = p.getProperty("scene.object.startup", "Earth");
        long OBJECT_FADE_MS = Long.parseLong(p.getProperty("scene.object.fadems"));
        float STAR_BRIGHTNESS = Parser.parseFloat(p.getProperty("scene.star.brightness", "2.0"));
        float STAR_BRIGHTNESS_POWER = Parser.parseFloat(p.getProperty("scene.star.brightness.pow", "0.65"));
        float STAR_POINT_SIZE = Parser.parseFloat(p.getProperty("scene.star.point.size", "4.7"));
        boolean STAR_GROUP_BILLBOARD_FLAG = Parser.parseBoolean(p.getProperty("scene.star.group.billboard.flag", "true"));
        int STAR_GROUP_N_BILLBOARDS = Parser.parseInt(p.getProperty("scene.star.group.billboards", "30"));
        int STAR_GROUP_N_LABELS = Parser.parseInt(p.getProperty("scene.star.group.labels", "50"));
        int STAR_GROUP_N_VELVECS = Parser.parseInt(p.getProperty("scene.star.group.velocityvectors", "500"));
        int STAR_TEX_INDEX = Parser.parseInt(p.getProperty("scene.star.tex.index", "3"));
        float AMBIENT_LIGHT = Parser.parseFloat(p.getProperty("scene.ambient"));
        float CAMERA_FOV = Parser.parseFloat(p.getProperty("scene.camera.fov"));
        int CAMERA_SPEED_LIMIT_IDX = Parser.parseInt(p.getProperty("scene.camera.speedlimit"));
        float CAMERA_SPEED = Parser.parseFloat(p.getProperty("scene.camera.focus.vel"));
        boolean FOCUS_LOCK = Parser.parseBoolean(p.getProperty("scene.focuslock"));
        boolean FOCUS_LOCK_ORIENTATION = Parser.parseBoolean(p.getProperty("scene.focuslock.orientation", "false"));
        float TURNING_SPEED = Parser.parseFloat(p.getProperty("scene.camera.turn.vel"));
        float ROTATION_SPEED = Parser.parseFloat(p.getProperty("scene.camera.rotate.vel"));
        float LABEL_SIZE_FACTOR = Parser.parseFloat(p.getProperty("scene.label.size"));
        float LABEL_NUMBER_FACTOR = Parser.parseFloat(p.getProperty("scene.label.number"));
        float LINE_WIDTH_FACTOR = Parser.parseFloat(p.getProperty("scene.line.width", "1.0"));
        double STAR_TH_ANGLE_QUAD = Parser.parseDouble(p.getProperty("scene.star.threshold.quad"));
        double STAR_TH_ANGLE_POINT = Parser.parseDouble(p.getProperty("scene.star.threshold.point"));
        double STAR_TH_ANGLE_NONE = Parser.parseDouble(p.getProperty("scene.star.threshold.none"));
        float STAR_MIN_OPACITY = Parser.parseFloat(p.getProperty("scene.point.alpha.min"));
        float STAR_MAX_OPACITY = Parser.parseFloat(p.getProperty("scene.point.alpha.max"));
        int ORBIT_RENDERER = Parser.parseInt(p.getProperty("scene.renderer.orbit", "0"));
        int LINE_RENDERER = Parser.parseInt(p.getProperty("scene.renderer.line"));
        boolean OCTREE_PARTICLE_FADE = Parser.parseBoolean(p.getProperty("scene.octree.particle.fade"));
        float OCTANT_THRESHOLD_0 = Parser.parseFloat(p.getProperty("scene.octant.threshold.0"));
        float OCTANT_THRESHOLD_1 = Parser.parseFloat(p.getProperty("scene.octant.threshold.1"));
        // Limiting draw distance in 32-bit JVM
        if (ARCH.equals("32")) {
            float delta = Math.abs(OCTANT_THRESHOLD_1 - OCTANT_THRESHOLD_0);
            OCTANT_THRESHOLD_0 = (float) Math.toRadians(80);
            OCTANT_THRESHOLD_1 = OCTANT_THRESHOLD_0 + delta;
        }
        float PM_NUM_FACTOR = Parser.parseFloat(p.getProperty("scene.propermotion.numfactor", "20.0"));
        float PM_LEN_FACTOR = Parser.parseFloat(p.getProperty("scene.propermotion.lenfactor", "1E1"));
        int PM_COLOR_MODE = Parser.parseInt(p.getProperty("scene.propermotion.colormode", "0"));
        boolean PM_ARROWHEADS = Parser.parseBoolean(p.getProperty("scene.propermotion.arrowheads", "true"));
        boolean GALAXY_3D = Parser.parseBoolean(p.getProperty("scene.galaxy.3d", "true"));
        boolean CROSSHAIR_FOCUS = Parser.parseBoolean(p.getProperty("scene.crosshair.focus", "true"));
        boolean CROSSHAIR_CLOSEST = Parser.parseBoolean(p.getProperty("scene.crosshair.closest", "true"));
        boolean CROSSHAIR_HOME = Parser.parseBoolean(p.getProperty("scene.crosshair.home", "true"));
        boolean CINEMATIC_CAMERA = Parser.parseBoolean(p.getProperty("scene.camera.cinematic", "false"));
        boolean FREE_CAMERA_TARGET_MODE_ON = Parser.parseBoolean(p.getProperty("scene.camera.free.targetmode", "false"));
        boolean SHADOW_MAPPING = Parser.parseBoolean(p.getProperty("scene.shadowmapping", "true"));
        int SHADOW_MAPPING_N_SHADOWS = MathUtilsd.clamp(Parser.parseInt(p.getProperty("scene.shadowmapping.nshadows", "2")), 0, 4);
        int SHADOW_MAPPING_RESOLUTION = Parser.parseInt(p.getProperty("scene.shadowmapping.resolution", "512"));
        long MAX_LOADED_STARS = Long.parseLong(p.getProperty("scene.octree.maxstars", "10000000"));
        // Limiting number of stars in 32-bit JVM
        if (ARCH.equals("32")) {
            MAX_LOADED_STARS = 1500000;
        }

        // Visibility of components
        ComponentType[] cts = ComponentType.values();
        boolean[] VISIBILITY = new boolean[cts.length];
        for (ComponentType ct : cts) {
            String key = "scene.visibility." + ct.name();
            if (p.containsKey(key)) {
                VISIBILITY[ct.ordinal()] = Parser.parseBoolean(p.getProperty(key));
            }
        }
        boolean LAZY_TEXTURE_INIT = Parser.parseBoolean(p.getProperty("scene.lazy.texture", "true"));
        boolean LAZY_MESH_INIT = Parser.parseBoolean(p.getProperty("scene.lazy.mesh", "true"));
        ElevationType ELEVATION_TYPE = ElevationType.valueOf(p.getProperty("scene.elevation.type", "tessellation").toUpperCase());
        double ELEVATION_MULTIPLIER = Parser.parseDouble(p.getProperty("scene.elevation.multiplier", "1.0"));
        double TESSELLATION_QUALITY = Parser.parseDouble(p.getProperty("scene.tessellation.quality", "4.0"));

        // Hardcoded for now
        double DIST_SCALE_DESKTOP = 1d;
        double DIST_SCALE_VR = 1e4d;

        SceneConf sceneConf = new SceneConf();
        sceneConf.initialize(STARTUP_OBJECT, GRAPHICS_QUALITY, OBJECT_FADE_MS, STAR_BRIGHTNESS, STAR_BRIGHTNESS_POWER, STAR_TEX_INDEX, STAR_GROUP_BILLBOARD_FLAG, STAR_GROUP_N_BILLBOARDS, STAR_GROUP_N_LABELS, STAR_GROUP_N_VELVECS, AMBIENT_LIGHT, CAMERA_FOV, CAMERA_SPEED, TURNING_SPEED, ROTATION_SPEED, CAMERA_SPEED_LIMIT_IDX, FOCUS_LOCK, FOCUS_LOCK_ORIENTATION, LABEL_SIZE_FACTOR, LABEL_NUMBER_FACTOR, LINE_WIDTH_FACTOR, VISIBILITY, ORBIT_RENDERER, LINE_RENDERER, STAR_TH_ANGLE_NONE, STAR_TH_ANGLE_POINT, STAR_TH_ANGLE_QUAD, STAR_MIN_OPACITY, STAR_MAX_OPACITY, OCTREE_PARTICLE_FADE, OCTANT_THRESHOLD_0, OCTANT_THRESHOLD_1, PM_NUM_FACTOR, PM_LEN_FACTOR, PM_COLOR_MODE, PM_ARROWHEADS, STAR_POINT_SIZE, GALAXY_3D, CROSSHAIR_FOCUS, CROSSHAIR_CLOSEST, CROSSHAIR_HOME, CINEMATIC_CAMERA, LAZY_TEXTURE_INIT, LAZY_MESH_INIT, FREE_CAMERA_TARGET_MODE_ON, SHADOW_MAPPING, SHADOW_MAPPING_N_SHADOWS, SHADOW_MAPPING_RESOLUTION, MAX_LOADED_STARS, ELEVATION_TYPE, ELEVATION_MULTIPLIER, TESSELLATION_QUALITY, DIST_SCALE_DESKTOP, DIST_SCALE_VR);

        // FRAME CONF
        String renderFolder;
        if (p.getProperty("graphics.render.folder") == null || p.getProperty("graphics.render.folder").isEmpty()) {
            Path framesDir = SysUtils.getDefaultFramesDir();
            Files.createDirectories(framesDir);
            renderFolder = framesDir.toAbsolutePath().toString();
        } else {
            renderFolder = p.getProperty("graphics.render.folder");
        }
        String RENDER_FOLDER = renderFolder.replaceAll("\\\\", "/");
        String RENDER_FILE_NAME = p.getProperty("graphics.render.filename");
        int RENDER_WIDTH = Parser.parseInt(p.getProperty("graphics.render.width"));
        int RENDER_HEIGHT = Parser.parseInt(p.getProperty("graphics.render.height"));
        double RENDER_TARGET_FPS = Parser.parseDouble(p.getProperty("graphics.render.targetfps", "60.0"));
        double CAMERA_REC_TARGET_FPS = Parser.parseDouble(p.getProperty("graphics.camera.recording.targetfps", "60.0"));
        boolean AUTO_FRAME_OUTPUT_CAMERA_PLAY = Parser.parseBoolean(p.getProperty("graphics.camera.recording.frameoutputauto", "false"));
        boolean RENDER_SCREENSHOT_TIME = Parser.parseBoolean(p.getProperty("graphics.render.time"));

        ScreenshotMode FRAME_MODE = ScreenshotMode.valueOf(p.getProperty("graphics.render.mode"));
        ImageFormat FRAME_FORMAT = ImageFormat.valueOf(p.getProperty("graphics.render.format", "jpg").toUpperCase());
        float FRAME_QUALITY = Parser.parseFloat(p.getProperty("graphics.render.quality", "0.93"));

        CameraKeyframeManager.PathType KF_POS = CameraKeyframeManager.PathType.valueOf(p.getProperty("graphics.camera.keyframe.path.position", CameraKeyframeManager.PathType.SPLINE.toString()));
        CameraKeyframeManager.PathType KF_ORI = CameraKeyframeManager.PathType.valueOf(p.getProperty("graphics.camera.keyframe.path.orientation", CameraKeyframeManager.PathType.SPLINE.toString()));

        FrameConf frameConf = new FrameConf();
        frameConf.initialize(RENDER_WIDTH, RENDER_HEIGHT, RENDER_TARGET_FPS, CAMERA_REC_TARGET_FPS, AUTO_FRAME_OUTPUT_CAMERA_PLAY, RENDER_FOLDER, RENDER_FILE_NAME, RENDER_SCREENSHOT_TIME, RENDER_SCREENSHOT_TIME, FRAME_MODE, FRAME_FORMAT, FRAME_QUALITY, KF_POS, KF_ORI);

        // SCREEN CONF
        int SCREEN_WIDTH = Parser.parseInt(p.getProperty("graphics.screen.width"));
        int SCREEN_HEIGHT = Parser.parseInt(p.getProperty("graphics.screen.height"));
        double BACKBUFFER_SCALE = Parser.parseDouble(p.getProperty("graphics.backbuffer.scale", "1.0"));
        boolean DYNAMIC_RESOLUTION = Parser.parseBoolean(p.getProperty("graphics.dynamic.resolution", "false"));
        int FULLSCREEN_WIDTH = Parser.parseInt(p.getProperty("graphics.screen.fullscreen.width"));
        int FULLSCREEN_HEIGHT = Parser.parseInt(p.getProperty("graphics.screen.fullscreen.height"));
        boolean FULLSCREEN = Parser.parseBoolean(p.getProperty("graphics.screen.fullscreen"));
        boolean RESIZABLE = Parser.parseBoolean(p.getProperty("graphics.screen.resizable"));
        boolean VSYNC = Parser.parseBoolean(p.getProperty("graphics.screen.vsync"));
        double LIMIT_FPS = Parser.parseDouble(p.getProperty("graphics.limit.fps", "0.0"));
        boolean SCREEN_OUTPUT = Parser.parseBoolean(p.getProperty("graphics.screen.screenoutput"));
        ScreenConf screenConf = new ScreenConf();
        screenConf.initialize(SCREEN_WIDTH, SCREEN_HEIGHT, FULLSCREEN_WIDTH, FULLSCREEN_HEIGHT, FULLSCREEN, RESIZABLE, VSYNC, SCREEN_OUTPUT, LIMIT_FPS, BACKBUFFER_SCALE, DYNAMIC_RESOLUTION);

        // SCREENSHOT CONF
        String screenshotFolder;
        if (p.getProperty("screenshot.folder") == null || p.getProperty("screenshot.folder").isEmpty()) {
            Path screenshotDir = SysUtils.getDefaultScreenshotsDir();
            Files.createDirectories(screenshotDir);
            screenshotFolder = screenshotDir.toAbsolutePath().toString();
        } else {
            screenshotFolder = p.getProperty("screenshot.folder");
        }
        String SCREENSHOT_FOLDER = screenshotFolder.replaceAll("\\\\", "/");
        int SCREENSHOT_WIDTH = Parser.parseInt(p.getProperty("screenshot.width"));
        int SCREENSHOT_HEIGHT = Parser.parseInt(p.getProperty("screenshot.height"));
        ScreenshotMode SCREENSHOT_MODE = ScreenshotMode.valueOf(p.getProperty("screenshot.mode"));
        ImageFormat SCREENSHOT_FORMAT = ImageFormat.valueOf(p.getProperty("screenshot.format", "jpg").toUpperCase());
        float SCREENSHOT_QUALITY = Parser.parseFloat(p.getProperty("screenshot.quality", "0.93"));
        ScreenshotConf screenshotConf = new ScreenshotConf();
        screenshotConf.initialize(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, SCREENSHOT_FOLDER, SCREENSHOT_MODE, SCREENSHOT_FORMAT, SCREENSHOT_QUALITY);

        // CONTROLS CONF
        ControlsConf controlsConf = new ControlsConf();
        String CONTROLLER_MAPPINGS_FILE = p.getProperty("controls.gamepad.mappings.file", "mappings/SDL_Controller.controller");
        boolean INVERT_LOOK_X_AXIS = Parser.parseBoolean(p.getProperty("controls.invert.x", "false"));
        boolean INVERT_LOOK_Y_AXIS = Parser.parseBoolean(p.getProperty("controls.invert.y", "true"));
        String[] CONTROLLER_BLACKLIST = GlobalResources.parseWhitespaceSeparatedList(p.getProperty("controls.blacklist"));

        controlsConf.initialize(CONTROLLER_MAPPINGS_FILE, INVERT_LOOK_X_AXIS, INVERT_LOOK_Y_AXIS, CONTROLLER_BLACKLIST);

        // SPACECRAFT CONF
        SpacecraftConf spacecraftConf = new SpacecraftConf();
        boolean sC_VEL_TO_DIRECTION = Parser.parseBoolean(p.getProperty("spacecraft.velocity.direction", "false"));
        boolean sC_SHOW_AXES = Parser.parseBoolean(p.getProperty("spacecraft.show.axes", "false"));

        spacecraftConf.initialize(sC_VEL_TO_DIRECTION, sC_SHOW_AXES);

        // INIT GLOBAL CONF
        GlobalConf.initialize(versionConf, programConf, sceneConf, dataConf, runtimeConf, postprocessConf, performanceConf, frameConf, screenConf, screenshotConf, controlsConf, spacecraftConf);

    }

    private int getValidWidth() {
        int w = GaiaSky.instance.graphics != null ? GaiaSky.instance.graphics.getWidth() : 0;
        if (w <= 0)
            return 1280;
        return w;
    }

    private int getValidHeight() {
        int h = GaiaSky.instance.graphics != null ? GaiaSky.instance.graphics.getHeight() : 0;
        if (h <= 0)
            return 720;
        return h;
    }

    @Override
    public void persistGlobalConf(File propsFile) {

        // SCREENSHOT
        p.setProperty("screenshot.folder", GlobalConf.screenshot.SCREENSHOT_FOLDER);
        p.setProperty("screenshot.width", Integer.toString(GlobalConf.screenshot.SCREENSHOT_WIDTH));
        p.setProperty("screenshot.height", Integer.toString(GlobalConf.screenshot.SCREENSHOT_HEIGHT));
        p.setProperty("screenshot.mode", GlobalConf.screenshot.SCREENSHOT_MODE.toString());
        p.setProperty("screenshot.format", GlobalConf.screenshot.SCREENSHOT_FORMAT.toString().toLowerCase());
        p.setProperty("screenshot.quality", Float.toString(GlobalConf.screenshot.SCREENSHOT_QUALITY));

        // PERFORMANCE
        p.setProperty("global.conf.multithreading", Boolean.toString(GlobalConf.performance.MULTITHREADING));
        p.setProperty("global.conf.numthreads", Integer.toString(GlobalConf.performance.NUMBER_THREADS));

        // POSTPROCESS
        p.setProperty("postprocess.antialiasing", Integer.toString(GlobalConf.postprocess.POSTPROCESS_ANTIALIAS.getAACode()));
        p.setProperty("postprocess.bloom.intensity", Float.toString(GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY));
        p.setProperty("postprocess.unsharpmask.factor", Float.toString(GlobalConf.postprocess.POSTPROCESS_UNSHARPMASK_FACTOR));
        p.setProperty("postprocess.motionblur", GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR ? "1.0" : "0.0");
        p.setProperty("postprocess.lensflare", Boolean.toString(GlobalConf.postprocess.POSTPROCESS_LENS_FLARE));
        p.setProperty("postprocess.lightscattering", Boolean.toString(GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING));
        p.setProperty("postprocess.brightness", Float.toString(GlobalConf.postprocess.POSTPROCESS_BRIGHTNESS));
        p.setProperty("postprocess.contrast", Float.toString(GlobalConf.postprocess.POSTPROCESS_CONTRAST));
        p.setProperty("postprocess.hue", Float.toString(GlobalConf.postprocess.POSTPROCESS_HUE));
        p.setProperty("postprocess.saturation", Float.toString(GlobalConf.postprocess.POSTPROCESS_SATURATION));
        p.setProperty("postprocess.gamma", Float.toString(GlobalConf.postprocess.POSTPROCESS_GAMMA));
        p.setProperty("postprocess.tonemapping.type", GlobalConf.postprocess.POSTPROCESS_TONEMAPPING_TYPE.toString().toLowerCase());
        p.setProperty("postprocess.exposure", Float.toString(GlobalConf.postprocess.POSTPROCESS_EXPOSURE));
        p.setProperty("postprocess.fisheye", Boolean.toString(GlobalConf.postprocess.POSTPROCESS_FISHEYE));

        // FRAME CONF
        p.setProperty("graphics.render.folder", GlobalConf.frame.RENDER_FOLDER);
        p.setProperty("graphics.render.filename", GlobalConf.frame.RENDER_FILE_NAME);
        p.setProperty("graphics.render.width", Integer.toString(GlobalConf.frame.RENDER_WIDTH));
        p.setProperty("graphics.render.height", Integer.toString(GlobalConf.frame.RENDER_HEIGHT));
        p.setProperty("graphics.render.targetfps", Double.toString(GlobalConf.frame.RENDER_TARGET_FPS));
        p.setProperty("graphics.camera.recording.targetfps", Double.toString(GlobalConf.frame.CAMERA_REC_TARGET_FPS));
        p.setProperty("graphics.camera.recording.frameoutputauto", Boolean.toString(GlobalConf.frame.AUTO_FRAME_OUTPUT_CAMERA_PLAY));
        p.setProperty("graphics.camera.keyframe.path.position", GlobalConf.frame.KF_PATH_TYPE_POSITION.toString());
        p.setProperty("graphics.camera.keyframe.path.orientation", GlobalConf.frame.KF_PATH_TYPE_ORIENTATION.toString());
        p.setProperty("graphics.render.time", Boolean.toString(GlobalConf.frame.RENDER_SCREENSHOT_TIME));
        p.setProperty("graphics.render.mode", GlobalConf.frame.FRAME_MODE.toString());
        p.setProperty("graphics.render.format", GlobalConf.frame.FRAME_FORMAT.toString().toLowerCase());
        p.setProperty("graphics.render.quality", Float.toString(GlobalConf.frame.FRAME_QUALITY));

        // DATA
        p.setProperty("data.location", GlobalConf.data.DATA_LOCATION);
        p.setProperty("data.json.catalog", TextUtils.concatenate(File.pathSeparator, GlobalConf.data.CATALOG_JSON_FILES));
        p.setProperty("data.json.objects", GlobalConf.data.OBJECTS_JSON_FILES);
        p.setProperty("data.attitude.real", Boolean.toString(GlobalConf.data.REAL_GAIA_ATTITUDE));
        p.setProperty("data.highaccuracy.positions", Boolean.toString(GlobalConf.data.HIGH_ACCURACY_POSITIONS));
        p.setProperty("data.skybox.location", GlobalConf.data.SKYBOX_LOCATION);

        // SCREEN
        if (Gdx.graphics != null) {
            p.setProperty("graphics.screen.width", Integer.toString(Gdx.graphics.isFullscreen() ? GlobalConf.screen.SCREEN_WIDTH : getValidWidth()));
            p.setProperty("graphics.screen.height", Integer.toString(Gdx.graphics.isFullscreen() ? GlobalConf.screen.SCREEN_HEIGHT : getValidHeight()));
            p.setProperty("graphics.screen.fullscreen.width", Integer.toString(!Gdx.graphics.isFullscreen() ? GlobalConf.screen.FULLSCREEN_WIDTH : getValidWidth()));
            p.setProperty("graphics.screen.fullscreen.height", Integer.toString(!Gdx.graphics.isFullscreen() ? GlobalConf.screen.FULLSCREEN_HEIGHT : getValidHeight()));
            p.setProperty("graphics.screen.fullscreen", Boolean.toString(Gdx.graphics.isFullscreen()));
        }
        p.setProperty("graphics.screen.resizable", Boolean.toString(GlobalConf.screen.RESIZABLE));
        p.setProperty("graphics.screen.vsync", Boolean.toString(GlobalConf.screen.VSYNC));
        p.setProperty("graphics.limit.fps", Double.toString(GlobalConf.screen.LIMIT_FPS));
        p.setProperty("graphics.screen.screenoutput", Boolean.toString(GlobalConf.screen.SCREEN_OUTPUT));

        // PROGRAM
        p.setProperty("program.display.hud", Boolean.toString(GlobalConf.program.DISPLAY_HUD));
        p.setProperty("program.pointer.coords.display", Boolean.toString(GlobalConf.program.DISPLAY_POINTER_COORDS));
        p.setProperty("program.pointer.guides.display", Boolean.toString(GlobalConf.program.DISPLAY_POINTER_GUIDES));
        p.setProperty("program.pointer.guides.color", Arrays.toString(GlobalConf.program.POINTER_GUIDES_COLOR));
        p.setProperty("program.pointer.guides.width", Float.toString(GlobalConf.program.POINTER_GUIDES_WIDTH));
        p.setProperty("program.recursivegrid.origin", GlobalConf.program.RECURSIVE_GRID_ORIGIN.toString().toLowerCase());
        p.setProperty("program.recursivegrid.origin.lines", Boolean.toString(GlobalConf.program.RECURSIVE_GRID_ORIGIN_LINES));
        p.setProperty("program.display.minimap", Boolean.toString(GlobalConf.program.DISPLAY_MINIMAP));
        p.setProperty("program.minimap.size", Float.toString(GlobalConf.program.MINIMAP_SIZE));
        p.setProperty("program.debuginfo", Boolean.toString(GlobalConf.program.SHOW_DEBUG_INFO));
        p.setProperty("program.lastchecked", GlobalConf.program.VERSION_LAST_TIME != null ? df.format(GlobalConf.program.VERSION_LAST_TIME) : "");
        p.setProperty("program.url.versioncheck", GlobalConf.program.VERSION_CHECK_URL);
        p.setProperty("program.url.data.descriptor", GlobalConf.program.DATA_DESCRIPTOR_URL);
        p.setProperty("program.url.data.mirror", GlobalConf.program.DATA_MIRROR_URL);
        p.setProperty("program.ui.theme", GlobalConf.program.UI_THEME);
        p.setProperty("program.ui.scale", Float.toString(MathUtilsd.lint(GlobalConf.program.UI_SCALE, Constants.UI_SCALE_INTERNAL_MIN, Constants.UI_SCALE_INTERNAL_MAX, Constants.UI_SCALE_MIN, Constants.UI_SCALE_MAX)));
        p.setProperty("program.exit.confirmation", Boolean.toString(GlobalConf.program.EXIT_CONFIRMATION));
        p.setProperty("program.scriptlocation", GlobalConf.program.SCRIPT_LOCATION);
        p.setProperty("program.restport", Integer.toString(GlobalConf.program.REST_PORT));
        p.setProperty("program.locale", GlobalConf.program.LOCALE);
        p.setProperty("program.stereoscopic", Boolean.toString(GlobalConf.program.STEREOSCOPIC_MODE));
        p.setProperty("program.stereoscopic.profile", Integer.toString(GlobalConf.program.STEREO_PROFILE.ordinal()));
        p.setProperty("program.cubemap", Boolean.toString(GlobalConf.program.CUBEMAP_MODE));
        p.setProperty("program.cubemap.projection", GlobalConf.program.CUBEMAP_PROJECTION.toString().toLowerCase());
        p.setProperty("program.cubemap.face.resolution", Integer.toString(GlobalConf.program.CUBEMAP_FACE_RESOLUTION));
        p.setProperty("program.planetarium.aperture", Float.toString(GlobalConf.program.PLANETARIUM_APERTURE));
        p.setProperty("program.planetarium.angle", Float.toString(GlobalConf.program.PLANETARIUM_ANGLE));
        p.setProperty("program.net.slave.yaw", Float.toString(GlobalConf.program.NET_SLAVE_YAW));
        p.setProperty("program.net.slave.pitch", Float.toString(GlobalConf.program.NET_SLAVE_PITCH));
        p.setProperty("program.net.slave.roll", Float.toString(GlobalConf.program.NET_SLAVE_ROLL));
        p.setProperty("program.filechooser.showhidden", Boolean.toString(GlobalConf.program.FILE_CHOOSER_SHOW_HIDDEN));
        // Persist last location if file chooser
        if (GlobalConf.program.LAST_OPEN_LOCATION != null && !GlobalConf.program.LAST_OPEN_LOCATION.isEmpty())
            p.setProperty("program.last.filesystem.location", GlobalConf.program.LAST_OPEN_LOCATION);
        // Only persist if flag is down, otherwise it has been set via CLI argument and must not be persisted
        if (!GlobalConf.program.SAFE_GRAPHICS_MODE_FLAG)
            p.setProperty("program.safe.graphics.mode", Boolean.toString(GlobalConf.program.SAFE_GRAPHICS_MODE));


        // SCENE
        p.setProperty("scene.object.startup", GlobalConf.scene.STARTUP_OBJECT);
        p.setProperty("scene.graphics.quality", GlobalConf.scene.GRAPHICS_QUALITY.toString().toLowerCase());
        p.setProperty("scene.object.fadems", Long.toString(GlobalConf.scene.OBJECT_FADE_MS));
        p.setProperty("scene.star.brightness", Double.toString(GlobalConf.scene.STAR_BRIGHTNESS));
        p.setProperty("scene.star.brightness.pow", Double.toString(GlobalConf.scene.STAR_BRIGHTNESS_POWER));
        p.setProperty("scene.ambient", Double.toString(GlobalConf.scene.AMBIENT_LIGHT));
        p.setProperty("scene.camera.fov", Float.toString(GlobalConf.scene.CAMERA_FOV));
        p.setProperty("scene.camera.speedlimit", Integer.toString(GlobalConf.scene.CAMERA_SPEED_LIMIT_IDX));
        p.setProperty("scene.camera.focus.vel", Double.toString(GlobalConf.scene.CAMERA_SPEED));
        p.setProperty("scene.camera.turn.vel", Double.toString(GlobalConf.scene.TURNING_SPEED));
        p.setProperty("scene.camera.rotate.vel", Double.toString(GlobalConf.scene.ROTATION_SPEED));
        p.setProperty("scene.focuslock", Boolean.toString(GlobalConf.scene.FOCUS_LOCK));
        p.setProperty("scene.focuslock.orientation", Boolean.toString(GlobalConf.scene.FOCUS_LOCK_ORIENTATION));
        p.setProperty("scene.label.size", Float.toString(GlobalConf.scene.LABEL_SIZE_FACTOR));
        p.setProperty("scene.label.number", Float.toString(GlobalConf.scene.LABEL_NUMBER_FACTOR));
        p.setProperty("scene.line.width", Float.toString(GlobalConf.scene.LINE_WIDTH_FACTOR));
        p.setProperty("scene.star.threshold.quad", Double.toString(GlobalConf.scene.STAR_THRESHOLD_QUAD));
        p.setProperty("scene.star.threshold.point", Double.toString(GlobalConf.scene.STAR_THRESHOLD_POINT));
        p.setProperty("scene.star.threshold.none", Double.toString(GlobalConf.scene.STAR_THRESHOLD_NONE));
        p.setProperty("scene.star.point.size", Float.toString(GlobalConf.scene.STAR_POINT_SIZE));
        p.setProperty("scene.point.alpha.min", Float.toString(GlobalConf.scene.STAR_MIN_OPACITY));
        p.setProperty("scene.point.alpha.max", Float.toString(GlobalConf.scene.STAR_MAX_OPACITY));
        p.setProperty("scene.renderer.orbit", Integer.toString(GlobalConf.scene.ORBIT_RENDERER));
        p.setProperty("scene.renderer.line", Integer.toString(GlobalConf.scene.LINE_RENDERER));
        p.setProperty("scene.octree.particle.fade", Boolean.toString(GlobalConf.scene.OCTREE_PARTICLE_FADE));
        p.setProperty("scene.octant.threshold.0", Float.toString(GlobalConf.scene.OCTANT_THRESHOLD_0));
        p.setProperty("scene.octant.threshold.1", Float.toString(GlobalConf.scene.OCTANT_THRESHOLD_1));
        p.setProperty("scene.propermotion.numfactor", Float.toString(GlobalConf.scene.PM_NUM_FACTOR));
        p.setProperty("scene.propermotion.lenfactor", Float.toString(GlobalConf.scene.PM_LEN_FACTOR));
        p.setProperty("scene.propermotion.colormode", Integer.toString(GlobalConf.scene.PM_COLOR_MODE));
        p.setProperty("scene.propermotion.arrowheads", Boolean.toString(GlobalConf.scene.PM_ARROWHEADS));
        p.setProperty("scene.galaxy.3d", Boolean.toString(GlobalConf.scene.GALAXY_3D));
        p.setProperty("scene.crosshair.focus", Boolean.toString(GlobalConf.scene.CROSSHAIR_FOCUS));
        p.setProperty("scene.crosshair.closest", Boolean.toString(GlobalConf.scene.CROSSHAIR_CLOSEST));
        p.setProperty("scene.crosshair.home", Boolean.toString(GlobalConf.scene.CROSSHAIR_HOME));
        p.setProperty("scene.camera.cinematic", Boolean.toString(GlobalConf.scene.CINEMATIC_CAMERA));
        p.setProperty("scene.camera.free.targetmode", Boolean.toString(GlobalConf.scene.FREE_CAMERA_TARGET_MODE_ON));
        p.setProperty("scene.lazy.texture", Boolean.toString(GlobalConf.scene.LAZY_TEXTURE_INIT));
        p.setProperty("scene.lazy.mesh", Boolean.toString(GlobalConf.scene.LAZY_MESH_INIT));
        p.setProperty("scene.shadowmapping", Boolean.toString(GlobalConf.scene.SHADOW_MAPPING));
        p.setProperty("scene.shadowmapping.nshadows", Integer.toString(GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS));
        p.setProperty("scene.shadowmapping.resolution", Integer.toString(GlobalConf.scene.SHADOW_MAPPING_RESOLUTION));
        p.setProperty("scene.octree.maxstars", Long.toString(GlobalConf.scene.MAX_LOADED_STARS));
        p.setProperty("scene.elevation.type", GlobalConf.scene.ELEVATION_TYPE.toString().toLowerCase());
        p.setProperty("scene.elevation.multiplier", Double.toString(GlobalConf.scene.ELEVATION_MULTIPLIER));
        p.setProperty("scene.tessellation.quality", Double.toString(GlobalConf.scene.TESSELLATION_QUALITY));
        p.setProperty("scene.star.group.billboard.flag", Boolean.toString(GlobalConf.scene.STAR_GROUP_BILLBOARD_FLAG));
        p.setProperty("scene.star.group.billboards", Integer.toString(GlobalConf.scene.STAR_GROUP_N_BILLBOARDS));
        p.setProperty("scene.star.group.labels", Integer.toString(GlobalConf.scene.STAR_GROUP_N_LABELS));
        p.setProperty("scene.star.group.velocityvectors", Integer.toString(GlobalConf.scene.STAR_GROUP_N_VELVECS));
        p.setProperty("scene.star.tex.index", Integer.toString(GlobalConf.scene.STAR_TEX_INDEX));

        // Visibility of components
        int idx = 0;
        ComponentType[] cts = ComponentType.values();
        for (boolean b : GlobalConf.scene.VISIBILITY) {
            ComponentType ct = cts[idx];
            p.setProperty("scene.visibility." + ct.name(), Boolean.toString(b));
            idx++;
        }

        // CONTROLS
        p.setProperty("controls.gamepad.mappings.file", GlobalConf.controls.CONTROLLER_MAPPINGS_FILE);
        p.setProperty("controls.invert.x", Boolean.toString(GlobalConf.controls.INVERT_LOOK_X_AXIS));
        p.setProperty("controls.invert.y", Boolean.toString(GlobalConf.controls.INVERT_LOOK_Y_AXIS));
        if (GlobalConf.controls.CONTROLLER_BLACKLIST != null)
            p.setProperty("controls.blacklist", GlobalResources.toWhitespaceSeparatedList(GlobalConf.controls.CONTROLLER_BLACKLIST));

        // SPACECRAFT
        p.setProperty("spacecraft.velocity.direction", Boolean.toString(GlobalConf.spacecraft.SC_VEL_TO_DIRECTION));
        p.setProperty("spacecraft.show.axes", Boolean.toString(GlobalConf.spacecraft.SC_SHOW_AXES));

        try {
            FileOutputStream fos = new FileOutputStream(propsFile);
            p.store(fos, null);
            fos.close();
            logger.info("Configuration saved to " + propsFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error(e);
        }

    }

    private String initConfigFile(boolean overwrite, boolean vr) throws IOException {
        // Use user folder
        Path userFolder = SysUtils.getConfigDir();
        Path userFolderConfFile = userFolder.resolve(getConfigFileName(vr));

        if (overwrite || !Files.exists(userFolderConfFile)) {
            // Copy file
            GlobalResources.copyFile(Path.of("conf", getConfigFileName(vr)), userFolderConfFile, overwrite);
        }
        String props = userFolderConfFile.toAbsolutePath().toString();
        System.setProperty("properties.file", props);
        return props;
    }

    public static String getConfigFileName(boolean vr) {
        if (vr)
            return "global.vr.properties";
        else
            return "global.properties";
    }

}
