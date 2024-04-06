/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.KeyBindings;
import gaiasky.gui.ModePopupInfo;
import gaiasky.input.AbstractGamepadListener;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.Logger.Log;
import gaiasky.util.camera.rec.KeyframesManager;
import gaiasky.util.gdx.contrib.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.update.VersionChecker;
import org.lwjgl.opengl.GL30;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds the settings of Gaia Sky. This class has a near 1-to-1 mapping to the configuration file, conf.yaml.
 */
public class Settings extends SettingsObject {

    /*
     * Source version, used to enable or disable datasets.
     * This is usually tag where each number is allocated 2 digits.
     * Version = major.minor.rev-seq -> 1.2.5 major=1; minor=2; rev=5; seq=0
     * Version = major * 1000000 + minor * 10000 + rev * 100 + seq
     * So 1.2.5   -> 1020500
     *    2.1.7   -> 2010700
     *    3.5.3-1 -> 3050301
     * Leading zeroes are omitted to avoid octal literal interpretation.
     */
    public static final int SOURCE_VERSION = 3060100;
    /**
     * Assets location for this instance of Gaia Sky.
     * macOS needs fully qualified paths when run as an app (GaiaSky.app), that's why we use the
     * {@link File#getAbsolutePath()} call.
     **/
    public static final String ASSETS_LOC = (new File(System.getProperty("assets.location") != null ? System.getProperty("assets.location") : ".")).getAbsolutePath();
    public static final String APPLICATION_SHORT_NAME = "gaiasky";
    public static final String HOMEPAGE = "https://zah.uni-heidelberg.de/gaia/outreach/gaiasky";
    public static final String HOMEPAGE_DOWNLOADS = "https://zah.uni-heidelberg.de/gaia/outreach/gaiasky/downloads";
    public static final String DOCUMENTATION = "https://gaia.ari.uni-heidelberg.de/gaiasky/docs";
    public static final String REPOSITORY = "https://codeberg.org/gaiasky/gaiasky";
    public static final String SOCIAL_MEDIA_NAME = "#GaiaSky";
    public static final String SOCIAL_MEDIA_URL = "https://mastodon.social/tags/GaiaSky";
    public static final String ICON_URL;
    public static final String REPO_ISSUES = REPOSITORY + "/issues";
    public static final String AUTHOR_NAME = "Toni Sagristà Sellés";
    public static final String AUTHOR_NAME_PLAIN = "Toni Sagrista Selles";
    public static final String AUTHOR_EMAIL = "tsagrista@ari.uni-heidelberg.de";
    public static final String AUTHOR_AFFILIATION = "Universität Heidelberg, Zentrum für Astronomie, Astronomisches Rechen-Institut";
    public static final String AUTHOR_AFFILIATION_PLAIN = "Universitaet Heidelberg, Zentrum fuer Astronomie, Astronomisches Rechen-Institut";
    public static final String AUTHOR_WEBSITE_TEXT = "tonisagrista.com";
    public static final String AUTHOR_WEBSITE_FULL = "https://tonisagrista.com";
    public static final String LICENSE_URL = "https://opensource.org/licenses/MPL-2.0";
    private static final Log logger = Logger.getLogger(Settings.class);
    // Static settings
    public static String APPLICATION_NAME = "Gaia Sky";
    public static String APPLICATION_NAME_TITLE = "G a i a  S k y";
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

    /**
     * Sets the static reference to {@link Settings} to the given object.
     * This method deactivates and disposes the old settings object, and activates the new one.
     *
     * @param s The settings object.
     * @return True if the given object is not null, false otherwise.
     */
    public static boolean setSettingsReference(Settings s) {
        if (s != null) {
            // Dispose old settings.
            if (settings != null) {
                settings.setEnabled(false);
                settings.dispose();
            }
            // Set new settings.
            settings = s;
            settings.setEnabled(true);
            return true;
        }
        return false;
    }

    // The configuration version
    public int configVersion;
    @JsonIgnore
    public boolean initialized = false;
    @JsonIgnore
    public VersionSettings version;
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
    @JsonIgnore
    public RuntimeSettings runtime;

    /**
     * Whether this settings object is currently active or not. Mainly affects whether the settings respond to events.
     */
    @JsonIgnore
    private AtomicBoolean enabled = new AtomicBoolean(true);

    @JsonIgnore
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @JsonIgnore
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Called to initialize the settings object. This mainly initializes the internal references.
     */
    public void initialize() {
        this.parent = null;
        setParentRecursive(this);
        setupListeners();
        this.initialized = true;
    }

    public static Path assetsPath(String relativeAssetsLoc) {
        return Path.of(ASSETS_LOC, relativeAssetsLoc);
    }

    public static String assetsFileStr(String relativeAssetsLoc) {
        return assetsPath(relativeAssetsLoc).toString();
    }

    public static String getApplicationTitle(boolean vr) {
        return APPLICATION_NAME_TITLE + (vr ? "\nVR" : "");
    }

    public static String getShortApplicationName() {
        return APPLICATION_SHORT_NAME + settings.program.net.getNetName() + " " + settings.version.version + " (build " + settings.version.build + ")";
    }

    public static String getSuperShortApplicationName() {
        return APPLICATION_NAME + " " + settings.version.version;
    }

    public static String getApplicationName(boolean vr) {
        return APPLICATION_NAME + (vr ? " VR" : "");
    }

    @Override
    public Settings clone() {
        var c = (Settings) super.clone();
        // Deactivated by default.
        c.enabled = new AtomicBoolean(false);

        if (this.camrecorder != null) {
            c.camrecorder = this.camrecorder.clone();
        }
        if (this.controls != null) {
            c.controls = this.controls.clone();
        }
        if (this.data != null) {
            c.data = this.data.clone();
        }
        if (this.frame != null) {
            c.frame = this.frame.clone();
        }
        if (this.graphics != null) {
            c.graphics = this.graphics.clone();
        }
        if (this.performance != null) {
            c.performance = this.performance.clone();
        }
        if (this.postprocess != null) {
            c.postprocess = this.postprocess.clone();
        }
        if (this.program != null) {
            c.program = this.program.clone();
        }
        if (this.proxy != null) {
            c.proxy = this.proxy.clone();
        }
        if (this.runtime != null) {
            c.runtime = this.runtime.clone();
        }
        if (this.scene != null) {
            c.scene = this.scene.clone();
        }
        if (this.screenshot != null) {
            c.screenshot = this.screenshot.clone();
        }
        if (this.spacecraft != null) {
            c.spacecraft = this.spacecraft.clone();
        }
        if (this.version != null) {
            c.version = this.version.clone();
        }
        c.initialize();

        return c;
    }

    @Override
    protected void setParentRecursive(SettingsObject s) {
        if (this.camrecorder != null)
            camrecorder.setParent(s);
        if (this.controls != null)
            controls.setParent(s);
        if (this.data != null)
            data.setParent(s);
        if (this.frame != null)
            frame.setParent(s);
        if (this.graphics != null)
            graphics.setParent(s);
        if (this.performance != null)
            performance.setParent(s);
        if (this.postprocess != null)
            postprocess.setParent(s);
        if (this.program != null)
            program.setParent(s);
        if (this.proxy != null)
            proxy.setParent(s);
        if (this.runtime != null)
            runtime.setParent(s);
        if (this.scene != null)
            scene.setParent(s);
        if (this.screenshot != null)
            screenshot.setParent(s);
        if (this.spacecraft != null)
            spacecraft.setParent(s);
        if (this.version != null)
            version.setParent(s);

    }

    @Override
    protected void setupListeners() {
        if (this.camrecorder != null)
            camrecorder.setupListeners();
        if (this.controls != null)
            controls.setupListeners();
        if (this.data != null)
            data.setupListeners();
        if (this.frame != null)
            frame.setupListeners();
        if (this.graphics != null)
            graphics.setupListeners();
        if (this.performance != null)
            performance.setupListeners();
        if (this.postprocess != null)
            postprocess.setupListeners();
        if (this.program != null)
            program.setupListeners();
        if (this.proxy != null)
            proxy.setupListeners();
        if (this.runtime != null)
            runtime.setupListeners();
        if (this.scene != null)
            scene.setupListeners();
        if (this.screenshot != null)
            screenshot.setupListeners();
        if (this.spacecraft != null)
            spacecraft.setupListeners();
        if (this.version != null)
            version.setupListeners();

    }

    @Override
    public void dispose() {
        if (this.camrecorder != null)
            camrecorder.dispose();
        if (this.controls != null)
            controls.dispose();
        if (this.data != null)
            data.dispose();
        if (this.frame != null)
            frame.dispose();
        if (this.graphics != null)
            graphics.dispose();
        if (this.performance != null)
            performance.dispose();
        if (this.postprocess != null)
            postprocess.dispose();
        if (this.program != null)
            program.dispose();
        if (this.proxy != null)
            proxy.dispose();
        if (this.runtime != null)
            runtime.dispose();
        if (this.scene != null)
            scene.dispose();
        if (this.screenshot != null)
            screenshot.dispose();
        if (this.spacecraft != null)
            spacecraft.dispose();
        if (this.version != null)
            version.dispose();

    }

    @Override
    public void apply() {
        if (this.camrecorder != null)
            camrecorder.apply();
        if (this.controls != null)
            controls.apply();
        if (this.data != null)
            data.apply();
        if (this.frame != null)
            frame.apply();
        if (this.graphics != null)
            graphics.apply();
        if (this.performance != null)
            performance.apply();
        if (this.postprocess != null)
            postprocess.apply();
        if (this.program != null)
            program.apply();
        if (this.proxy != null)
            proxy.apply();
        if (this.runtime != null)
            runtime.apply();
        if (this.scene != null)
            scene.apply();
        if (this.screenshot != null)
            screenshot.apply();
        if (this.spacecraft != null)
            spacecraft.apply();
        if (this.version != null)
            version.apply();

    }

    public enum ScreenshotMode {
        SIMPLE,
        ADVANCED
    }

    public enum ImageFormat {
        PNG,
        JPG
    }

    public enum ReprojectionMode {
        DISABLED(-1),
        DEFAULT(0),
        ACCURATE(1),

        STEREOGRAPHIC_SCREEN(10),
        STEREOGRAPHIC_LONG(11),
        STEREOGRAPHIC_SHORT(12),
        STEREOGRAPHIC_180(13),

        LAMBERT_SCREEN(20),
        LAMBERT_LONG(21),
        LAMBERT_SHORT(22),
        LAMBERT_180(23),

        ORTHOGRAPHIC_SCREEN(30),
        ORTHOGRAPHIC_LONG(31),
        ORTHOGRAPHIC_SHORT(32),
        ORTHOGRAPHIC_180(33);

        public final int mode;

        ReprojectionMode(int mode) {
            this.mode = mode;
        }

        @Override
        public String toString() {
            try {
                String name = I18n.msg("gui.reproj.mode." + this.name().toLowerCase());
                if (name != null && !name.isEmpty()) {
                    return name;
                } else {
                    return this.name();
                }
            }catch (Exception e) {
                return this.name();
            }
        }
    }

    public enum LensFlareType {
        SIMPLE,
        COMPLEX,
        PSEUDO;

        public boolean isPseudoLensFlare() {
            return this == PSEUDO;
        }

        public boolean isSimpleLensFlare() {
            return this == SIMPLE;
        }

        public boolean isComplexLensFlare() {
            return this == COMPLEX;
        }
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

    public enum GridStyle {
        CIRCULAR,
        SQUARE
    }

