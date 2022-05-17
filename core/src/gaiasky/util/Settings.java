package gaiasky.util;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import gaiasky.GaiaSky;
import gaiasky.util.camera.rec.CameraKeyframeManager;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.KeyBindings;
import gaiasky.gui.ModePopupInfo;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections.CubemapProjection;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.update.VersionChecker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

/**
 * This class contains the settings for Gaia Sky, organized into
 * several inner classes by topic.
 */
public class Settings {
    private static final Log logger = Logger.getLogger(Settings.class);

    // Assets location for this instance of Gaia Sky
    // macOS needs fully qualified paths when run as an app (GaiaSky.app), that's why we use the getAbsolutePath() part
    public static final String ASSETS_LOC = (new File(System.getProperty("assets.location") != null ? System.getProperty("assets.location") : ".")).getAbsolutePath();

    public static Path assetsPath(String relativeAssetsLoc) {
        return Path.of(ASSETS_LOC, relativeAssetsLoc);
    }

    public static String assetsFileStr(String relativeAssetsLoc) {
        return assetsPath(relativeAssetsLoc).toString();
    }

    // Static settings
    public static String APPLICATION_NAME = "Gaia Sky";
    public static String APPLICATION_NAME_TITLE = "G a i a   S k y";
    public static final String APPLICATION_SHORT_NAME = "gaiasky";
    public static final String WEBPAGE = "https://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky";
    public static final String WEBPAGE_DOWNLOADS = "https://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky/downloads";
    public static final String DOCUMENTATION = "https://gaia.ari.uni-heidelberg.de/gaiasky/docs";
    public static final String REPOSITORY = "https://gitlab.com/langurmonkey/gaiasky";
    public static final String ICON_URL;
    public static final String REPO_ISSUES = REPOSITORY + "/issues";
    public static final String AUTHOR_NAME = "Toni Sagristà Sellés";
    public static final String AUTHOR_NAME_PLAIN = "Toni Sagrista Selles";
    public static final String AUTHOR_EMAIL = "tsagrista@ari.uni-heidelberg.de";
    public static final String AUTHOR_AFFILIATION = "Universität Heidelberg, Zentrum für Astronomie, Astronomisches Rechen-Institut";
    public static final String AUTHOR_AFFILIATION_PLAIN = "Universitaet Heidelberg, Zentrum fuer Astronomie, Astronomisches Rechen-Institut";
    public static final String LICENSE_URL = "https://opensource.org/licenses/MPL-2.0";

    // The settings instance
    public static Settings settings;

    static {
        // Initialize icon: if running from source, use icon in assets/icon, otherwise, use global icon
        Path iconPath = Path.of(ASSETS_LOC + "/icon/gs_064.png");
        if (Files.exists(iconPath)) {
            logger.info("Icon found: " + iconPath);
            ICON_URL = "file://" + iconPath.toAbsolutePath();
        } else {
            logger.info("Icon not found: " + iconPath + ", using: " + ASSETS_LOC + "/gs_icon.png");
            ICON_URL = "file://" + ASSETS_LOC + "/gs_icon.png";
        }
    }

    public static String getApplicationTitle(boolean vr) {
        return APPLICATION_NAME_TITLE + (vr ? "  VR" : "");
    }

    public static String getShortApplicationName() {
        return APPLICATION_SHORT_NAME + settings.program.net.getNetName() + " " + settings.version.version + " (build " + settings.version.build + ")";
    }

    public static String getSuperShortApplicationName() {
        return APPLICATION_NAME + " " + settings.version.version;
    }

    // The configuration version
    public int configVersion;
    @JsonIgnore public boolean initialized = false;
    @JsonIgnore public VersionSettings version;

    @JsonInclude(Include.NON_NULL)
    public DataSettings data;
    @JsonInclude(Include.NON_NULL)
    public PerformanceSettings performance;
    @JsonInclude(Include.NON_NULL)
    public GraphicsSettings graphics;
    @JsonInclude(Include.NON_NULL)
    public SceneSettings scene;
    @JsonInclude(Include.NON_NULL)
    public ProgramSettings program;
    @JsonInclude(Include.NON_NULL)
    public ControlsSettings controls;
    @JsonInclude(Include.NON_NULL)
    public FrameSettings frame;
    @JsonInclude(Include.NON_NULL)
    public ScreenshotSettings screenshot;
    @JsonInclude(Include.NON_NULL)
    public CamrecorderSettings camrecorder;
    @JsonInclude(Include.NON_NULL)
    public PostprocessSettings postprocess;
    @JsonInclude(Include.NON_NULL)
    public SpacecraftSettings spacecraft;
    @JsonInclude(Include.NON_NULL)
    public ProxySettings proxy;

    @JsonIgnore public RuntimeSettings runtime;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VersionSettings {
        public String version;
        public int versionNumber;
        public Instant buildTime;
        public String builder;
        public String system;
        public String build;
        private DateTimeFormatter dateFormatter;

        public void initialize(String version, Instant buildTime, String builder, String system, String build) {
            this.version = version;
            this.versionNumber = VersionChecker.stringToVersionNumber(version);
            this.buildTime = buildTime;
            this.builder = builder;
            this.system = system;
            this.build = build;
            dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());
        }

        @Override
        public String toString() {
            return version;
        }