    public enum ElevationType {
        REGULAR,
        TESSELLATION,
        NONE;

        public boolean isRegular() {
            return this.equals(REGULAR);
        }

        public boolean isTessellation() {
            return this.equals(TESSELLATION);
        }

        public boolean isParallaxMapping() {
            return false;
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

        public final String key;
        public final String suffix;
        public final int texWidthTarget;
        public final int texHeightTarget;

        GraphicsQuality(String key,
                        String suffix,
                        int texWidthTarget,
                        int texHeightTarget) {
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
                return 3;
            } else if (isNormal()) {
                return 5;
            } else if (isHigh()) {
                return 6;
            } else if (isUltra()) {
                return 8;
            }
            return 5;
        }

    }

    public enum UpscaleFilter {
        NEAREST(TextureFilter.Nearest, TextureFilter.Nearest),
        LINEAR(TextureFilter.Linear, TextureFilter.Linear),
        XBRZ(TextureFilter.Nearest, TextureFilter.Nearest);

        public final TextureFilter minification, magnification;

        UpscaleFilter(TextureFilter min,
                      TextureFilter mag) {
            this.minification = min;
            this.magnification = mag;
        }

        public String toString() {
            return I18n.msg("gui.upscale." + this.name().toLowerCase());
        }
    }

    public enum AntialiasSettings {
        NONE(0),
        FXAA(-1),
        NFAA(-2),
        SSAA(1);

        final int aaCode;

        AntialiasSettings(int aacode) {
            this.aaCode = aacode;
        }

        public static AntialiasSettings getFromCode(int code) {
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
        POINTS;

        public boolean isPoints() {
            return this.equals(POINTS);
        }

        public boolean isTriangles() {
            return this.equals(TRIANGLES);
        }
    }

    public enum LineMode {
        POLYLINE_QUADSTRIP,
        GL_LINES
    }

    public enum DefaultTimeZone {
        UTC,
        SYSTEM_DEFAULT;

        public ZoneId getTimeZone() {
            if (this.equals(UTC)) {
                return ZoneId.of("UTC");
            } else {
                return ZoneOffset.systemDefault();
            }
        }
    }

    public enum DistanceUnits {
        INTERNAL(Constants.U_TO_KM, Constants.KM_TO_U, "internal"),
        M(1.0e-3, 1000, "m"),
        KM(1, 1, "km"),
        AU(Nature.AU_TO_KM, Nature.KM_TO_AU, "au"),
        PC(Nature.PC_TO_KM, Nature.KM_TO_PC, "pc"),
        LY(Nature.LY_TO_KM, Nature.KM_TO_LY, "ly");
        // Factor to apply to this unit to get kilometres
        public final double toKm;
        // Factor to apply to kilometers to get this unit
        public final double fromKm;
        private final String unitString;

        DistanceUnits(double toKm,
                      double fromKm,
                      String unitString) {
            this.toKm = toKm;
            this.fromKm = fromKm;
            this.unitString = unitString;
        }

        public String getUnitString() {
            return I18n.msg("gui.unit." + this.unitString);
        }

        /**
         * Converts the given value in the units represented by this object in
         * internal units.
         *
         * @param value The value in the units represented by this object.
         * @return The value in internal units.
         */
        public double toInternalUnits(double value) {
            return value * toKm * Constants.KM_TO_U;
        }

        /**
         * Returns the given value, given in internal units, in the units represented by this object.
         *
         * @param internal The value in internal units.
         * @return The value in the units represented by this object.
         */
        public double fromInternalUnits(double internal) {
            return internal * Constants.U_TO_KM * fromKm;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VersionSettings extends SettingsObject {
        public String version;
        public int versionNumber;
        public Instant buildTime;
        public String builder;
        public String system;
        public String build;
        private DateTimeFormatter dateFormatter;

        public void initialize(String version,
                               Instant buildTime,
                               String builder,
                               String system,
                               String build) {
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

        @Override
        public VersionSettings clone() {
            var c = (VersionSettings) super.clone();
            c.buildTime = Instant.ofEpochMilli(this.buildTime.toEpochMilli());
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
        }

        @Override
        protected void setupListeners() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public void apply() {
        }
    }

    // ============
    // ENUMERATIONS
    //=============

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataSettings extends SettingsObject {
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

        public Path dataPath(String pathStr,
                             String dsLocation) {
            // Windows does not allow asterisks in paths
            String resolvedPathStr = pathStr.replaceAll("\\*", Constants.STAR_SUBSTITUTE);

            if (resolvedPathStr.startsWith(Constants.DATA_LOCATION_TOKEN)) {
                // Path is in data directory, just remove leading 'data/' and prepend data location
                String pathFromDataStr = resolvedPathStr.replace(Constants.DATA_LOCATION_TOKEN, "");
                Path pathFromData = Path.of(pathFromDataStr);
                Path resolvedPath = Path.of(location).resolve(pathFromDataStr);
                // We inject the location if:
                // - the current resolved path does not exist, and
                // - the dataset location is not null or empty, and
                // - the injected dataset location is not already in the path.
                if (!Files.exists(resolvedPath) && dsLocation != null && !dsLocation.isEmpty() && !pathFromData.getName(0).toString().equals(dsLocation)) {
                    // Use dsLocation
                    return Path.of(location).resolve(dsLocation).resolve(pathFromDataStr);
                } else {
                    // It exists, use as it is
                    return resolvedPath;
                }
            } else {
                var p = Path.of(resolvedPathStr);
                if (p.toFile().exists()) {
                    // Relative to working dir, or absolute.
                    return p;
                } else {
                    // In data directory.
                    return Path.of(location).resolve(resolvedPathStr);
                }
            }
        }

        public Path dataPath(String path) {
            return dataPath(path, Constants.DEFAULT_DATASET_KEY);
        }

        public String dataFile(String path,
                               String dsLocation) {
            return dataPath(path, dsLocation).toString().replaceAll("\\\\", "/");
        }

        public String dataFile(String path) {
            return dataFile(path, Constants.DEFAULT_DATASET_KEY);
        }

        public FileHandle dataFileHandle(String path,
                                         String dsLocation) {
            return new FileHandle(dataFile(path, dsLocation));
        }

        public FileHandle dataFileHandle(String path) {
            return dataFileHandle(path, Constants.DEFAULT_DATASET_KEY);
        }

        /**
         * Adds the given catalog descriptor file to the list of JSON selected files.
         *
         * @param catalog The catalog descriptor file pointer.
         * @return True if the catalog was added, false if it does not exist, or it is not a file, or it is not
         * readable, or it is already in the list.
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

        @Override
        public DataSettings clone() {
            var c = (DataSettings) super.clone();
            c.dataFiles = new ArrayList<>(this.dataFiles);
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
        }

        @Override
        protected void setupListeners() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public void apply() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PerformanceSettings extends SettingsObject {
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

        @Override
        public PerformanceSettings clone() {
            return (PerformanceSettings) super.clone();
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
        }

        @Override
        protected void setupListeners() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public void apply() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphicsSettings extends SettingsObject implements IObserver {
        /**
         * This controls the dynamic resolution levels available as back buffer scales.
         * Add more items to add more levels.
         **/
        @JsonIgnore
        final public double[] dynamicResolutionScale = new double[]{1f, 0.85f, 0.75f, 0.5f};
        public GraphicsQuality quality;
        public int[] resolution;
        public boolean resizable;
        public FullscreenSettings fullScreen;
        public boolean vsync;
        public double fpsLimit;
        public double backBufferScale;
        @JsonIgnore
        public int[] backBufferResolution;
        public boolean dynamicResolution;
        /**
         * Whether to output to the main display.
         */
        public boolean screenOutput;
        /**
         * Use the sRGB color space as a frame buffer format. Only supported by OpenGL 3.2 and above.
         * If this is activated, the internal format {@link GL30#GL_SRGB8_ALPHA8} is used only
         * when safe graphics mode is not active.
         **/
        public boolean useSRGB = false;

        public void setQuality(final String qualityString) {
            this.quality = GraphicsQuality.valueOf(qualityString.toUpperCase());
        }

        @JsonIgnore
        public int getScreenWidth() {
            return fullScreen.active ? fullScreen.resolution[0] : resolution[0];
        }

        @JsonIgnore
        public int getScreenHeight() {
            return fullScreen.active ? fullScreen.resolution[1] : resolution[1];
        }

        public void resize(int w,
                           int h) {
            if (fullScreen.active) {
                fullScreen.resolution[0] = w;
                fullScreen.resolution[1] = h;
            } else {
                resolution[0] = w;
                resolution[1] = h;
            }
        }

        @Override
        public void notify(final Event event,
                           Object source,
                           final Object... data) {
            if (isEnabled() && source != this) {
                switch (event) {
                    case LIMIT_FPS_CMD -> {
                        fpsLimit = (Double) data[0];
                        if (fpsLimit > 0) {
                            // When FPS limit is active, dynamic resolution must be inactive
                            GaiaSky.postRunnable(() -> GaiaSky.instance.resetDynamicResolution());
                        }
                    }
                    case BACKBUFFER_SCALE_CMD -> {
                        backBufferScale = (Float) data[0];
                        backBufferResolution[0] = (int) Math.round(resolution[0] * backBufferScale);
                        backBufferResolution[1] = (int) Math.round(resolution[1] * backBufferScale);
                    }
                }
            }
        }

        @Override
        public GraphicsSettings clone() {
            var c = (GraphicsSettings) super.clone();
            c.backBufferResolution = this.backBufferResolution.clone();
            c.resolution = this.resolution.clone();
            c.fullScreen = this.fullScreen.clone();
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
            fullScreen.setParent(s);
        }

        @Override
        protected void setupListeners() {
            EventManager.instance.subscribe(this, Event.LIMIT_FPS_CMD,
                    Event.BACKBUFFER_SCALE_CMD);

            fullScreen.setupListeners();
        }

        @Override
        public void dispose() {
            EventManager.instance.removeAllSubscriptions(this);
            fullScreen.dispose();
        }

        @Override
        public void apply() {
            EventManager.publish(Event.LIMIT_FPS_CMD, this, fpsLimit);
            // EventManager.publish(Event.BACKBUFFER_SCALE_CMD, this, (float) backBufferScale);

            fullScreen.apply();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FullscreenSettings extends SettingsObject {
            public boolean active;
            public int[] resolution;
            public int bitDepth;
            public int refreshRate;

            @Override
            public FullscreenSettings clone() {
                var c = (FullscreenSettings) super.clone();
                c.resolution = this.resolution.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SceneSettings extends SettingsObject implements IObserver {
        public String homeObject;
        public long fadeMs;
        public CameraSettings camera;
        public ParticleSettings particleGroups;
        public StarSettings star;
        public LabelSettings label;
        public ProperMotionSettings properMotion;
        public OctreeSettings octree;
        public RendererSettings renderer;
        public CrosshairSettings crosshair;
        public InitializationSettings initialization;
        public Map<String, Boolean> visibility;
        @JsonIgnore
        public double distanceScaleDesktop = 1.0;
        @JsonIgnore
        public double distanceScaleVr = 1.0e4;

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

        @Override
        public void notify(final Event event,
                           Object source,
                           final Object... data) {
            if (isEnabled() && source != this) {
                if (Objects.requireNonNull(event) == Event.TOGGLE_VISIBILITY_CMD) {
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
            }
        }

        @Override
        public SceneSettings clone() {
            var c = (SceneSettings) super.clone();
            c.camera = this.camera.clone();
            c.star = this.star.clone();
            c.renderer = this.renderer.clone();
            c.octree = this.octree.clone();
            c.crosshair = this.crosshair.clone();
            c.properMotion = this.properMotion.clone();
            c.label = this.label.clone();
            c.particleGroups = this.particleGroups.clone();
            c.initialization = this.initialization.clone();
            c.visibility = new HashMap<>(this.visibility);
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
            camera.setParent(s);
            star.setParent(s);
            renderer.setParent(s);
            octree.setParent(s);
            crosshair.setParent(s);
            properMotion.setParent(s);
            label.setParent(s);
            particleGroups.setParent(s);
            initialization.setParent(s);
        }

        @Override
        protected void setupListeners() {
            EventManager.instance.subscribe(this, Event.TOGGLE_VISIBILITY_CMD);

            camera.setupListeners();
            star.setupListeners();
            renderer.setupListeners();
            octree.setupListeners();
            crosshair.setupListeners();
            properMotion.setupListeners();
            label.setupListeners();
            particleGroups.setupListeners();
            initialization.setupListeners();
        }

        @Override
        public void dispose() {
            EventManager.instance.removeAllSubscriptions(this);
            camera.dispose();
            star.dispose();
            renderer.dispose();
            octree.dispose();
            crosshair.dispose();
            properMotion.dispose();
            label.dispose();
            particleGroups.dispose();
            initialization.dispose();
        }

        @Override
        public void apply() {
            var keys = visibility.keySet();
            for (var ctName : keys) {
                var value = visibility.get(ctName);
                var ct = ComponentType.valueOf(ctName);
                EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, ct.key, value);
            }

            camera.apply();
            star.apply();
            renderer.apply();
            octree.apply();
            crosshair.apply();
            properMotion.apply();
            label.apply();
            particleGroups.apply();
            initialization.apply();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CameraSettings extends SettingsObject implements IObserver {
            public int speedLimitIndex;
            @JsonIgnore
            public double speedLimit;
            public double speed;
            public double turn;
            public double rotate;
            public float fov;
            public boolean cinematic;
            public boolean targetMode;
            public FocusSettings focusLock;

            @JsonProperty("speedLimitIndex")
            public void setSpeedLimitIndex(int index) {
                this.speedLimitIndex = index;
                updateSpeedLimit();
            }

            @Override
            public void notify(final Event event,
                               Object source,
                               final Object... data) {
                if (isEnabled() && source != this) {
                    switch (event) {
                        case FOCUS_LOCK_CMD -> focusLock.position = (boolean) data[0];
                        case ORIENTATION_LOCK_CMD -> focusLock.orientation = (boolean) data[0];
                        case FOV_CHANGED_CMD -> {
                            if (!SlaveManager.projectionActive()) {
                                boolean checkMax = source instanceof Actor;
                                fov = MathUtilsDouble.clamp((Float) data[0], Constants.MIN_FOV, checkMax ? Constants.MAX_FOV : 179f);
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
            }

            public void updateSpeedLimit() {
                switch (speedLimitIndex) {
                    case 0 ->
                        // 1 km/h is 0.00027 km/s
                            speedLimit = 0.000277777778 * Constants.KM_TO_U;
                    case 1 ->
                        // 10 km/h is 0.0027 km/s
                            speedLimit = 0.00277777778 * Constants.KM_TO_U;
                    case 2 ->
                        // 100 km/h is 0.027 km/s
                            speedLimit = 0.0277777778 * Constants.KM_TO_U;
                    case 3 ->
                        // 1000 km/h is 0.27 km/s
                            speedLimit = 0.277777778 * Constants.KM_TO_U;
                    case 4 ->
                        // 1 km/s
                            speedLimit = Constants.KM_TO_U;
                    case 5 ->
                        // 10 km/s
                            speedLimit = Constants.KM_TO_U;
                    case 6 ->
                        // 100 km/s
                            speedLimit = Constants.KM_TO_U;
                    case 7 ->
                        // 1000 km/h
                            speedLimit = 0.277777778 * Constants.KM_TO_U;
                    case 8 -> speedLimit = 0.01 * Nature.C * Constants.M_TO_U;
                    case 9 -> speedLimit = 0.1 * Nature.C * Constants.M_TO_U;
                    case 10 -> speedLimit = 0.5 * Nature.C * Constants.M_TO_U;
                    case 11 -> speedLimit = 0.8 * Nature.C * Constants.M_TO_U;
                    case 12 -> speedLimit = 0.9 * Nature.C * Constants.M_TO_U;
                    case 13 -> speedLimit = 0.99 * Nature.C * Constants.M_TO_U;
                    case 14 -> speedLimit = 0.99999 * Nature.C * Constants.M_TO_U;
                    case 15 -> speedLimit = Nature.C * Constants.M_TO_U;
                    case 16 -> speedLimit = 2.0 * Nature.C * Constants.M_TO_U;
                    case 17 ->
                        // 10 c
                            speedLimit = 10.0 * Nature.C * Constants.M_TO_U;
                    case 18 ->
                        // 1000 c
                            speedLimit = 1000.0 * Nature.C * Constants.M_TO_U;
                    case 19 -> speedLimit = Constants.AU_TO_U;
                    case 20 -> speedLimit = 10.0 * Constants.AU_TO_U;
                    case 21 -> speedLimit = 1000.0 * Constants.AU_TO_U;
                    case 22 -> speedLimit = 10000.0 * Constants.AU_TO_U;
                    case 23 -> speedLimit = Constants.PC_TO_U;
                    case 24 -> speedLimit = 2.0 * Constants.PC_TO_U;
                    case 25 ->
                        // 10 pc/s
                            speedLimit = 10.0 * Constants.PC_TO_U;
                    case 26 ->
                        // 1000 pc/s
                            speedLimit = 1000.0 * Constants.PC_TO_U;
                    case 27 ->
                        // No limit
                            speedLimit = -1;
                }
            }

            @Override
            public CameraSettings clone() {
                var c = (CameraSettings) super.clone();
                c.focusLock = this.focusLock.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
                focusLock.setParent(s);
            }

            @Override
            protected void setupListeners() {
                EventManager.instance.subscribe(this, Event.CAMERA_CINEMATIC_CMD, Event.FOCUS_LOCK_CMD, Event.ORIENTATION_LOCK_CMD, Event.FOV_CHANGED_CMD,
                        Event.CAMERA_SPEED_CMD, Event.ROTATION_SPEED_CMD, Event.TURNING_SPEED_CMD, Event.SPEED_LIMIT_CMD);

                focusLock.setupListeners();
            }

            @Override
            public void dispose() {
                EventManager.instance.removeAllSubscriptions(this);
                focusLock.dispose();
            }

            @Override
            public void apply() {
                EventManager.publish(Event.FOCUS_LOCK_CMD, this, focusLock.position);
                EventManager.publish(Event.ORIENTATION_LOCK_CMD, this, focusLock.orientation);
                EventManager.publish(Event.FOV_CHANGED_CMD, this, fov);
                EventManager.publish(Event.CAMERA_SPEED_CMD, this, (float) speed);
                EventManager.publish(Event.ROTATION_SPEED_CMD, this, (float) rotate);
                EventManager.publish(Event.TURNING_SPEED_CMD, this, (float) turn);
                EventManager.publish(Event.SPEED_LIMIT_CMD, this, speedLimitIndex);
                EventManager.publish(Event.CAMERA_CINEMATIC_CMD, this, cinematic);

                focusLock.apply();
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class FocusSettings extends SettingsObject {
                public boolean position;
                public boolean orientation;

                @Override
                public FocusSettings clone() {
                    return (FocusSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ParticleSettings extends SettingsObject {
            /**
             * Default number of labels for particle groups.
             **/
            public int numLabels = 0;

            @Override
            public ParticleSettings clone() {
                return (ParticleSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StarSettings extends SettingsObject implements IObserver {
            public boolean renderStarSpheres;
            public float brightness;
            public float power;
            public float pointSize;
            /**
             * When close to the stars, this factor controls the amount of glow.
             * It should be set rather low, i.e., in [0.01,0.2].
             **/
            public double glowFactor = 0.06;
            public float[] opacity;
            /**
             * Texture index to use for regular star rendering.
             **/
            public int textureIndex = 4;
            /**
             * Texture index to use for the light glow effect. Usually includes a lens artifact.
             **/
            public int textureIndexLens = 1;
            public GroupSettings group;
            public ThresholdSettings threshold;
            @JsonIgnore
            private float pointSizeBak;

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
                        pointSize = MathUtilsDouble.lint(cubemapRes, 500f, 2000f, 20f, 8f);
                    } else {
                        pointSize = MathUtilsDouble.lint(cubemapRes, 2000f, 3000f, 8f, 4f);
                    }
                    return MathUtils.clamp(pointSize, 0f, 50f) * (settings.scene.star.pointSize / 3.75f) * (
                            MathUtilsDouble.lint(screenArea, 500000f, 8000000f, 1.8f, 4.8f) / 3.75f);
                } else {
                    return settings.scene.star.pointSize;
                }
            }

            @JsonIgnore
            public String getStarTexture(int textureIndex) {
                String starTexIdx = String.format("%02d", textureIndex);
                String texture = settings.data.dataFile(
                        GlobalResources.unpackAssetPath(Constants.DATA_LOCATION_TOKEN + "tex/base/star-tex-" + starTexIdx + Constants.STAR_SUBSTITUTE + ".png"));
                if (!Files.exists(Path.of(texture))) {
                    // Fall back to whatever available
                    for (int i = 1; i < 9; i++) {
                        starTexIdx = String.format("%02d", i);
                        texture = settings.data.dataFile(
                                GlobalResources.unpackAssetPath(Constants.DATA_LOCATION_TOKEN + "tex/base/star-tex-" + starTexIdx + Constants.STAR_SUBSTITUTE + ".png"));
                        if (Files.exists(Path.of(texture)))
                            return texture;
                    }
                } else {
                    return texture;
                }
                return null;
            }

            @JsonIgnore
            public String getStarTexture() {
                return getStarTexture(textureIndex);
            }

            @JsonIgnore
            public String getStarLensTexture() {
                return getStarTexture(textureIndexLens);
            }

            public void setPointSize(float pointSize) {
                this.pointSize = pointSize;
                this.pointSizeBak = pointSize;
            }

            public void notify(final Event event,
                               Object source,
                               final Object... data) {
                if (isEnabled() && source != this) {
                    switch (event) {
                        case STAR_POINT_SIZE_CMD -> pointSize = (float) data[0];
                        case STAR_POINT_SIZE_INCREASE_CMD -> {
                            float size = Math.min(this.pointSize + Constants.SLIDER_STEP_TINY, Constants.MAX_STAR_POINT_SIZE);
                            EventManager.publish(Event.STAR_POINT_SIZE_CMD, this, size);
                        }
                        case STAR_POINT_SIZE_DECREASE_CMD -> {
                            float size = Math.max(this.pointSize - Constants.SLIDER_STEP_TINY, Constants.MIN_STAR_POINT_SIZE);
                            EventManager.publish(Event.STAR_POINT_SIZE_CMD, this, size);
                        }
                        case STAR_POINT_SIZE_RESET_CMD -> this.pointSize = pointSizeBak;
                        case STAR_BASE_LEVEL_CMD -> opacity[0] = (float) data[0];
                        case STAR_GROUP_BILLBOARD_CMD -> group.billboard = (boolean) data[0];
                        case STAR_GROUP_NEAREST_CMD -> {
                            group.numBillboard = (int) data[0];
                            group.numLabels = (int) data[0];
                            group.numVelocityVector = (int) data[0];
                        }
                        case BILLBOARD_TEXTURE_IDX_CMD -> textureIndex = (int) data[0];
                        case STAR_BRIGHTNESS_CMD ->
                                brightness = MathUtilsDouble.clamp((float) data[0], Constants.MIN_STAR_BRIGHTNESS, Constants.MAX_STAR_BRIGHTNESS);
                        case STAR_BRIGHTNESS_POW_CMD -> power = (float) data[0];
                        case STAR_GLOW_FACTOR_CMD -> glowFactor = (float) data[0];
                    }
                }
            }

            @Override
            public StarSettings clone() {
                var c = (StarSettings) super.clone();
                c.opacity = this.opacity.clone();
                c.group = this.group.clone();
                c.threshold = this.threshold.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
                group.setParent(s);
                threshold.setParent(s);
            }

            @Override
            protected void setupListeners() {
                EventManager.instance.subscribe(this, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD, Event.STAR_GLOW_FACTOR_CMD, Event.STAR_POINT_SIZE_CMD,
                        Event.STAR_POINT_SIZE_INCREASE_CMD, Event.STAR_POINT_SIZE_DECREASE_CMD, Event.STAR_POINT_SIZE_RESET_CMD,
                        Event.STAR_BASE_LEVEL_CMD, Event.STAR_GROUP_BILLBOARD_CMD, Event.STAR_GROUP_NEAREST_CMD, Event.BILLBOARD_TEXTURE_IDX_CMD);

                group.setupListeners();
                threshold.setupListeners();
            }

            @Override
            public void dispose() {
                EventManager.instance.removeAllSubscriptions(this);
                group.dispose();
                threshold.dispose();
            }

            @Override
            public void apply() {
                EventManager.publish(Event.STAR_POINT_SIZE_CMD, this, pointSize);
                EventManager.publish(Event.STAR_BASE_LEVEL_CMD, this, opacity[0]);
                EventManager.publish(Event.STAR_GROUP_BILLBOARD_CMD, this, group.billboard);
                EventManager.publish(Event.BILLBOARD_TEXTURE_IDX_CMD, this, textureIndex);
                EventManager.publish(Event.STAR_BRIGHTNESS_CMD, this, brightness);
                EventManager.publish(Event.STAR_BRIGHTNESS_POW_CMD, this, power);
                EventManager.publish(Event.STAR_GLOW_FACTOR_CMD, this, (float) glowFactor);

                group.apply();
                threshold.apply();
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class GroupSettings extends SettingsObject {
                public boolean billboard;
                public int numBillboard;
                public int numLabels = 30;
                public int numVelocityVector;

                /**
                 * Compatibility with old 'numLabel' property, renamed to 'numLabels'.
                 *
                 * @param numLabels The number of labels to render for each star group.
                 */
                public void setNumLabel(int numLabels) {
                    this.numLabels = numLabels;
                }

                @Override
                public GroupSettings clone() {
                    return (GroupSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ThresholdSettings extends SettingsObject {
                public double quad;
                public double point;
                public double none;

                @Override
                public ThresholdSettings clone() {
                    return (ThresholdSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class LabelSettings extends SettingsObject implements IObserver {
            public float size;
            public float number;

            @Override
            public void notify(final Event event,
                               Object source,
                               final Object... data) {
                if (isEnabled() && source != this) {
                    if (event == Event.LABEL_SIZE_CMD) {
                        size = MathUtilsDouble.clamp((float) data[0], Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE);
                    }
                }
            }

            @Override
            public LabelSettings clone() {
                return (LabelSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
                EventManager.instance.subscribe(this, Event.LABEL_SIZE_CMD);
            }

            @Override
            public void dispose() {
                EventManager.instance.removeAllSubscriptions(this);
            }

            @Override
            public void apply() {
                EventManager.publish(Event.LABEL_SIZE_CMD, this, size);
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ProperMotionSettings extends SettingsObject implements IObserver {
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

            @Override
            public void notify(final Event event,
                               Object source,
                               final Object... data) {
                if (isEnabled() && source != this) {
                    switch (event) {
                        case PM_NUM_FACTOR_CMD ->
                                number = MathUtilsDouble.clamp((float) data[0], Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR);
                        case PM_LEN_FACTOR_CMD ->
                                length = MathUtilsDouble.clamp((float) data[0], Constants.MIN_PM_LEN_FACTOR, Constants.MAX_PM_LEN_FACTOR);
                        case PM_COLOR_MODE_CMD -> colorMode = MathUtilsDouble.clamp((int) data[0], 0, 5);
                        case PM_ARROWHEADS_CMD -> arrowHeads = (boolean) data[0];
                    }
                }
            }

            @Override
            public ProperMotionSettings clone() {
                return (ProperMotionSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
                EventManager.instance.subscribe(this, Event.PM_LEN_FACTOR_CMD, Event.PM_NUM_FACTOR_CMD, Event.PM_COLOR_MODE_CMD, Event.PM_ARROWHEADS_CMD);
            }

            @Override
            public void dispose() {
                EventManager.instance.removeAllSubscriptions(this);
            }

            @Override
            public void apply() {
                EventManager.publish(Event.PM_NUM_FACTOR_CMD, this, (float) number);
                EventManager.publish(Event.PM_LEN_FACTOR_CMD, this, (float) length);
                EventManager.publish(Event.PM_COLOR_MODE_CMD, this, colorMode);
                EventManager.publish(Event.PM_ARROWHEADS_CMD, this, arrowHeads);
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class OctreeSettings extends SettingsObject implements IObserver {
            public int maxStars;
            public float[] threshold;
            public boolean fade;

            @Override
            public void notify(Event event,
                               Object source,
                               Object... data) {
                if (isEnabled() && source != this) {
                    if (event == Event.OCTREE_PARTICLE_FADE_CMD) {
                        fade = (boolean) data[0];
                    }
                }
            }

            @Override
            public OctreeSettings clone() {
                var c = (OctreeSettings) super.clone();
                c.threshold = this.threshold.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
                EventManager.instance.subscribe(this, Event.OCTREE_PARTICLE_FADE_CMD);
            }

            @Override
            public void dispose() {
                EventManager.instance.removeAllSubscriptions(this);
            }

            @Override
            public void apply() {
                EventManager.publish(Event.OCTREE_PARTICLE_FADE_CMD, this, fade);
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RendererSettings extends SettingsObject implements IObserver {
            public PointCloudMode pointCloud = PointCloudMode.POINTS;
            public double ambient;
            public LineSettings line;
            public ShadowSettings shadow;
            public EclipseSettings eclipses;
            public ElevationSettings elevation;
            public VirtualTextureSettings virtualTextures;
            @JsonIgnore
            public double orbitSolidAngleThreshold = Math.toRadians(1.5);


            @JsonProperty("pointCloud")
            public void setPointCloud(String pointCloud) {
                if (pointCloud == null || pointCloud.isEmpty()) {
                    // Default
                    pointCloud = "POINTS";
                }
                if (pointCloud.startsWith("GL_")) {
                    pointCloud = pointCloud.substring(3);
                }
                if (pointCloud.equalsIgnoreCase("TRIANGLES_INSTANCED")) {
                    pointCloud = "TRIANGLES";
                }
                this.pointCloud = PointCloudMode.valueOf(pointCloud.toUpperCase(Locale.ROOT));
            }

            @Override
            public void notify(final Event event,
                               Object source,
                               final Object... data) {
                if (isEnabled() && source != this) {
                    switch (event) {
                        case AMBIENT_LIGHT_CMD -> ambient = (float) data[0];
                        case ELEVATION_MULTIPLIER_CMD ->
                                elevation.multiplier = MathUtilsDouble.clamp((float) data[0], Constants.MIN_ELEVATION_MULT, Constants.MAX_ELEVATION_MULT);
                        case ELEVATION_TYPE_CMD -> elevation.type = (ElevationType) data[0];
                        case TESSELLATION_QUALITY_CMD -> elevation.quality = (float) data[0];
                        case ORBIT_SOLID_ANGLE_TH_CMD -> orbitSolidAngleThreshold = (double) data[0];
                        case SVT_CACHE_SIZE_CMD -> virtualTextures.cacheSize = (int) data[0];
                    }
                }
            }

            @Override
            public RendererSettings clone() {
                var c = (RendererSettings) super.clone();
                c.line = this.line.clone();
                c.shadow = this.shadow.clone();
                c.eclipses = this.eclipses.clone();
                c.elevation = this.elevation.clone();
                c.virtualTextures = this.virtualTextures.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
                line.setParent(s);
                shadow.setParent(s);
                eclipses.setParent(s);
                elevation.setParent(s);
                virtualTextures.setParent(s);
            }

            @Override
            protected void setupListeners() {
                EventManager.instance.subscribe(this, Event.AMBIENT_LIGHT_CMD, Event.ELEVATION_MULTIPLIER_CMD, Event.ELEVATION_TYPE_CMD, Event.TESSELLATION_QUALITY_CMD,
                        Event.ORBIT_SOLID_ANGLE_TH_CMD, Event.SVT_CACHE_SIZE_CMD);

                line.setupListeners();
                shadow.setupListeners();
                eclipses.setupListeners();
                elevation.setupListeners();
                virtualTextures.setupListeners();
            }

            @Override
            public void dispose() {
                EventManager.instance.removeAllSubscriptions(this);
                line.dispose();
                shadow.dispose();
                eclipses.dispose();
                elevation.dispose();
                virtualTextures.dispose();
            }

            @Override
            public void apply() {
                EventManager.publish(Event.AMBIENT_LIGHT_CMD, this, (float) ambient);
                EventManager.publish(Event.ELEVATION_MULTIPLIER_CMD, this, (float) elevation.multiplier);
                EventManager.publish(Event.ELEVATION_TYPE_CMD, this, elevation.type);
                EventManager.publish(Event.TESSELLATION_QUALITY_CMD, this, (float) elevation.quality);
                EventManager.publish(Event.ORBIT_SOLID_ANGLE_TH_CMD, this, orbitSolidAngleThreshold);
                EventManager.publish(Event.SVT_CACHE_SIZE_CMD, this, virtualTextures.cacheSize);

                line.apply();
                shadow.apply();
                eclipses.apply();
                elevation.apply();
                virtualTextures.apply();
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class LineSettings extends SettingsObject implements IObserver {
                public LineMode mode = LineMode.POLYLINE_QUADSTRIP;
                public float width;
                public float glWidthBias = 0;

                @JsonIgnore
                public boolean isNormalLineRenderer() {
                    return mode.equals(LineMode.GL_LINES);
                }

                @JsonIgnore
                public boolean isQuadLineRenderer() {
                    return mode.equals(LineMode.POLYLINE_QUADSTRIP);
                }

                @Override
                public LineSettings clone() {
                    return (LineSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                    EventManager.instance.subscribe(this, Event.LINE_WIDTH_CMD);
                }

                @Override
                public void notify(Event event, Object source, Object... data) {
                    if (isEnabled() && source != this) {
                        if (Objects.requireNonNull(event) == Event.LINE_WIDTH_CMD)
                            width = MathUtilsDouble.clamp((float) data[0], Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH);
                    }

                }

                @Override
                public void dispose() {
                    EventManager.instance.removeAllSubscriptions(this);
                }

                @Override
                public void apply() {
                    EventManager.publish(Event.LINE_WIDTH_CMD, this, width);
                }
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ShadowSettings extends SettingsObject {
                public boolean active;
                public int resolution;
                public int number;

                @Override
                public ShadowSettings clone() {
                    return (ShadowSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class EclipseSettings extends SettingsObject {
                public boolean active;
                public boolean outlines;

                @Override
                public EclipseSettings clone() {
                    return (EclipseSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ElevationSettings extends SettingsObject {
                public ElevationType type;
                public double multiplier;
                public double quality;

                public void setType(String typeString) {
                    // Parallax mapping discontinued after 3.5.5-2.
                    if (typeString.equalsIgnoreCase("PARALLAX_MAPPING")) {
                        typeString = "REGULAR";
                    }
                    this.type = ElevationType.valueOf(typeString.toUpperCase());
                }

                @Override
                public ElevationSettings clone() {
                    return (ElevationSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class VirtualTextureSettings extends SettingsObject {
                /**
                 * Cache size, in tiles.
                 **/
                public int cacheSize;
                /**
                 * The tile detection buffer is smaller than the main window by this factor.
                 **/
                public double detectionBufferFactor;
                /**
                 * Maximum number of tiles to load each frame.
                 **/
                public int maxTilesPerFrame = 8;

                @Override
                public VirtualTextureSettings clone() {
                    return (VirtualTextureSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }

        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CrosshairSettings extends SettingsObject implements IObserver {
            public boolean focus;
            public boolean closest;
            public boolean home;

            @Override
            public void notify(final Event event,
                               Object source,
                               final Object... data) {
                if (isEnabled() && source != this) {
                    switch (event) {
                        case CROSSHAIR_FOCUS_CMD -> focus = (boolean) data[0];
                        case CROSSHAIR_CLOSEST_CMD -> closest = (boolean) data[0];
                        case CROSSHAIR_HOME_CMD -> home = (boolean) data[0];
                        default -> {
                        }
                    }
                }
            }

            @Override
            public CrosshairSettings clone() {
                return (CrosshairSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
                EventManager.instance.subscribe(this, Event.CROSSHAIR_FOCUS_CMD, Event.CROSSHAIR_CLOSEST_CMD, Event.CROSSHAIR_HOME_CMD);
            }

            @Override
            public void dispose() {
                EventManager.instance.removeAllSubscriptions(this);
            }

            @Override
            public void apply() {
                EventManager.publish(Event.CROSSHAIR_FOCUS_CMD, this, focus);
                EventManager.publish(Event.CROSSHAIR_CLOSEST_CMD, this, closest);
                EventManager.publish(Event.CROSSHAIR_HOME_CMD, this, home);
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class InitializationSettings extends SettingsObject {
            public boolean lazyTexture;
            public boolean lazyMesh;

            @Override
            public InitializationSettings clone() {
                return (InitializationSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgramSettings extends SettingsObject implements IObserver {
        public boolean safeMode;
        /**
         * Flag to mark whether safe mode is activated via command line argument.
         */
        @JsonIgnore
        public boolean safeModeFlag;
        public boolean debugInfo;
        public boolean offlineMode;
        public boolean shaderCache = false;
        public boolean saveProceduralTextures = false;
        /** Show time in no-GUI mode. **/
        public boolean displayTimeNoUi = true;
        public MinimapSettings minimap;
        public FileChooserSettings fileChooser;
        public PointerSettings pointer;
        public RecursiveGridSettings recursiveGrid;
        public ModeStereoSettings modeStereo;
        public ModeCubemapSettings modeCubemap;
        public NetSettings net;
        public UiSettings ui;
        public boolean exitConfirmation;
        public String locale;
        public UpdateSettings update;
        public UrlSettings url;
        public DefaultTimeZone timeZone = DefaultTimeZone.UTC;

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

        @JsonProperty("timeZone")
        public void setTimeZone(String timeZone) {
            if (timeZone == null || timeZone.isEmpty()) {
                // Default
                timeZone = "UTC";
            }
            this.timeZone = DefaultTimeZone.valueOf(timeZone.toUpperCase(Locale.ROOT));
        }

        @Override
        public void notify(final Event event,
                           Object source,
                           final Object... data) {
            if (isEnabled() && source != this) {
                switch (event) {
                    case STEREOSCOPIC_CMD -> {
                        modeStereo.active = (boolean) (Boolean) data[0];
                        if (modeStereo.active && modeCubemap.active) {
                            modeStereo.active = false;
                            EventManager.publish(Event.DISPLAY_GUI_CMD, this, true, I18n.msg("notif.cleanmode"));
                        }
                    }
                    case STEREO_PROFILE_CMD -> modeStereo.profile = StereoProfile.values()[(Integer) data[0]];
                    case CUBEMAP_CMD -> {
                        modeCubemap.active = (Boolean) data[0] && !Settings.settings.runtime.openXr;
                        if (modeCubemap.active) {
                            modeCubemap.projection = (CubemapProjection) data[1];

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
                                String[] keysStrProj = KeyBindings.instance.getStringArrayKeys("action.toggle/element.planetarium.projection");
                                mpi.title = I18n.msg("gui.planetarium.title");
                                mpi.header = I18n.msg("gui.planetarium.notice.header");
                                mpi.addMapping(I18n.msg("gui.planetarium.notice.back"), keysStr);
                                mpi.addMapping(I18n.msg("gui.360.notice.projection"), keysStrProj);
                                if (settings.scene.renderer.pointCloud.isPoints()) {
                                    mpi.warn = I18n.msg("gui.360.notice.renderer");
                                }
                            } else if (modeCubemap.projection.isOrthosphere()) {
                                String[] keysStrToggle = KeyBindings.instance.getStringArrayKeys("action.toggle/element.orthosphere");
                                String[] keysStrProfile = KeyBindings.instance.getStringArrayKeys("action.toggle/element.orthosphere.profile");
                                mpi.title = I18n.msg("gui.orthosphere.title");
                                mpi.header = I18n.msg("gui.orthosphere.notice.header");
                                mpi.addMapping(I18n.msg("gui.orthosphere.notice.back"), keysStrToggle);
                                mpi.addMapping(I18n.msg("gui.orthosphere.notice.profile"), keysStrProfile);
                            }

                            EventManager.publish(Event.MODE_POPUP_CMD, this, mpi, "cubemap", 10f);
                        } else {
                            EventManager.publish(Event.MODE_POPUP_CMD, this, null, "cubemap");
                            EventManager.publish(Event.FOV_CHANGED_CMD, this, GaiaSky.instance.cameraManager.getCamera().fieldOfView);
                        }
                    }
                    case CUBEMAP_PROJECTION_CMD -> {
                        modeCubemap.projection = (CubemapProjection) data[0];
                        logger.info(I18n.msg("gui.360.projection", modeCubemap.projection.toString()));
                    }
                    case PLANETARIUM_PROJECTION_CMD -> {
                        modeCubemap.projection = (CubemapProjection) data[0];
                        if (modeCubemap.projection.isSphericalMirror() && modeCubemap.planetarium.sphericalMirrorWarp == null) {
                            modeCubemap.projection = CubemapProjection.AZIMUTHAL_EQUIDISTANT;
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.planetarium.sphericalmirror.nowarpfile"), 10f);
                        } else {
                            logger.info(I18n.msg("gui.360.projection", modeCubemap.projection.toString()));
                        }
                    }
                    case INDEXOFREFRACTION_CMD -> modeCubemap.celestialSphereIndexOfRefraction = (float) data[0];
                    case CUBEMAP_RESOLUTION_CMD -> modeCubemap.faceResolution = (int) data[0];
                    case SHOW_MINIMAP_ACTION -> minimap.active = (boolean) (Boolean) data[0];
                    case TOGGLE_MINIMAP -> minimap.active = !minimap.active;
                    case PLANETARIUM_APERTURE_CMD -> modeCubemap.planetarium.aperture = (float) data[0];
                    case PLANETARIUM_ANGLE_CMD -> modeCubemap.planetarium.angle = (float) data[0];
                    case PLANETARIUM_GEOMETRYWARP_FILE_CMD ->
                            modeCubemap.planetarium.sphericalMirrorWarp = (Path) data[0];
                    case POINTER_GUIDES_CMD -> {
                        if (data.length > 0 && data[0] != null) {
                            pointer.guides.active = (boolean) data[0];
                            if (data.length > 1 && data[1] != null) {
                                pointer.guides.color = (float[]) data[1];
                                if (data.length > 2 && data[2] != null) {
                                    pointer.guides.width = (float) data[2];
                                }
                            }
                        }
                    }
                    case RECURSIVE_GRID_ANIMATE_CMD -> {
                        if (data.length > 0 && data[0] != null) {
                            recursiveGrid.animate = (boolean) data[0];
                        }
                    }
                    case UI_SCALE_CMD -> ui.scale = (Float) data[0];
                    default -> {
                    }
                }
            }
        }

        @Override
        public ProgramSettings clone() {
            var c = (ProgramSettings) super.clone();
            c.minimap = this.minimap.clone();
            c.fileChooser = this.fileChooser.clone();
            c.pointer = this.pointer.clone();
            c.ui = this.ui.clone();
            c.net = this.net.clone();
            c.modeCubemap = this.modeCubemap.clone();
            c.modeStereo = this.modeStereo.clone();
            c.recursiveGrid = this.recursiveGrid.clone();
            c.url = this.url.clone();
            c.update = this.update.clone();
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
            minimap.setParent(s);
            fileChooser.setParent(s);
            ui.setParent(s);
            net.setParent(s);
            modeCubemap.setParent(s);
            modeStereo.setParent(s);
            recursiveGrid.setParent(s);
            url.setParent(s);
            update.setParent(s);

        }

        @Override
        protected void setupListeners() {
            EventManager.instance.subscribe(this, Event.STEREOSCOPIC_CMD, Event.STEREO_PROFILE_CMD,
                    Event.CUBEMAP_CMD, Event.CUBEMAP_PROJECTION_CMD, Event.PLANETARIUM_PROJECTION_CMD,
                    Event.INDEXOFREFRACTION_CMD, Event.SHOW_MINIMAP_ACTION, Event.TOGGLE_MINIMAP,
                    Event.PLANETARIUM_APERTURE_CMD, Event.PLANETARIUM_ANGLE_CMD, Event.CUBEMAP_PROJECTION_CMD,
                    Event.PLANETARIUM_GEOMETRYWARP_FILE_CMD, Event.CUBEMAP_RESOLUTION_CMD, Event.POINTER_GUIDES_CMD,
                    Event.UI_SCALE_CMD, Event.RECURSIVE_GRID_ANIMATE_CMD);

            minimap.setupListeners();
            fileChooser.setupListeners();
            ui.setupListeners();
            net.setupListeners();
            modeCubemap.setupListeners();
            modeStereo.setupListeners();
            recursiveGrid.setupListeners();
            url.setupListeners();
            update.setupListeners();
        }

        @Override
        public void dispose() {
            EventManager.instance.removeAllSubscriptions(this);
            minimap.dispose();
            fileChooser.dispose();
            ui.dispose();
            net.dispose();
            modeCubemap.dispose();
            modeStereo.dispose();
            recursiveGrid.dispose();
            url.dispose();
            update.dispose();
        }

        @Override
        public void apply() {
            EventManager.publish(Event.INDEXOFREFRACTION_CMD, this, modeCubemap.celestialSphereIndexOfRefraction);
            EventManager.publish(Event.CUBEMAP_RESOLUTION_CMD, this, modeCubemap.faceResolution);
            EventManager.publish(Event.PLANETARIUM_APERTURE_CMD, this, modeCubemap.planetarium.aperture);
            EventManager.publish(Event.PLANETARIUM_GEOMETRYWARP_FILE_CMD, this, modeCubemap.planetarium.sphericalMirrorWarp);
            EventManager.publish(Event.PLANETARIUM_ANGLE_CMD, this, modeCubemap.planetarium.angle);
            EventManager.publish(Event.POINTER_GUIDES_CMD, this, pointer.guides.active, pointer.guides.color, pointer.guides.width);
            EventManager.publish(Event.RECURSIVE_GRID_ANIMATE_CMD, this, recursiveGrid.animate);

            // Those need to run in the main thread, as they may need the OpenGL context.
            GaiaSky.postRunnable(() -> {
                //     EventManager.publish(Event.STEREOSCOPIC_CMD, this, modeStereo.active);
                //     EventManager.publish(Event.STEREO_PROFILE_CMD, this, modeStereo.profile.ordinal());
                //     EventManager.publish(Event.CUBEMAP_CMD, this, modeCubemap.active, modeCubemap.projection);
                EventManager.publish(Event.SHOW_MINIMAP_ACTION, this, minimap.active);

                EventManager.publish(Event.UI_SCALE_CMD, this, ui.scale);
            });

            minimap.apply();
            fileChooser.apply();
            ui.apply();
            net.apply();
            modeCubemap.apply();
            modeStereo.apply();
            recursiveGrid.apply();
            url.apply();
            update.apply();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class MinimapSettings extends SettingsObject {
            public boolean active;
            public float size;
            public boolean inWindow = false;

            @Override
            public MinimapSettings clone() {
                return (MinimapSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FileChooserSettings extends SettingsObject {
            public boolean showHidden;
            public String lastLocation;

            @Override
            public FileChooserSettings clone() {
                return (FileChooserSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PointerSettings extends SettingsObject {
            public boolean coordinates;
            public GuidesSettings guides;

            @Override
            public PointerSettings clone() {
                var c = (PointerSettings) super.clone();
                c.guides = this.guides.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
                guides.setParent(s);
            }

            @Override
            protected void setupListeners() {
                guides.setupListeners();
            }

            @Override
            public void dispose() {
                guides.dispose();
            }

            @Override
            public void apply() {
                guides.apply();
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class GuidesSettings extends SettingsObject {
                public boolean active;
                public float[] color;
                public float width;

                @Override
                public GuidesSettings clone() {
                    var c = (GuidesSettings) super.clone();
                    c.color = this.color.clone();
                    return c;
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RecursiveGridSettings extends SettingsObject {
            public OriginType origin;
            public GridStyle style;
            public boolean animate = false;

            public boolean projectionLines;

            public void setOrigin(final String originString) {
                try {
                    origin = OriginType.valueOf(originString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Default.
                    origin = OriginType.REFSYS;
                }
            }

            public void setStyle(final String styleString) {
                try {
                    style = GridStyle.valueOf(styleString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Default.
                    style = GridStyle.CIRCULAR;
                }
            }

            @Override
            public RecursiveGridSettings clone() {
                return (RecursiveGridSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ModeStereoSettings extends SettingsObject {
            public boolean active;
            public StereoProfile profile;
            @JsonIgnore
            public float eyeSeparation = 1f;

            public void setProfile(String profileString) {
                if (profileString.equalsIgnoreCase("ANAGLYPH")) {
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

            @Override
            public ModeStereoSettings clone() {
                return (ModeStereoSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ModeCubemapSettings extends SettingsObject {
            public boolean active;
            public CubemapProjection projection;
            public int faceResolution;
            public PlanetariumSettings planetarium;
            public float celestialSphereIndexOfRefraction;

            public void setProjection(final String projectionString) {
                projection = CubemapProjection.valueOf(projectionString.toUpperCase());
            }

            /**
             * Checks whether the program is in planetarium mode.
             *
             * @return Whether planetarium mode is on.
             */
            @JsonIgnore
            public boolean isPlanetariumOn() {
                return active && projection.isPlanetarium();
            }

            /**
             * Checks whether the program is in panorama mode.
             *
             * @return Whether panorama mode is on.
             */
            @JsonIgnore
            public boolean isPanoramaOn() {
                return active && projection.isPanorama();
            }

            /**
             * Checks whether the program is in orthosphere view mode.
             *
             * @return Whether orthosphere view mode is on.
             */
            @JsonIgnore
            public boolean isOrthosphereOn() {
                return active && projection.isOrthosphere();
            }

            /**
             * Checks whether we are in fixed fov mode (slave, planetarium, panorama).
             *
             * @return Whether we are in a fixed-fov mode.
             */
            @JsonIgnore
            public boolean isFixedFov() {
                return isPanoramaOn() || isPlanetariumOn();
            }

            @Override
            public ModeCubemapSettings clone() {
                var c = (ModeCubemapSettings) super.clone();
                c.planetarium = this.planetarium.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
                planetarium.setParent(s);
            }

            @Override
            protected void setupListeners() {
                planetarium.setupListeners();
            }

            @Override
            public void dispose() {
                planetarium.dispose();
            }

            @Override
            public void apply() {
                planetarium.apply();
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class PlanetariumSettings extends SettingsObject {
                public float aperture;
                public float angle;
                public Path sphericalMirrorWarp;

                public void setSphericalMirrorWarp(String file) {
                    if (file != null) {
                        try {
                            sphericalMirrorWarp = Path.of(file);
                        } catch (InvalidPathException e) {
                            logger.error("Invalid spherical mirror mesh warp file path: " + file);
                            logger.error(e);
                        }
                    }
                }

                public String getSphericalMirrorWarp() {
                    if (sphericalMirrorWarp != null) {
                        return sphericalMirrorWarp.toString();
                    } else {
                        return null;
                    }
                }

                @Override
                public PlanetariumSettings clone() {
                    return (PlanetariumSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class NetSettings extends SettingsObject {
            public int restPort;
            public MasterSettings master;
            public SlaveSettings slave;

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

            @Override
            public NetSettings clone() {
                var c = (NetSettings) super.clone();
                c.master = this.master.clone();
                c.slave = this.slave.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
                master.setParent(s);
                slave.setParent(s);
            }

            @Override
            protected void setupListeners() {
                master.setupListeners();
                slave.setupListeners();
            }

            @Override
            public void dispose() {
                master.dispose();
                slave.dispose();
            }

            @Override
            public void apply() {
                master.apply();
                slave.apply();
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class MasterSettings extends SettingsObject {
                public boolean active;
                public List<String> slaves;

                @Override
                public MasterSettings clone() {
                    var c = (MasterSettings) super.clone();
                    c.slaves = new ArrayList<>(this.slaves);
                    return c;
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class SlaveSettings extends SettingsObject {
                public boolean active;
                public String configFile;
                public String warpFile;
                public String blendFile;
                public float yaw;
                public float pitch;
                public float roll;

                @Override
                public SlaveSettings clone() {
                    return (SlaveSettings) super.clone();
                }

                @Override
                protected void setParentRecursive(SettingsObject s) {
                }

                @Override
                protected void setupListeners() {
                }

                @Override
                public void dispose() {
                }

                @Override
                public void apply() {
                }
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UiSettings extends SettingsObject {
            public String theme;
            public float scale;
            public long animationMs = 600;
            public boolean newUI = true;
            public boolean modeChangeInfo;
            public DistanceUnits distanceUnits;

            /**
             * Never use this method to get the scale, use the field itself, it is public.
             */
            public float getScale() {
                return MathUtilsDouble.lint(scale, Constants.UI_SCALE_INTERNAL_MIN, Constants.UI_SCALE_INTERNAL_MAX, Constants.UI_SCALE_MIN, Constants.UI_SCALE_MAX);
            }

            @JsonIgnore
            public boolean isUINightMode() {
                return theme.contains("night");
            }

            @JsonIgnore
            public boolean isHiDPITheme() {
                return scale > 1.5;
            }

            @JsonIgnore
            public float getAnimationSeconds() {
                return animationMs / 1000f;
            }

            @JsonProperty("distanceUnits")
            public void setDistanceUnits(String distanceUnits) {
                if (distanceUnits == null || distanceUnits.isEmpty()) {
                    // Default
                    distanceUnits = "PC";
                }
                this.distanceUnits = DistanceUnits.valueOf(distanceUnits.toUpperCase());
            }

            @Override
            public UiSettings clone() {
                return (UiSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UpdateSettings extends SettingsObject {
            // Update checker time, in ms
            @JsonIgnore
            public static long VERSION_CHECK_INTERVAL_MS = 86400000L;
            public Instant lastCheck;
            public String lastVersion;

            @JsonIgnore
            public String getLastCheckedString() {
                DateTimeFormatter df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(ZoneOffset.UTC);
                return df.format(lastCheck);
            }

            @Override
            public UpdateSettings clone() {
                var c = (UpdateSettings) super.clone();
                c.lastCheck = Instant.ofEpochMilli(this.lastCheck.toEpochMilli());
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UrlSettings extends SettingsObject {
            public String versionCheck;
            public String dataMirror = "https://gaia.ari.uni-heidelberg.de/gaiasky/files/repository/";
            public String dataDescriptor;

            public void setVersionCheck(String versionCheck) {
                // Make sure it points to codeberg.
                if (versionCheck.startsWith("https://gitlab.com")) {
                    versionCheck = "https://codeberg.org/api/v1/repos/gaiasky/gaiasky/tags";
                }
                this.versionCheck = versionCheck;
            }

            @Override
            public UrlSettings clone() {
                return (UrlSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ControlsSettings extends SettingsObject {
        public GamepadSettings gamepad;

        @Override
        public ControlsSettings clone() {
            var c = (ControlsSettings) super.clone();
            c.gamepad = this.gamepad.clone();
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
            gamepad.setParent(s);
        }

        @Override
        protected void setupListeners() {
        }

        @Override
        public void dispose() {
            gamepad.dispose();
        }

        @Override
        public void apply() {
            gamepad.apply();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class GamepadSettings extends SettingsObject implements IObserver {
            /**
             * Keep track of added controller listeners
             */
            private final Map<Controller, Set<ControllerListener>> controllerListenersMap = new HashMap<>();
            public String mappingsFile;
            public boolean invertX;
            public boolean invertY;
            public String[] blacklist;

            public GamepadSettings clone() {
                var c = (GamepadSettings) super.clone();
                c.controllerListenersMap.clear();
                c.controllerListenersMap.putAll(this.controllerListenersMap);

                c.blacklist = this.blacklist.clone();
                return c;
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
                EventManager.instance.subscribe(this, Event.INVERT_X_CMD, Event.INVERT_Y_CMD);
            }

            @Override
            public void dispose() {
                EventManager.instance.removeAllSubscriptions(this);
            }

            public boolean isControllerBlacklisted(String controllerName) {
                if (blacklist == null) {
                    return false;
                } else {
                    for (String cn : blacklist) {
                        if (controllerName.equalsIgnoreCase(cn))
                            return true;
                    }
                }
                return false;
            }

            /**
             * Adds the given listener to the given controller in our local map.
             *
             * @param controller         The controller.
             * @param controllerListener The listener.
             */
            private void addListener(Controller controller,
                                     ControllerListener controllerListener) {
                if (!controllerListenersMap.containsKey(controller)) {
                    Set<ControllerListener> cs = new HashSet<>();
                    cs.add(controllerListener);
                    controllerListenersMap.put(controller, cs);
                } else {
                    Set<ControllerListener> cs = controllerListenersMap.get(controller);
                    cs.add(controllerListener);
                }
                activate(controllerListener);
            }

            private void activate(ControllerListener l) {
                if (l instanceof AbstractGamepadListener) {
                    ((AbstractGamepadListener) l).activate();
                }
            }

            /**
             * Removes the given listener from the given controller in our local map.
             *
             * @param controller         The controller.
             * @param controllerListener The listener.
             */
            private void removeListener(Controller controller,
                                        ControllerListener controllerListener) {
                if (controllerListenersMap.containsKey(controller)) {
                    Set<ControllerListener> cs = controllerListenersMap.get(controller);
                    cs.remove(controllerListener);
                    deactivate(controllerListener);
                }
            }

            private void deactivate(ControllerListener l) {
                if (l instanceof AbstractGamepadListener) {
                    ((AbstractGamepadListener) l).deactivate();
                }
            }

            /**
             * Adds the given controller listeners to all detected controllers.
             *
             * @param listener The controller listener.
             */
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

            /**
             * Adds the given controller listener to the controller with the given name, if it is not
             * blacklisted.
             *
             * @param listener       The controller listener.
             * @param controllerName The controller name.
             */
            public void addControllerListener(ControllerListener listener,
                                              String controllerName) {
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

            /**
             * Removes the given listener from all controllers.
             *
             * @param listener The listener to remove.
             */
            public void removeControllerListener(ControllerListener listener) {
                Array<Controller> controllers = Controllers.getControllers();
                for (Controller controller : controllers) {
                    if (!isControllerBlacklisted(controller.getName())) {
                        controller.removeListener(listener);
                        removeListener(controller, listener);
                    }
                }
            }

            /**
             * Removes all controller listeners from all controllers.
             */
            public void removeAllControllerListeners() {
                Array<Controller> controllers = Controllers.getControllers();
                for (Controller controller : controllers) {
                    if (!isControllerBlacklisted(controller.getName())) {
                        Set<ControllerListener> s = controllerListenersMap.get(controller);
                        if (s != null) {
                            for (ControllerListener cl : s) {
                                controller.removeListener(cl);
                                deactivate(cl);
                            }
                            s.clear();
                        }
                    }
                }
            }

            /**
             * Returns a copy of the current controller listeners for the first detected controller.
             *
             * @return A set with all current controller listeners.
             */
            @JsonIgnore
            public Set<ControllerListener> getControllerListeners() {
                Array<Controller> controllers = Controllers.getControllers();
                if (!controllers.isEmpty()) {
                    Controller c = controllers.get(0);
                    if (controllerListenersMap.containsKey(c) && controllerListenersMap.get(c) != null) {
                        return new HashSet<>(controllerListenersMap.get(c));
                    } else {
                        return new HashSet<>();
                    }
                }
                return new HashSet<>();
            }

            /**
             * Adds all controller listeners in the set to all detected controllers.
             *
             * @param controllerListeners The listeners.
             */
            @JsonIgnore
            public void setControllerListeners(Set<ControllerListener> controllerListeners) {
                if (controllerListeners != null) {
                    for (ControllerListener listener : controllerListeners) {
                        addControllerListener(listener);
                    }
                }
            }

            @Override
            public void notify(Event event,
                               Object source,
                               Object... data) {
                if (isEnabled() && source != this) {
                    switch (event) {
                        case INVERT_X_CMD -> this.invertX = (Boolean) data[0];
                        case INVERT_Y_CMD -> this.invertY = (Boolean) data[0];
                    }
                }
            }

            @Override
            public void apply() {
                EventManager.publish(Event.INVERT_X_CMD, this, invertX);
                EventManager.publish(Event.INVERT_Y_CMD, this, invertY);
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScreenshotSettings extends SettingsObject implements IObserver {
        public static final int MIN_SCREENSHOT_SIZE = 50;
        public static final int MAX_SCREENSHOT_SIZE = 25000;

        public String location;
        public ImageFormat format;
        public float quality;
        public ScreenshotMode mode;
        public int[] resolution;

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
        public void notify(Event event,
                           Object source,
                           Object... data) {
            if (isEnabled() && source != this) {
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

        @Override
        public ScreenshotSettings clone() {
            var c = (ScreenshotSettings) super.clone();
            c.resolution = this.resolution.clone();
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
        }

        @Override
        protected void setupListeners() {
            EventManager.instance.subscribe(this, Event.CONFIG_SCREENSHOT_CMD, Event.SCREENSHOT_MODE_CMD);
        }

        @Override
        public void dispose() {
            EventManager.instance.removeAllSubscriptions(this);
        }

        @Override
        public void apply() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FrameSettings extends ScreenshotSettings implements IObserver {
        @JsonIgnore
        public boolean active;
        public String prefix;
        public boolean time;
        public double targetFps;

        @Override
        protected void setupListeners() {
            EventManager.instance.subscribe(this, Event.CONFIG_FRAME_OUTPUT_CMD, Event.FRAME_OUTPUT_CMD, Event.FRAME_OUTPUT_MODE_CMD);
        }

        @Override
        public void notify(final Event event,
                           Object source,
                           final Object... data) {
            if (isEnabled() && source != this) {
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

        @Override
        public FrameSettings clone() {
            return (FrameSettings) super.clone();
        }

        @Override
        public void apply() {
            EventManager.publish(Event.FRAME_OUTPUT_MODE_CMD, this, mode);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CamrecorderSettings extends SettingsObject implements IObserver {
        public double targetFps;
        public KeyframeSettings keyframe;
        public boolean auto;

        @Override
        public void notify(Event event,
                           Object source,
                           Object... data) {
            if (isEnabled() && source != this) {
                if (event == Event.CAMRECORDER_FPS_CMD) {
                    targetFps = (Double) data[0];
                }
            }
        }

        @Override
        public CamrecorderSettings clone() {
            var c = (CamrecorderSettings) super.clone();
            c.keyframe = this.keyframe.clone();
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
            keyframe.setParent(s);
        }

        @Override
        protected void setupListeners() {
            EventManager.instance.subscribe(this, Event.CAMRECORDER_FPS_CMD);

            keyframe.setupListeners();
        }

        @Override
        public void dispose() {
            EventManager.instance.removeAllSubscriptions(this);
            keyframe.dispose();
        }

        @Override
        public void apply() {
            EventManager.publish(Event.CAMRECORDER_FPS_CMD, this, targetFps);
            keyframe.apply();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class KeyframeSettings extends SettingsObject {
            public KeyframesManager.PathType position;

            /**
             * Since we started using quaternion spherical linear interpolation, this property is not used.
             *
             * @deprecated Not used anymore.
             */
            @Deprecated
            public KeyframesManager.PathType orientation;


            @Override
            public KeyframeSettings clone() {
                return (KeyframeSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            public void setPosition(final String positionString) {
                position = getPathType(positionString);
            }

            public void setOrientation(final String orientationString) {
                orientation = getPathType(orientationString);
            }

            private KeyframesManager.PathType getPathType(String str) {
                // Keep compatibility with old path types.
                if (str.equalsIgnoreCase("SPLINE")) {
                    str = KeyframesManager.PathType.CATMULL_ROM_SPLINE.toString();
                }
                return KeyframesManager.PathType.valueOf(str.toUpperCase());
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostprocessSettings extends SettingsObject implements IObserver {
        public AntialiasSettings antialias;
        public BloomSettings bloom;
        public UnsharpMaskSettings unsharpMask;
        public ChromaticAberrationSettings chromaticAberration;
        public FilmGrainSettings filmGrain;
        public LensFlareSettings lensFlare;
        public LightGlowSettings lightGlow;
        public LevelsSettings levels;
        public ToneMappingSettings toneMapping;
        public SSRSettings ssr;
        public MotionBlurSettings motionBlur;
        public ReprojectionSettings reprojection;
        public UpscaleFilter upscaleFilter = UpscaleFilter.NEAREST;
        public GeometryWarpSettings warpingMesh;

        public void setAntialias(final String antialiasString) {
            antialias = AntialiasSettings.valueOf(antialiasString.toUpperCase());
        }

        public void setUpscaleFilter(final String upscaleFilterString) {
            upscaleFilter = UpscaleFilter.valueOf(upscaleFilterString.toUpperCase());
        }

        public AntialiasSettings getAntialias(int code) {
            return switch (code) {
                case -1 -> AntialiasSettings.FXAA;
                case -2 -> AntialiasSettings.NFAA;
                case 1 -> AntialiasSettings.SSAA;
                default -> AntialiasSettings.NONE;
            };
        }

        @Override
        public void notify(final Event event,
                           Object source,
                           final Object... data) {
            if (isEnabled() && source != this) {
                switch (event) {
                    case BLOOM_CMD -> bloom.intensity = (float) data[0];
                    case UNSHARP_MASK_CMD -> unsharpMask.factor = (float) data[0];
                    case CHROMATIC_ABERRATION_CMD -> chromaticAberration.amount = (float) data[0];
                    case FILM_GRAIN_CMD -> filmGrain.intensity = (float) data[0];
                    case LENS_FLARE_CMD -> {
                        float strength = (Float) data[0];
                        lensFlare.active = strength > 0;
                        lensFlare.strength = strength;
                    }
                    case LIGHT_GLOW_CMD -> lightGlow.active = (Boolean) data[0];
                    case SSR_CMD -> ssr.active = (Boolean) data[0];
                    case MOTION_BLUR_CMD -> motionBlur.active = (Boolean) data[0];
                    case REPROJECTION_CMD -> {
                        reprojection.active = (Boolean) data[0];
                        reprojection.mode = (ReprojectionMode) data[1];
                    }
                    case BRIGHTNESS_CMD ->
                            levels.brightness = MathUtils.clamp((float) data[0], Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS);
                    case CONTRAST_CMD ->
                            levels.contrast = MathUtils.clamp((float) data[0], Constants.MIN_CONTRAST, Constants.MAX_CONTRAST);
                    case HUE_CMD -> levels.hue = MathUtils.clamp((float) data[0], Constants.MIN_HUE, Constants.MAX_HUE);
                    case SATURATION_CMD ->
                            levels.saturation = MathUtils.clamp((float) data[0], Constants.MIN_SATURATION, Constants.MAX_SATURATION);
                    case GAMMA_CMD ->
                            levels.gamma = MathUtils.clamp((float) data[0], Constants.MIN_GAMMA, Constants.MAX_GAMMA);
                    case TONEMAPPING_TYPE_CMD -> {
                        ToneMapping newTM;
                        if (data[0] instanceof String) {
                            newTM = ToneMapping.valueOf(((String) data[0]).toUpperCase());
                        } else {
                            newTM = (ToneMapping) data[0];
                        }
                        toneMapping.type = newTM;
                    }
                    case EXPOSURE_CMD ->
                            toneMapping.exposure = MathUtilsDouble.clamp((float) data[0], Constants.MIN_EXPOSURE, Constants.MAX_EXPOSURE);
                    case UPSCALE_FILTER_CMD -> upscaleFilter = (UpscaleFilter) data[0];
                    default -> {
                    }
                }
            }
        }

        @Override
        public PostprocessSettings clone() {
            var c = (PostprocessSettings) super.clone();
            c.bloom = this.bloom.clone();
            c.unsharpMask = this.unsharpMask.clone();
            c.chromaticAberration = this.chromaticAberration.clone();
            c.filmGrain = this.filmGrain.clone();
            c.lensFlare = this.lensFlare.clone();
            c.lightGlow = this.lightGlow.clone();
            c.levels = this.levels.clone();
            c.toneMapping = this.toneMapping.clone();
            c.ssr = this.ssr.clone();
            c.motionBlur = this.motionBlur.clone();
            c.reprojection = this.reprojection.clone();
            c.warpingMesh = this.warpingMesh.clone();

            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
            bloom.setParent(s);
            unsharpMask.setParent(s);
            chromaticAberration.setParent(s);
            filmGrain.setParent(s);
            lensFlare.setParent(s);
            lightGlow.setParent(s);
            levels.setParent(s);
            toneMapping.setParent(s);
            ssr.setParent(s);
            motionBlur.setParent(s);
            reprojection.setParent(s);
            warpingMesh.setParent(s);

        }

        @Override
        protected void setupListeners() {
            EventManager.instance.subscribe(this, Event.BLOOM_CMD, Event.UNSHARP_MASK_CMD, Event.LENS_FLARE_CMD,
                    Event.MOTION_BLUR_CMD, Event.SSR_CMD, Event.LIGHT_GLOW_CMD, Event.REPROJECTION_CMD, Event.BRIGHTNESS_CMD,
                    Event.CONTRAST_CMD, Event.HUE_CMD, Event.SATURATION_CMD, Event.GAMMA_CMD, Event.TONEMAPPING_TYPE_CMD,
                    Event.EXPOSURE_CMD, Event.UPSCALE_FILTER_CMD, Event.CHROMATIC_ABERRATION_CMD, Event.FILM_GRAIN_CMD);

            bloom.setupListeners();
            unsharpMask.setupListeners();
            chromaticAberration.setupListeners();
            filmGrain.setupListeners();
            lensFlare.setupListeners();
            lightGlow.setupListeners();
            levels.setupListeners();
            toneMapping.setupListeners();
            ssr.setupListeners();
            motionBlur.setupListeners();
            reprojection.setupListeners();
            warpingMesh.setupListeners();
        }

        @Override
        public void dispose() {
            EventManager.instance.removeAllSubscriptions(this);
            bloom.dispose();
            unsharpMask.dispose();
            chromaticAberration.dispose();
            filmGrain.dispose();
            lensFlare.dispose();
            lightGlow.dispose();
            levels.dispose();
            toneMapping.dispose();
            ssr.dispose();
            motionBlur.dispose();
            reprojection.dispose();
            warpingMesh.dispose();
        }

        @Override
        public void apply() {
            // This needs to run in the main thread because it accesses the OpenGL context.
            GaiaSky.postRunnable(() -> {
                EventManager.publish(Event.BLOOM_CMD, this, bloom.intensity);
                EventManager.publish(Event.UNSHARP_MASK_CMD, this, unsharpMask.factor);
                EventManager.publish(Event.CHROMATIC_ABERRATION_CMD, this, chromaticAberration.amount);
                EventManager.publish(Event.FILM_GRAIN_CMD, this, filmGrain.intensity);
                EventManager.publish(Event.LENS_FLARE_CMD, this, lensFlare.strength);
                EventManager.publish(Event.LIGHT_GLOW_CMD, this, lightGlow.active);
                EventManager.publish(Event.SSR_CMD, this, ssr.active);
                EventManager.publish(Event.MOTION_BLUR_CMD, this, motionBlur.active);
                EventManager.publish(Event.REPROJECTION_CMD, this, reprojection.active, reprojection.mode);
                EventManager.publish(Event.BRIGHTNESS_CMD, this, levels.brightness);
                EventManager.publish(Event.CONTRAST_CMD, this, levels.contrast);
                EventManager.publish(Event.SATURATION_CMD, this, levels.saturation);
                EventManager.publish(Event.GAMMA_CMD, this, levels.gamma);
                EventManager.publish(Event.TONEMAPPING_TYPE_CMD, this, toneMapping.type);
                EventManager.publish(Event.EXPOSURE_CMD, this, toneMapping.exposure);
                EventManager.publish(Event.UPSCALE_FILTER_CMD, this, upscaleFilter);

                bloom.apply();
                unsharpMask.apply();
                chromaticAberration.apply();
                filmGrain.apply();
                lensFlare.apply();
                lightGlow.apply();
                levels.apply();
                toneMapping.apply();
                ssr.apply();
                motionBlur.apply();
                reprojection.apply();
                warpingMesh.apply();
            });
        }


        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class GeometryWarpSettings extends SettingsObject {
            public String pfmFile = null;

            @Override
            public GeometryWarpSettings clone() {
                return (GeometryWarpSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class BloomSettings extends SettingsObject {
            public float intensity;
            public float fboScale = 0.5f;

            @Override
            public BloomSettings clone() {
                return (BloomSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UnsharpMaskSettings extends SettingsObject {
            public float factor;

            @Override
            public UnsharpMaskSettings clone() {
                return (UnsharpMaskSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ChromaticAberrationSettings extends SettingsObject {
            public float amount = 0.01f;

            @Override
            public ChromaticAberrationSettings clone() {
                return (ChromaticAberrationSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FilmGrainSettings extends SettingsObject {
            public float intensity = 0.0f;

            @Override
            public FilmGrainSettings clone() {
                return (FilmGrainSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class LensFlareSettings extends SettingsObject {
            public boolean active;
            public LensFlareType type = LensFlareType.SIMPLE;
            /**
             * Strength of the real lens flare.
             **/
            public float strength = 1.0f;
            public int numGhosts = 8;
            public float haloWidth = 0.5f;
            public int blurPasses = 35;
            public float flareSaturation = 0.8f;
            public float bias = -0.98f;
            public String texLensColor = Constants.DATA_LOCATION_TOKEN + "tex/base/lenscolor.png";
            public String texLensDirt = Constants.DATA_LOCATION_TOKEN + "tex/base/lensdirt" + Constants.STAR_SUBSTITUTE + ".jpg";
            public String texLensStarburst = Constants.DATA_LOCATION_TOKEN + "tex/base/lensstarburst.jpg";
            public float fboScale = 0.2f;

            public void setType(String type) {
                this.type = LensFlareType.valueOf(type.toUpperCase());
            }

            @Override
            public LensFlareSettings clone() {
                return (LensFlareSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class LightGlowSettings extends SettingsObject {
            public boolean active;
            public int samples = 1;

            @Override
            public LightGlowSettings clone() {
                return (LightGlowSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class LevelsSettings extends SettingsObject {
            public float brightness;
            public float contrast;
            public float hue;
            public float saturation;
            public float gamma;

            @Override
            public LevelsSettings clone() {
                return (LevelsSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ToneMappingSettings extends SettingsObject {
            public ToneMapping type;
            public float exposure;

            public void setType(final String typeString) {
                type = ToneMapping.valueOf(typeString.toUpperCase());
            }

            @Override
            public ToneMappingSettings clone() {
                return (ToneMappingSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SSRSettings extends SettingsObject {
            public boolean active;

            @Override
            public SSRSettings clone() {
                return (SSRSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class MotionBlurSettings extends SettingsObject {
            public boolean active;

            @Override
            public MotionBlurSettings clone() {
                return (MotionBlurSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ReprojectionSettings extends SettingsObject {
            public boolean active;
            /*
             * The re-projection mode.
             */
            public ReprojectionMode mode;

            public void setReprojection(final String reprojectionString) {
                mode = ReprojectionMode.valueOf(reprojectionString.toUpperCase());
            }

            @Override
            public ReprojectionSettings clone() {
                return (ReprojectionSettings) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpacecraftSettings extends SettingsObject {
        public boolean velocityDirection;
        public boolean showAxes;

        @Override
        public SpacecraftSettings clone() {
            return (SpacecraftSettings) super.clone();
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
        }

        @Override
        protected void setupListeners() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public void apply() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RuntimeSettings extends SettingsObject implements IObserver {
        public boolean openXr = false;
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
        public boolean octreeLoadActive = false;

        // Max clock time, 5 Myr by default
        public long maxTimeMs = 5000000L * (long) Nature.Y_TO_MS;
        // Min clock time, -5 Myr by default
        public long minTimeMs = -maxTimeMs;

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

        public void toggleRecord(Boolean rec,
                                 Settings settings) {
            recordCamera = Objects.requireNonNullElseGet(rec, () -> !recordCamera);
        }

        @Override
        public void notify(Event event,
                           Object source,
                           Object... data) {

            if (isEnabled() && source != this) {
                switch (event) {
                    case INPUT_ENABLED_CMD -> inputEnabled = (boolean) data[0];
                    case DISPLAY_GUI_CMD -> displayGui = (boolean) data[0];
                    case DISPLAY_VR_GUI_CMD -> {
                        if (data.length > 1) {
                            displayVrGui = (Boolean) data[1];
                        } else {
                            displayVrGui = !displayVrGui;
                        }
                    }
                    case TOGGLE_UPDATEPAUSE -> {
                        updatePause = !updatePause;
                        EventManager.publish(Event.UPDATEPAUSE_CHANGED, this, updatePause);
                    }
                    case TIME_STATE_CMD -> toggleTimeOn((Boolean) data[0]);
                    case RECORD_CAMERA_CMD -> toggleRecord((Boolean) data[0], settings);
                    case GRAV_WAVE_START -> gravitationalWaves = true;
                    case GRAV_WAVE_STOP -> gravitationalWaves = false;
                    default -> {
                    }
                }
            }
        }

        @Override
        public RuntimeSettings clone() {
            return (RuntimeSettings) super.clone();
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
        }

        @Override
        protected void setupListeners() {
            EventManager.instance.subscribe(this, Event.INPUT_ENABLED_CMD, Event.DISPLAY_GUI_CMD, Event.TOGGLE_UPDATEPAUSE, Event.TIME_STATE_CMD, Event.RECORD_CAMERA_CMD,
                    Event.GRAV_WAVE_START, Event.GRAV_WAVE_STOP, Event.DISPLAY_VR_GUI_CMD);
        }

        @Override
        public void dispose() {
            EventManager.instance.removeAllSubscriptions(this);
        }

        @Override
        public void apply() {
            EventManager.publish(Event.INPUT_ENABLED_CMD, this, inputEnabled);
            EventManager.publish(Event.DISPLAY_GUI_CMD, this, displayGui, I18n.msg("notif.cleanmode"));
            EventManager.publish(Event.DISPLAY_VR_GUI_CMD, this, displayVrGui);
            EventManager.publish(Event.TIME_STATE_CMD, this, timeOn);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxySettings extends SettingsObject {

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

        @Override
        public ProxySettings clone() {
            var c = (ProxySettings) super.clone();
            c.http = this.http.clone();
            c.https = this.https.clone();
            c.socks = this.socks.clone();
            c.ftp = this.ftp.clone();
            return c;
        }

        @Override
        protected void setParentRecursive(SettingsObject s) {
            http.setParent(s);
            https.setParent(s);
            socks.setParent(s);
            ftp.setParent(s);
        }

        @Override
        protected void setupListeners() {
            http.setupListeners();
            https.setupListeners();
            socks.setupListeners();
            ftp.setupListeners();
        }

        @Override
        public void apply() {
            http.apply();
            https.apply();
            socks.apply();
            ftp.apply();
        }

        @Override
        public void dispose() {
            http.dispose();
            https.dispose();
            socks.dispose();
            ftp.dispose();
        }

        public static class ProxyBean extends SettingsObject {
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


            @Override
            public ProxyBean clone() {
                return (ProxyBean) super.clone();
            }

            @Override
            protected void setParentRecursive(SettingsObject s) {
            }

            @Override
            protected void setupListeners() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void apply() {
            }
        }
    }

    /**
     * Sets the configuration version. Adapts to the new min.maj.rev-seq format automagically.
     *
     * @param configVersion The configuration version.
     */
    public void setConfigVersion(int configVersion) {
        this.configVersion = VersionChecker.correctVersionNumber(configVersion);
    }
}