        public String getBuildTimePretty() {
            return dateFormatter.format(buildTime);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataSettings {
        public String location;
        public List<String> dataFiles;
        public String reflectionSkyboxLocation;
        public boolean highAccuracy;
        public boolean realGaiaAttitude;

        /**
         * This method keeps compatibility with older versions of the configuration file where
         * the setting {@link DataSettings#reflectionSkyboxLocation} was called
         * <code>skyboxLocation</code>.
         *
         * @param location The reflection skybox location.
         */
        public void setSkyboxLocation(String location) {
            this.reflectionSkyboxLocation = location;
        }

        public Path dataPath(String path) {
            // Windows does not allow asterisks in paths
            String pth = path.replaceAll("\\*", Constants.STAR_SUBSTITUTE);
            if (Path.of(pth).isAbsolute()) {
                // Absolute path, just leave it
                return Path.of(pth);
            } else {
                // Relative path, just remove leading 'data/' and prepend data location
                if (pth.startsWith("data/")) {
                    pth = pth.substring(5);
                }
                return Path.of(location).resolve(pth);
            }
        }

        public String dataFile(String path) {
            return dataPath(path).toString().replaceAll("\\\\", "/");
        }

        public FileHandle dataFileHandle(String path) {
            return new FileHandle(dataFile(path));
        }

        /**
         * Adds the given catalog descriptor file to the list of JSON selected files.
         *
         * @param catalog The catalog descriptor file pointer.
         *
         * @return True if the catalog was added, false if it does not exist, or it is not a file, or it is not readable, or it is already in the list.
         */
        public boolean addSelectedCatalog(Path catalog) {
            // Look for catalog already existing
            if (!Files.exists(catalog) || !Files.isReadable(catalog) || !Files.isRegularFile(catalog)) {
                return false;
            }
            for (String pathStr : dataFiles) {
                Path path = Path.of(pathStr);
                try {
                    if (Files.isSameFile(path, catalog)) {
                        return false;
                    }
                } catch (IOException e) {
                    logger.error(e);
                }
            }
            dataFiles.add(catalog.toString());
            return true;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PerformanceSettings {
        public boolean multithreading;
        public int numberThreads;

        /**
         * Returns the actual number of threads. It accounts for the number of
         * threads being 0 or less, "let the program decide" option, in which
         * case the number of logical processors is returned.
         *
         * @return The number of threads.
         */
        @JsonIgnore
        public int getNumberOfThreads() {
            if (numberThreads <= 0)
                return Runtime.getRuntime().availableProcessors();
            else
                return numberThreads;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphicsSettings implements IObserver {
        public GraphicsQuality quality;
        public int[] resolution;
        public boolean resizable;
        public FullscreenSettings fullScreen;
        public boolean vsync;
        public double fpsLimit;
        public double backBufferScale;
        @JsonIgnore public int[] backBufferResolution;
        public boolean dynamicResolution;
        // This controls the dynamic resolution levels available as back buffer scales.
        // Add more items to add more levels.
        @JsonIgnore final public double[] dynamicResolutionScale = new double[] { 1f, 0.85f, 0.75f };
        public boolean screenOutput;

        public GraphicsSettings() {
            EventManager.instance.subscribe(this, Event.LIMIT_FPS_CMD);
        }

        public void setQuality(final String qualityString) {
            this.quality = GraphicsQuality.valueOf(qualityString.toUpperCase());
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FullscreenSettings {
            public boolean active;
            public int[] resolution;
            public int bitDepth;
            public int refreshRate;
        }

        @JsonIgnore
        public int getScreenWidth() {
            return fullScreen.active ? fullScreen.resolution[0] : resolution[0];
        }

        @JsonIgnore
        public int getScreenHeight() {
            return fullScreen.active ? fullScreen.resolution[1] : resolution[1];
        }

        public void resize(int w, int h) {
            if (fullScreen.active) {
                fullScreen.resolution[0] = w;
                fullScreen.resolution[1] = h;
            } else {
                resolution[0] = w;
                resolution[1] = h;
            }
        }

        @Override
        public void notify(final Event event, Object source, final Object... data) {
            if (event == Event.LIMIT_FPS_CMD) {
                fpsLimit = (Double) data[0];
                if (fpsLimit > 0) {
                    // When FPS limit is active, dynamic resolution must be inactive
                    GaiaSky.postRunnable(() -> GaiaSky.instance.resetDynamicResolution());
                }
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SceneSettings implements IObserver {
        public String homeObject;
        public long fadeMs;
        public CameraSettings camera;
        public StarSettings star;
        public LabelSettings label;
        public float lineWidth;
        public ProperMotionSettings properMotion;
        public OctreeSettings octree;
        public RendererSettings renderer;
        public CrosshairSettings crosshair;
        public InitializationSettings initialization;
        public Map<String, Boolean> visibility;
        @JsonIgnore public double distanceScaleDesktop = 1d;
        @JsonIgnore public double distanceScaleVr = 1e4d;

        public SceneSettings() {
            EventManager.instance.subscribe(this, Event.TOGGLE_VISIBILITY_CMD, Event.LINE_WIDTH_CMD);
        }

        public void setVisibility(final Map<String, Object> map) {
            ComponentType[] cts = ComponentType.values();
            // Sort using the order of the {@link ComponentType} elements
            Comparator<String> componentTypeComparator = Comparator.comparingInt(s -> ComponentType.valueOf(s).ordinal());
            visibility = new TreeMap<>(componentTypeComparator);
            for (ComponentType ct : cts) {
                String key = ct.name();
                if (map.containsKey(key)) {
                    visibility.put(key, (Boolean) map.get(key));
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CameraSettings implements IObserver {
            public int speedLimitIndex;
            @JsonIgnore public double speedLimit;
            public double speed;
            public double turn;
            public double rotate;
            public float fov;
            public boolean cinematic;
            public boolean targetMode;
            public FocusSettings focusLock;

            public CameraSettings() {
                EventManager.instance.subscribe(this, Event.CAMERA_CINEMATIC_CMD, Event.FOCUS_LOCK_CMD, Event.ORIENTATION_LOCK_CMD, Event.FOV_CHANGED_CMD, Event.CAMERA_SPEED_CMD, Event.ROTATION_SPEED_CMD, Event.TURNING_SPEED_CMD, Event.SPEED_LIMIT_CMD);
            }

            @JsonProperty("speedLimitIndex")
            public void setSpeedLimitIndex(int index) {
                this.speedLimitIndex = index;
                updateSpeedLimit();
            }

            @Override
            public void notify(final Event event, Object source, final Object... data) {
                switch (event) {
                case FOCUS_LOCK_CMD -> focusLock.position = (boolean) data[1];
                case ORIENTATION_LOCK_CMD -> focusLock.orientation = (boolean) data[1];
                case FOV_CHANGED_CMD -> {
                    if (!SlaveManager.projectionActive()) {
                        boolean checkMax = source instanceof Actor;
                        fov = MathUtilsd.clamp((Float) data[0], Constants.MIN_FOV, checkMax ? Constants.MAX_FOV : 179f);
                    }
                }
                case CAMERA_SPEED_CMD -> speed = (float) data[0];
                case ROTATION_SPEED_CMD -> rotate = (float) data[0];
                case TURNING_SPEED_CMD -> turn = (float) data[0];
                case SPEED_LIMIT_CMD -> {
                    speedLimitIndex = (Integer) data[0];
                    updateSpeedLimit();
                }
                case CAMERA_CINEMATIC_CMD -> cinematic = (boolean) data[0];
                }
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class FocusSettings {
                public boolean position;
                public boolean orientation;
            }

            public void updateSpeedLimit() {
                switch (speedLimitIndex) {
                case 0 ->
                        // 100 km/h is 0.027 km/s
                        speedLimit = 0.0277777778 * Constants.KM_TO_U;
                case 1 -> speedLimit = 0.5 * Constants.C * Constants.M_TO_U;
                case 2 -> speedLimit = 0.8 * Constants.C * Constants.M_TO_U;
                case 3 -> speedLimit = 0.9 * Constants.C * Constants.M_TO_U;
                case 4 -> speedLimit = 0.99 * Constants.C * Constants.M_TO_U;
                case 5 -> speedLimit = 0.99999 * Constants.C * Constants.M_TO_U;
                case 6 -> speedLimit = Constants.C * Constants.M_TO_U;
                case 7 -> speedLimit = 2.0 * Constants.C * Constants.M_TO_U;
                case 8 ->
                        // 10 c
                        speedLimit = 10.0 * Constants.C * Constants.M_TO_U;
                case 9 ->
                        // 1000 c
                        speedLimit = 1000.0 * Constants.C * Constants.M_TO_U;
                case 10 -> speedLimit = Constants.AU_TO_U;
                case 11 -> speedLimit = 10.0 * Constants.AU_TO_U;
                case 12 -> speedLimit = 1000.0 * Constants.AU_TO_U;
                case 13 -> speedLimit = 10000.0 * Constants.AU_TO_U;
                case 14 -> speedLimit = Constants.PC_TO_U;
                case 15 -> speedLimit = 2.0 * Constants.PC_TO_U;
                case 16 ->
                        // 10 pc/s
                        speedLimit = 10.0 * Constants.PC_TO_U;
                case 17 ->
                        // 1000 pc/s
                        speedLimit = 1000.0 * Constants.PC_TO_U;
                case 18 ->
                        // No limit
                        speedLimit = -1;
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StarSettings implements IObserver {
            public float brightness;
            public float power;
            public float pointSize;
            @JsonIgnore private float pointSizeBak;
            public float[] opacity;
            public int textureIndex;
            public GroupSettings group;
            public ThresholdSettings threshold;

            public StarSettings() {
                EventManager.instance.subscribe(this, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD, Event.STAR_POINT_SIZE_CMD, Event.STAR_POINT_SIZE_INCREASE_CMD, Event.STAR_POINT_SIZE_DECREASE_CMD, Event.STAR_POINT_SIZE_RESET_CMD, Event.STAR_MIN_OPACITY_CMD, Event.STAR_GROUP_BILLBOARD_CMD, Event.STAR_GROUP_NEAREST_CMD, Event.BILLBOARD_TEXTURE_IDX_CMD);
            }

            @JsonIgnore
            public String getStarTexture() {
                String starTexIdx = String.format("%02d", textureIndex);
                String texture = settings.data.dataFile(GlobalResources.unpackAssetPath("data/tex/base/star-tex-" + starTexIdx + Constants.STAR_SUBSTITUTE + ".png"));
                if (!Files.exists(Path.of(texture))) {
                    // Fall back to whatever available
                    for (int i = 1; i < 9; i++) {
                        starTexIdx = String.format("%02d", i);
                        texture = settings.data.dataFile(GlobalResources.unpackAssetPath("data/tex/base/star-tex-" + starTexIdx + Constants.STAR_SUBSTITUTE + ".png"));
                        if (Files.exists(Path.of(texture)))
                            return texture;
                    }
                } else {
                    return texture;
                }
                return null;
            }

            /**
             * Computes the runtime star point size taking into account cubemap rendering
             *
             * @return The point size in pixels
             */
            public static float getStarPointSize() {
                if (settings.program.modeCubemap.active) {
                    float screenArea = settings.graphics.getScreenHeight() * settings.graphics.getScreenWidth();
                    float cubemapRes = settings.program.modeCubemap.faceResolution;
                    float pointSize;
                    if (cubemapRes <= 2000) {
                        pointSize = MathUtilsd.lint(cubemapRes, 500f, 2000f, 20f, 8f);
                    } else {
                        pointSize = MathUtilsd.lint(cubemapRes, 2000f, 3000f, 8f, 4f);
                    }
                    return MathUtils.clamp(pointSize, 0f, 50f) * (settings.scene.star.pointSize / 3.75f) * (MathUtilsd.lint(screenArea, 500000f, 8000000f, 1.8f, 4.8f) / 3.75f);
                } else {
                    return settings.scene.star.pointSize;
                }
            }

            public void setPointSize(float pointSize) {
                this.pointSize = pointSize;
                this.pointSizeBak = pointSize;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class GroupSettings {
                public boolean billboard;
                public int numBillboard;
                public int numLabel;
                public int numVelocityVector;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ThresholdSettings {
                public double quad;
                public double point;
                public double none;
            }

            public void notify(final Event event, Object source, final Object... data) {
                switch (event) {
                case STAR_POINT_SIZE_CMD:
                    pointSize = (float) data[0];
                    break;
                case STAR_POINT_SIZE_INCREASE_CMD:
                    float size = Math.min(this.pointSize + Constants.SLIDER_STEP_TINY, Constants.MAX_STAR_POINT_SIZE);
                    EventManager.publish(Event.STAR_POINT_SIZE_CMD, this, size);
                    break;
                case STAR_POINT_SIZE_DECREASE_CMD:
                    size = Math.max(this.pointSize - Constants.SLIDER_STEP_TINY, Constants.MIN_STAR_POINT_SIZE);
                    EventManager.publish(Event.STAR_POINT_SIZE_CMD, this, size);
                    break;
                case STAR_POINT_SIZE_RESET_CMD:
                    this.pointSize = pointSizeBak;
                    break;
                case STAR_MIN_OPACITY_CMD:
                    opacity[0] = (float) data[0];
                    break;
                case STAR_GROUP_BILLBOARD_CMD:
                    group.billboard = (boolean) data[0];
                    break;
                case STAR_GROUP_NEAREST_CMD:
                    group.numBillboard = (int) data[0];
                    group.numLabel = (int) data[0];
                    group.numVelocityVector = (int) data[0];
                    break;
                case BILLBOARD_TEXTURE_IDX_CMD:
                    textureIndex = (int) data[0];
                    break;
                case STAR_BRIGHTNESS_CMD:
                    brightness = MathUtilsd.clamp((float) data[0], Constants.MIN_STAR_BRIGHTNESS, Constants.MAX_STAR_BRIGHTNESS);
                    break;
                case STAR_BRIGHTNESS_POW_CMD:
                    power = (float) data[0];
                    break;
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class LabelSettings implements IObserver {
            public float size;
            public float number;

            public LabelSettings() {
                EventManager.instance.subscribe(this, Event.LABEL_SIZE_CMD);
            }

            @Override
            public void notify(final Event event, Object source, final Object... data) {
                if (event == Event.LABEL_SIZE_CMD) {
                    size = MathUtilsd.clamp((float) data[0], Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE);
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ProperMotionSettings implements IObserver {
            public float length;
            public double number;
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
            public int colorMode;
            public boolean arrowHeads;

            public ProperMotionSettings() {
                EventManager.instance.subscribe(this, Event.PM_LEN_FACTOR_CMD, Event.PM_NUM_FACTOR_CMD, Event.PM_COLOR_MODE_CMD, Event.PM_ARROWHEADS_CMD);
            }

            @Override
            public void notify(final Event event, Object source, final Object... data) {
                switch (event) {
                case PM_NUM_FACTOR_CMD -> number = MathUtilsd.clamp((float) data[0], Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR);
                case PM_LEN_FACTOR_CMD -> length = MathUtilsd.clamp((float) data[0], Constants.MIN_PM_LEN_FACTOR, Constants.MAX_PM_LEN_FACTOR);
                case PM_COLOR_MODE_CMD -> colorMode = MathUtilsd.clamp((int) data[0], 0, 5);
                case PM_ARROWHEADS_CMD -> arrowHeads = (boolean) data[0];
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class OctreeSettings implements IObserver {
            public int maxStars;
            public float[] threshold;
            public boolean fade;

            public OctreeSettings() {
                EventManager.instance.subscribe(this, Event.OCTREE_PARTICLE_FADE_CMD);
            }

            @Override
            public void notify(Event event, Object source, Object... data) {
                if (event == Event.OCTREE_PARTICLE_FADE_CMD) {
                    fade = (boolean) data[1];
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RendererSettings implements IObserver {
            public PointCloudMode pointCloud = PointCloudMode.POINTS;
            public LineMode line;
            public double ambient;
            public ShadowSettings shadow;
            public ElevationSettings elevation;

            public RendererSettings() {
                EventManager.instance.subscribe(this, Event.AMBIENT_LIGHT_CMD, Event.ELEVATION_MULTIPLIER_CMD, Event.ELEVATION_TYPE_CMD, Event.TESSELLATION_QUALITY_CMD);
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ShadowSettings {
                public boolean active;
                public int resolution;
                public int number;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ElevationSettings {
                public ElevationType type;
                public double multiplier;
                public double quality;

                public void setType(final String typeString) {
                    this.type = ElevationType.valueOf(typeString.toUpperCase());
                }
            }

            @JsonProperty("pointCloud")
            public void setPointCloud(String pointCloud) {
                if (pointCloud == null || pointCloud.isEmpty()) {
                    // Default
                    pointCloud = "POINTS";
                }
                if (pointCloud.startsWith("GL_")) {
                    pointCloud = pointCloud.substring(3);
                }
                this.pointCloud = PointCloudMode.valueOf(pointCloud.toUpperCase(Locale.ROOT));
            }

            @JsonIgnore
            public boolean isNormalLineRenderer() {
                return line.equals(LineMode.GL_LINES);
            }

            @JsonIgnore
            public boolean isQuadLineRenderer() {
                return line.equals(LineMode.POLYLINE_QUADSTRIP);
            }

            @Override
            public void notify(final Event event, Object source, final Object... data) {
                switch (event) {
                case AMBIENT_LIGHT_CMD -> ambient = (float) data[0];
                case ELEVATION_MULTIPLIER_CMD -> elevation.multiplier = MathUtilsd.clamp((float) data[0], Constants.MIN_ELEVATION_MULT, Constants.MAX_ELEVATION_MULT);
                case ELEVATION_TYPE_CMD -> elevation.type = (ElevationType) data[0];
                case TESSELLATION_QUALITY_CMD -> elevation.quality = (float) data[0];
                }
            }

        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CrosshairSettings implements IObserver {
            public boolean focus;
            public boolean closest;
            public boolean home;

            public CrosshairSettings() {
                EventManager.instance.subscribe(this, Event.CROSSHAIR_FOCUS_CMD, Event.CROSSHAIR_CLOSEST_CMD, Event.CROSSHAIR_HOME_CMD);
            }

            @Override
            public void notify(final Event event, Object source, final Object... data) {
                switch (event) {
                case CROSSHAIR_FOCUS_CMD -> focus = (boolean) data[0];
                case CROSSHAIR_CLOSEST_CMD -> closest = (boolean) data[0];
                case CROSSHAIR_HOME_CMD -> home = (boolean) data[0];
                default -> {
                }
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class InitializationSettings {
            public boolean lazyTexture;
            public boolean lazyMesh;
        }

        @Override
        public void notify(final Event event, Object source, final Object... data) {
            switch (event) {
            case TOGGLE_VISIBILITY_CMD -> {
                String key = (String) data[0];
                Boolean state = null;
                if (data.length == 2) {
                    state = (Boolean) data[1];
                }
                ComponentType ct = ComponentType.getFromKey(key);
                if (ct != null) {
                    visibility.put(ct.name(), (state != null ? state : !visibility.get(ct.name())));
                }
            }
            case LINE_WIDTH_CMD -> lineWidth = MathUtilsd.clamp((float) data[0], Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH);
            default -> {
            }
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgramSettings implements IObserver {
        public boolean safeMode;
        // Flag to mark whether safe mode is activated via command line argument
        @JsonIgnore public boolean safeModeFlag;
        public boolean debugInfo;
        public boolean offlineMode;
        public boolean hud;
        public boolean saveProceduralTextures = false;
        public MinimapSettings minimap;
        public FileChooserSettings fileChooser;
        public PointerSettings pointer;
        public RecursiveGridSettings recursiveGrid;
        public ModeStereoSettings modeStereo;
        public ModeCubemapSettings modeCubemap;
        public NetSettings net;
        public String scriptsLocation;
        public UiSettings ui;
        public boolean exitConfirmation;
        public String locale;
        public UpdateSettings update;
        public UrlSettings url;

        public ProgramSettings() {
            EventManager.instance.subscribe(this, Event.STEREOSCOPIC_CMD, Event.STEREO_PROFILE_CMD, Event.CUBEMAP_CMD, Event.CUBEMAP_PROJECTION_CMD, Event.SHOW_MINIMAP_ACTION, Event.TOGGLE_MINIMAP, Event.PLANETARIUM_APERTURE_CMD, Event.CUBEMAP_PROJECTION_CMD, Event.CUBEMAP_RESOLUTION_CMD, Event.POINTER_GUIDES_CMD, Event.UI_SCALE_CMD);
        }

        @JsonIgnore
        public String getDefaultLocale() {
            return "en-GB";
        }

        public String getLocale() {
            if (locale == null || locale.isEmpty()) {
                locale = Locale.getDefault().toLanguageTag();
            }
            if (locale == null) {
                locale = getDefaultLocale();
            }
            return locale;
        }

        @JsonIgnore
        public boolean isStereoOrCubemap() {
            return modeStereo.active || modeCubemap.active;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class MinimapSettings {
            public boolean active;
            public float size;
            public boolean inWindow = false;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FileChooserSettings {
            public boolean showHidden;
            public String lastLocation;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PointerSettings {
            public boolean coordinates;
            public GuidesSettings guides;

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class GuidesSettings {
                public boolean active;
                public float[] color;
                public float width;

            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RecursiveGridSettings {
            public OriginType origin;
            public boolean projectionLines;

            public void setOrigin(final String originString) {
                origin = OriginType.valueOf(originString.toUpperCase());
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ModeStereoSettings {
            public boolean active;
            public StereoProfile profile;
            @JsonIgnore public float eyeSeparation = 1f;

            public void setProfile(String profileString) {
                if (profileString.toUpperCase().equals("ANAGLYPH")) {
                    profileString = StereoProfile.ANAGLYPH_RED_CYAN.toString();
                }
                this.profile = StereoProfile.valueOf(profileString.toUpperCase());
            }

            @JsonIgnore
            public boolean isStereoHalfWidth() {
                return active && profile.correctAspect();
            }

            @JsonIgnore
            public boolean isStereoFullWidth() {
                return !isStereoHalfWidth();
            }

            @JsonIgnore
            public boolean isStereoHalfViewport() {
                return active && !profile.isAnaglyph();
            }

            @JsonIgnore
            public boolean isStereoVR() {
                return active && profile.isVR();
            }

        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ModeCubemapSettings {
            public boolean active;
            public CubemapProjection projection;
            public int faceResolution;
            public PlanetariumSettings planetarium;

            public void setProjection(final String projectionString) {
                projection = CubemapProjection.valueOf(projectionString.toUpperCase());
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class PlanetariumSettings {
                public float aperture;
                public float angle;
            }

            /**
             * Checks whether the program is in planetarium mode
             *
             * @return Whether planetarium mode is on
             */
            @JsonIgnore
            public boolean isPlanetariumOn() {
                return active && projection.isPlanetarium();
            }

            /**
             * Checks whether the program is in panorama mode
             *
             * @return Whether panorama mode is on
             */
            @JsonIgnore
            public boolean isPanoramaOn() {
                return active && projection.isPanorama();
            }

            /**
             * Checks whether we are in fixed fov mode (slave, planetarium, panorama)
             *
             * @return Whether we are in a fixed-fov mode
             */
            @JsonIgnore
            public boolean isFixedFov() {
                return isPanoramaOn() || isPlanetariumOn();
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class NetSettings {
            public int restPort;
            public MasterSettings master;
            public SlaveSettings slave;

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class MasterSettings {
                public boolean active;
                public List<String> slaves;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class SlaveSettings {
                public boolean active;
                public String configFile;
                public String warpFile;
                public String blendFile;
                public float yaw;
                public float pitch;
                public float roll;

            }

            @JsonIgnore
            public String getNetName() {
                if (master.active)
                    return " MASTER";
                else if (slave.active)
                    return " SLAVE";
                return "";
            }

            @JsonIgnore
            public boolean isMasterInstance() {
                return master.active;
            }

            @JsonIgnore
            public boolean isSlaveInstance() {
                return slave.active;
            }

            /**
             * Checks whether the MPCDI configuration file for this slave is set
             *
             * @return Whether the MPCDI file for this slave is set
             */
            @JsonIgnore
            public boolean isSlaveMPCDIPresent() {
                return slave.configFile != null && !slave.configFile.isEmpty();
            }

            /**
             * Checks whether the slave is configured directly in the properties file
             * of Gaia Sky
             *
             * @return Whether the slave is configured in the properties file
             */
            public boolean areSlaveConfigPropertiesPresent() {
                return !Double.isNaN(slave.yaw) && !Double.isNaN(slave.pitch) && !Double.isNaN(slave.roll);
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UiSettings {
            public String theme;
            public float scale;
            public DistanceUnits distanceUnits;

            /**
             * Never use this method to get the scale, use the field itself, it is public.
             */
            public float getScale() {
                return MathUtilsd.lint(scale, Constants.UI_SCALE_INTERNAL_MIN, Constants.UI_SCALE_INTERNAL_MAX, Constants.UI_SCALE_MIN, Constants.UI_SCALE_MAX);
            }

            @JsonIgnore
            public boolean isUINightMode() {
                return theme.contains("night");
            }

            @JsonIgnore
            public boolean isHiDPITheme() {
                return scale > 1.5;
            }

            @JsonProperty("distanceUnits")
            public void setDistanceUnits(String distanceUnits) {
                if (distanceUnits == null || distanceUnits.isEmpty()) {
                    // Default
                    distanceUnits = "PC";
                }
                this.distanceUnits = DistanceUnits.valueOf(distanceUnits.toUpperCase());
            }

        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UpdateSettings {
            // Update checker time, in ms
            @JsonIgnore public static long VERSION_CHECK_INTERVAL_MS = 86400000L;
            public Instant lastCheck;
            public String lastVersion;

            @JsonIgnore
            public String getLastCheckedString() {
                DateTimeFormatter df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(ZoneOffset.UTC);
                return df.format(lastCheck);
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UrlSettings {
            public String versionCheck;
            public String dataMirror = "https://gaia.ari.uni-heidelberg.de/gaiasky/files/repository/";
            public String dataDescriptor;
        }

        @Override
        public void notify(final Event event, Object source, final Object... data) {
            switch (event) {
            case STEREOSCOPIC_CMD:
                if (!GaiaSky.instance.cameraManager.mode.isGaiaFov()) {
                    modeStereo.active = (boolean) (Boolean) data[0];
                    if (modeStereo.active && modeCubemap.active) {
                        modeStereo.active = false;
                        EventManager.publish(Event.DISPLAY_GUI_CMD, this, true, I18n.msg("notif.cleanmode"));
                    }
                }
                break;
            case STEREO_PROFILE_CMD:
                modeStereo.profile = StereoProfile.values()[(Integer) data[0]];
                break;
            case CUBEMAP_CMD:
                modeCubemap.active = (Boolean) data[0] && !Settings.settings.runtime.openVr;
                if (modeCubemap.active) {
                    modeCubemap.projection = (CubemapProjections.CubemapProjection) data[1];

                    // Post a message to the screen
                    ModePopupInfo mpi = new ModePopupInfo();
                    if (modeCubemap.projection.isPanorama()) {
                        String[] keysStrToggle = KeyBindings.instance.getStringArrayKeys("action.toggle/element.360");
                        String[] keysStrProj = KeyBindings.instance.getStringArrayKeys("action.toggle/element.projection");
                        mpi.title = I18n.msg("gui.360.title");
                        mpi.header = I18n.msg("gui.360.notice.header");
                        mpi.addMapping(I18n.msg("gui.360.notice.back"), keysStrToggle);
                        mpi.addMapping(I18n.msg("gui.360.notice.projection"), keysStrProj);
                        if (settings.scene.renderer.pointCloud.isPoints()) {
                            mpi.warn = I18n.msg("gui.360.notice.renderer");
                        }
                    } else if (modeCubemap.projection.isPlanetarium()) {
                        String[] keysStr = KeyBindings.instance.getStringArrayKeys("action.toggle/element.planetarium");
                        mpi.title = I18n.msg("gui.planetarium.title");
                        mpi.header = I18n.msg("gui.planetarium.notice.header");
                        mpi.addMapping(I18n.msg("gui.planetarium.notice.back"), keysStr);
                    }

                    EventManager.publish(Event.MODE_POPUP_CMD, this, mpi, "cubemap", 120f);
                } else {
                    EventManager.publish(Event.MODE_POPUP_CMD, this, null, "cubemap");
                }
                break;
            case CUBEMAP_PROJECTION_CMD:
                modeCubemap.projection = (CubemapProjections.CubemapProjection) data[0];
                logger.info(I18n.msg("gui.360.projection", modeCubemap.projection.toString()));
                break;
            case CUBEMAP_RESOLUTION_CMD:
                modeCubemap.faceResolution = (int) data[0];
                break;
            case SHOW_MINIMAP_ACTION:
                minimap.active = (boolean) (Boolean) data[0];
                break;
            case TOGGLE_MINIMAP:
                minimap.active = !minimap.active;
                break;
            case PLANETARIUM_APERTURE_CMD:
                modeCubemap.planetarium.aperture = (float) data[0];
                break;
            case POINTER_GUIDES_CMD:
                if (data.length > 0 && data[0] != null) {
                    pointer.guides.active = (boolean) data[0];
                    if (data.length > 1 && data[1] != null) {
                        pointer.guides.color = (float[]) data[1];
                        if (data.length > 2 && data[2] != null) {
                            pointer.guides.width = (float) data[2];
                        }
                    }
                }
                break;
            case UI_SCALE_CMD:
                ui.scale = (Float) data[0];
                break;
            default:
                break;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ControlsSettings {
        public GamepadSettings gamepad;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class GamepadSettings {
            public String mappingsFile;
            public boolean invertX;
            public boolean invertY;
            public String[] blacklist;
            /**
             * Keep track of added controller listeners
             */
            private final Map<Controller, Set<ControllerListener>> controllerListenersMap;

            public GamepadSettings() {
                controllerListenersMap = new HashMap<>();
            }

            public boolean isControllerBlacklisted(String controllerName) {
                if (blacklist == null || blacklist.length == 0) {
                    return false;
                } else {
                    for (String cn : blacklist) {
                        if (controllerName.equalsIgnoreCase(cn))
                            return true;
                    }
                }
                return false;
            }

            private void addListener(Controller c, ControllerListener cl) {
                if (!controllerListenersMap.containsKey(c)) {
                    Set<ControllerListener> cs = new HashSet<>();
                    cs.add(cl);
                    controllerListenersMap.put(c, cs);
                } else {
                    Set<ControllerListener> cs = controllerListenersMap.get(c);
                    cs.add(cl);
                }
            }

            private void removeListener(Controller c, ControllerListener cl) {
                if (controllerListenersMap.containsKey(c)) {
                    Set<ControllerListener> cs = controllerListenersMap.get(c);
                    cs.remove(cl);
                }
            }

            public void addControllerListener(ControllerListener listener) {
                Array<Controller> controllers = Controllers.getControllers();
                for (Controller controller : controllers) {
                    if (!isControllerBlacklisted(controller.getName())) {
                        // Prevent duplicates
                        controller.removeListener(listener);
                        // Add
                        controller.addListener(listener);
                        addListener(controller, listener);
                    }
                }
            }

            public void addControllerListener(ControllerListener listener, String controllerName) {
                Array<Controller> controllers = Controllers.getControllers();
                for (Controller controller : controllers) {
                    if (!isControllerBlacklisted(controller.getName()) && controllerName.equals(controller.getName())) {
                        // Prevent duplicates
                        controller.removeListener(listener);
                        // Add
                        controller.addListener(listener);
                        addListener(controller, listener);
                    }
                }
            }

            public void removeControllerListener(ControllerListener listener) {
                Array<Controller> controllers = Controllers.getControllers();
                for (Controller controller : controllers) {
                    if (!isControllerBlacklisted(controller.getName())) {
                        controller.removeListener(listener);
                        removeListener(controller, listener);
                    }
                }
            }

            public void removeAllControllerListeners() {
                Array<Controller> controllers = Controllers.getControllers();
                for (Controller controller : controllers) {
                    if (!isControllerBlacklisted(controller.getName())) {
                        Set<ControllerListener> s = controllerListenersMap.get(controller);
                        if (s != null) {
                            for (ControllerListener cl : s)
                                controller.removeListener(cl);
                            s.clear();
                        }
                    }
                }
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScreenshotSettings implements IObserver {
        public static final int MIN_SCREENSHOT_SIZE = 50;
        public static final int MAX_SCREENSHOT_SIZE = 25000;

        public String location;
        public ImageFormat format;
        public float quality;
        public ScreenshotMode mode;
        public int[] resolution;

        public ScreenshotSettings() {
            EventManager.instance.subscribe(this, Event.CONFIG_SCREENSHOT_CMD, Event.SCREENSHOT_MODE_CMD);
        }

        public void setFormat(final String formatString) {
            format = ImageFormat.valueOf(formatString.toUpperCase());
        }

        public void setMode(final String modeString) {
            mode = ScreenshotMode.valueOf(modeString);
        }

        @JsonIgnore
        public boolean isSimpleMode() {
            return mode.equals(ScreenshotMode.SIMPLE);
        }

        @JsonIgnore
        public boolean isAdvancedMode() {
            return mode.equals(ScreenshotMode.ADVANCED);
        }

        @Override
        public void notify(Event event, Object source, Object... data) {
            switch (event) {
            case CONFIG_SCREENSHOT_CMD -> {
                resolution[0] = (int) data[0];
                resolution[1] = (int) data[1];
                location = (String) data[3];
            }
            case SCREENSHOT_MODE_CMD -> {
                Object newMode = data[0];
                ScreenshotMode mode = null;
                if (newMode instanceof String) {
                    try {
                        mode = ScreenshotMode.valueOf(((String) newMode).toUpperCase());
                    } catch (IllegalArgumentException e) {
                        logger.error("Given value is not a representation of ScreenshotMode (simple|advanced): '" + newMode + "'");
                    }
                } else {
                    mode = (ScreenshotMode) newMode;
                }
                if (mode != null) {
                    this.mode = mode;
                }
            }
            }

        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FrameSettings extends ScreenshotSettings implements IObserver {
        @JsonIgnore public boolean active;
        public String prefix;
        public boolean time;
        public double targetFps;

        public FrameSettings() {
            EventManager.instance.subscribe(this, Event.CONFIG_FRAME_OUTPUT_CMD, Event.FRAME_OUTPUT_CMD, Event.FRAME_OUTPUT_MODE_CMD);
        }

        @Override
        public void notify(final Event event, Object source, final Object... data) {
            switch (event) {
            case CONFIG_FRAME_OUTPUT_CMD -> {
                boolean updateFrameSize = resolution[0] != (int) data[0] || resolution[1] != (int) data[1];
                resolution[0] = (int) data[0];
                resolution[1] = (int) data[1];
                targetFps = (double) data[2];
                location = (String) data[3];
                prefix = (String) data[4];
                if (updateFrameSize) {
                    EventManager.publish(Event.FRAME_SIZE_UPDATE, this, resolution[0], resolution[1]);
                }
            }
            case FRAME_OUTPUT_MODE_CMD -> {
                Object newMode = data[0];
                ScreenshotMode mode = null;
                if (newMode instanceof String) {
                    try {
                        mode = ScreenshotMode.valueOf(((String) newMode).toUpperCase());
                    } catch (IllegalArgumentException e) {
                        logger.error("Given value is not a representation of ScreenshotMode (simple|advanced): '" + newMode + "'");
                    }
                } else {
                    mode = (ScreenshotMode) newMode;
                }
                if (mode != null) {
                    this.mode = mode;
                }
            }
            case FRAME_OUTPUT_CMD -> {
                active = (Boolean) data[0];
                // Flush buffer if needed
                if (!active && GaiaSky.instance != null) {
                    EventManager.publish(Event.FLUSH_FRAMES, this);
                }
            }
            default -> {
            }
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CamrecorderSettings implements IObserver {
        public double targetFps;
        public KeyframeSettings keyframe;
        public boolean auto;

        public CamrecorderSettings() {
            EventManager.instance.subscribe(this, Event.CAMRECORDER_FPS_CMD);
        }

        @Override
        public void notify(Event event, Object source, Object... data) {
            if (event == Event.CAMRECORDER_FPS_CMD) {
                targetFps = (Double) data[0];
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class KeyframeSettings {
            public CameraKeyframeManager.PathType position;
            public CameraKeyframeManager.PathType orientation;

            public void setPosition(final String positionString) {
                position = getPathType(positionString);
            }

            public void setOrientation(final String orientationString) {
                orientation = getPathType(orientationString);
            }

            private CameraKeyframeManager.PathType getPathType(String str) {
                return CameraKeyframeManager.PathType.valueOf(str.toUpperCase());
            }
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostprocessSettings implements IObserver {
        public Antialias antialias;
        public BloomSettings bloom;
        public UnsharpMaskSettings unsharpMask;
        public LevelsSettings levels;
        public ToneMappingSettings toneMapping;
        public boolean ssr;
        public boolean motionBlur;
        public boolean lensFlare;
        public boolean lightGlow;
        public boolean fisheye;

        public PostprocessSettings() {
            EventManager.instance.subscribe(this, Event.BLOOM_CMD, Event.UNSHARP_MASK_CMD, Event.LENS_FLARE_CMD, Event.MOTION_BLUR_CMD, Event.SSR_CMD, Event.LIGHT_SCATTERING_CMD, Event.FISHEYE_CMD, Event.BRIGHTNESS_CMD, Event.CONTRAST_CMD, Event.HUE_CMD, Event.SATURATION_CMD, Event.GAMMA_CMD, Event.TONEMAPPING_TYPE_CMD, Event.EXPOSURE_CMD);
        }

        public void setAntialias(final String antialiasString) {
            antialias = Antialias.valueOf(antialiasString.toUpperCase());
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class BloomSettings {
            public float intensity;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UnsharpMaskSettings {
            public float factor;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class LevelsSettings {
            public float brightness;
            public float contrast;
            public float hue;
            public float saturation;
            public float gamma;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ToneMappingSettings {
            public ToneMapping type;
            public float exposure;

            public void setType(final String typeString) {
                type = ToneMapping.valueOf(typeString.toUpperCase());
            }
        }

        public Antialias getAntialias(int code) {
            return switch (code) {
                case -1 -> Antialias.FXAA;
                case -2 -> Antialias.NFAA;
                case 1 -> Antialias.SSAA;
                default -> Antialias.NONE;
            };
        }

        @Override
        public void notify(final Event event, Object source, final Object... data) {
            switch (event) {
            case BLOOM_CMD -> bloom.intensity = (float) data[0];
            case UNSHARP_MASK_CMD -> unsharpMask.factor = (float) data[0];
            case LENS_FLARE_CMD -> lensFlare = (Boolean) data[0];
            case LIGHT_SCATTERING_CMD -> lightGlow = (Boolean) data[0];
            case SSR_CMD -> ssr = (Boolean) data[0];
            case MOTION_BLUR_CMD -> motionBlur = (Boolean) data[0];
            case FISHEYE_CMD -> {
                fisheye = (Boolean) data[0];

                // Post a message to the screen
                if (fisheye) {
                    String[] keysStr = KeyBindings.instance.getStringArrayKeys("action.toggle/element.planetarium");
                    ModePopupInfo mpi = new ModePopupInfo();
                    mpi.title = I18n.msg("gui.planetarium.title");
                    mpi.header = I18n.msg("gui.planetarium.notice.header");
                    mpi.addMapping(I18n.msg("gui.planetarium.notice.back"), keysStr);

                    EventManager.publish(Event.MODE_POPUP_CMD, this, mpi, "planetarium", 120f);
                } else {
                    EventManager.publish(Event.MODE_POPUP_CMD, this, null, "planetarium");
                }
            }
            case BRIGHTNESS_CMD -> levels.brightness = MathUtils.clamp((float) data[0], Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS);
            case CONTRAST_CMD -> levels.contrast = MathUtils.clamp((float) data[0], Constants.MIN_CONTRAST, Constants.MAX_CONTRAST);
            case HUE_CMD -> levels.hue = MathUtils.clamp((float) data[0], Constants.MIN_HUE, Constants.MAX_HUE);
            case SATURATION_CMD -> levels.saturation = MathUtils.clamp((float) data[0], Constants.MIN_SATURATION, Constants.MAX_SATURATION);
            case GAMMA_CMD -> levels.gamma = MathUtils.clamp((float) data[0], Constants.MIN_GAMMA, Constants.MAX_GAMMA);
            case TONEMAPPING_TYPE_CMD -> {
                ToneMapping newTM;
                if (data[0] instanceof String) {
                    newTM = ToneMapping.valueOf(((String) data[0]).toUpperCase());
                } else {
                    newTM = (ToneMapping) data[0];
                }
                toneMapping.type = newTM;
            }
            case EXPOSURE_CMD -> toneMapping.exposure = MathUtilsd.clamp((float) data[0], Constants.MIN_EXPOSURE, Constants.MAX_EXPOSURE);
            default -> {
            }
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpacecraftSettings {
        public boolean velocityDirection;
        public boolean showAxes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RuntimeSettings implements IObserver {
        public boolean openVr = false;
        public boolean displayGui = true;
        public boolean updatePause = false;
        public boolean timeOn = false;
        public boolean realTime = false;
        public boolean inputEnabled = true;
        public boolean recordCamera = false;
        public boolean recordKeyframeCamera = false;

        public boolean drawOctree = false;
        public boolean relativisticAberration = false;
        public boolean gravitationalWaves = false;
        public boolean displayVrGui = false;

        // Max clock time, 5 Myr by default
        public long maxTimeMs = 5000000L * (long) Nature.Y_TO_MS;
        // Min clock time, -5 Myr by default
        public long minTimeMs = -maxTimeMs;

        public RuntimeSettings() {
            EventManager.instance.subscribe(this, Event.INPUT_ENABLED_CMD, Event.DISPLAY_GUI_CMD, Event.TOGGLE_UPDATEPAUSE, Event.TIME_STATE_CMD, Event.RECORD_CAMERA_CMD, Event.GRAV_WAVE_START, Event.GRAV_WAVE_STOP, Event.DISPLAY_VR_GUI_CMD);
        }

        public void setMaxTime(long years) {
            maxTimeMs = years * (long) Nature.Y_TO_MS;
            minTimeMs = -maxTimeMs;
        }

        /**
         * Toggles the time
         */
        public void toggleTimeOn(Boolean timeOn) {
            this.timeOn = Objects.requireNonNullElseGet(timeOn, () -> !this.timeOn);
        }

        /**
         * Toggles the record camera
         */
        private double backupLimitFps = 0;

        public void toggleRecord(Boolean rec, Settings settings) {
            recordCamera = Objects.requireNonNullElseGet(rec, () -> !recordCamera);

            if (recordCamera) {
                // Activation, set limit FPS
                backupLimitFps = settings.graphics.fpsLimit;
                settings.graphics.fpsLimit = settings.frame.targetFps;
            } else {
                // Deactivation, remove limit
                settings.graphics.fpsLimit = backupLimitFps;
            }
        }

        @Override
        public void notify(Event event, Object source, Object... data) {

            switch (event) {
            case INPUT_ENABLED_CMD:
                inputEnabled = (boolean) data[0];
                break;
            case DISPLAY_GUI_CMD:
                displayGui = (boolean) data[0];
                break;
            case DISPLAY_VR_GUI_CMD:
                if (data.length > 1) {
                    displayVrGui = (Boolean) data[1];
                } else {
                    displayVrGui = !displayVrGui;
                }
                break;
            case TOGGLE_UPDATEPAUSE:
                updatePause = !updatePause;
                EventManager.publish(Event.UPDATEPAUSE_CHANGED, this, updatePause);
                break;
            case TIME_STATE_CMD:
                toggleTimeOn((Boolean) data[0]);
                break;
            case RECORD_CAMERA_CMD:
                toggleRecord((Boolean) data[0], settings);
                break;
            case GRAV_WAVE_START:
                gravitationalWaves = true;
                break;
            case GRAV_WAVE_STOP:
                gravitationalWaves = false;
                break;
            default:
                break;

            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxySettings {

        @JsonInclude(Include.NON_NULL)
        public ProxyBean http;
        @JsonInclude(Include.NON_NULL)
        public ProxyBean https;
        @JsonInclude(Include.NON_NULL)
        public ProxyBean socks;
        @JsonInclude(Include.NON_NULL)
        public ProxyBean ftp;
        @JsonInclude(Include.NON_NULL)
        public Boolean useSystemProxies;

        public static class ProxyBean {
            @JsonInclude(Include.NON_NULL)
            public Integer version;
            @JsonInclude(Include.NON_NULL)
            public Integer port;
            @JsonInclude(Include.NON_EMPTY)
            public String host;
            @JsonInclude(Include.NON_EMPTY)
            public String username;
            @JsonInclude(Include.NON_EMPTY)
            public String password;
            @JsonInclude(Include.NON_EMPTY)
            public String nonProxyHosts;
        }
    }

    // ============
    // ENUMERATIONS
    //=============

    public enum ScreenshotMode {
        SIMPLE,
        ADVANCED
    }

    public enum ImageFormat {
        PNG,
        JPG
    }

    public enum StereoProfile {
        /**
         * Left image -> left eye, distortion
         **/
        VR_HEADSET,
        /**
         * Left image -> left eye, distortion
         **/
        HORIZONTAL_3DTV,
        /**
         * Top-bottom
         **/
        VERTICAL_3DTV,
        /**
         * Left image -> right eye, no distortion
         **/
        CROSSEYE,
        /**
         * Left image -> left eye, no distortion
         **/
        PARALLEL_VIEW,
        /**
         * Red-cyan anaglyph 3D mode
         **/
        ANAGLYPH_RED_CYAN,
        /**
         * Red-blue anaglyph 3D mode
         **/
        ANAGLYPH_RED_BLUE;

        public boolean isHorizontal() {
            return this.equals(VR_HEADSET) || this.equals(HORIZONTAL_3DTV) || this.equals(CROSSEYE) || this.equals(PARALLEL_VIEW);
        }

        public boolean isVertical() {
            return this.equals(VERTICAL_3DTV);
        }

        public boolean isVR() {
            return this.equals(VR_HEADSET);
        }

        public boolean isAnaglyph() {
            return this.equals(ANAGLYPH_RED_BLUE) || this.equals(ANAGLYPH_RED_CYAN);
        }

        public boolean isAnaglyphRedCyan() {
            return this.equals(ANAGLYPH_RED_CYAN);
        }

        public boolean isAnaglyphRedBlue() {
            return this.equals(ANAGLYPH_RED_BLUE);
        }

        public int getAnaglyphModeInteger() {
            if (isAnaglyphRedBlue()) {
                return 0;
            } else if (isAnaglyphRedCyan()) {
                return 1;
            } else {
                return 1;
            }
        }

        public boolean correctAspect() {
            return !this.equals(HORIZONTAL_3DTV) && !this.isAnaglyph();
        }
    }

    public enum OriginType {
        REFSYS,
        FOCUS;

        public boolean isRefSys() {
            return this.equals(REFSYS);
        }

        public boolean isFocus() {
            return this.equals(FOCUS);
        }

    }

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

    public enum Antialias {
        NONE(0),
        FXAA(-1),
        NFAA(-2),
        SSAA(1);

        int aaCode;

        Antialias(int aacode) {
            this.aaCode = aacode;
        }

        public static Antialias getFromCode(int code) {
            return switch (code) {
                case 0 -> NONE;
                case -1 -> FXAA;
                case -2 -> NFAA;
                case 1 -> SSAA;
                default -> throw new IllegalStateException("Unexpected value: " + code);
            };
        }

        public int getAACode() {
            return this.aaCode;
        }

        public boolean isPostProcessAntialias() {
            return this.aaCode < 0;
        }
    }

    public enum ToneMapping {
        AUTO,
        EXPOSURE,
        ACES,
        UNCHARTED,
        FILMIC,
        NONE
    }

    public enum PointCloudMode {
        TRIANGLES,
        TRIANGLES_INSTANCED,
        POINTS;

        public boolean isPoints() {
            return this.equals(POINTS);
        }

        public boolean isTriangles() {
            return this.equals(TRIANGLES) || this.equals(TRIANGLES_INSTANCED);
        }
    }

    public enum LineMode {
        GL_LINES,
        POLYLINE_QUADSTRIP,
    }

    public enum DistanceUnits {
        PC(Nature.PC_TO_KM, Nature.KM_TO_PC, "pc"),
        LY(Nature.LY_TO_KM, Nature.KM_TO_LY, "ly");
        // Factor to apply to this unit to get kilometres
        public final double toKm;
        // Factor to apply to kilometers to get this unit
        public final double fromKm;
        private final String unitString;

        DistanceUnits(double toKm, double fromKm, String unitString) {
            this.toKm = toKm;
            this.fromKm = fromKm;
            this.unitString = unitString;
        }

        public String getUnitString() {
            return I18n.msg("gui.unit." + this.unitString);
        }
    }
}
