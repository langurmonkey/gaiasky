package gaiasky.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import gaiasky.desktop.GaiaSkyDesktop;
import gaiasky.desktop.util.SysUtils;
import gaiasky.util.Settings.VersionSettings;
import gaiasky.util.math.MathUtilsd;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Properties;

/**
 * Initializes and stores the YAML configuration file for Gaia Sky.
 */
public class SettingsManager {
    private static final Logger.Log logger = Logger.getLogger(SettingsManager.class);

    public static SettingsManager instance;

    public static void initialize(boolean vr) throws Exception {
        SettingsManager.instance = new SettingsManager(vr);
        instance.initSettings();
    }
    public static void initialize(FileInputStream fis, FileInputStream vis) throws Exception {
        SettingsManager.instance = new SettingsManager(fis, vis);
        instance.initSettings();
    }

    private Settings settings;
    private Properties vp;
    private ObjectMapper mapper;

    public SettingsManager(boolean vr) {
        super();
        try {
            String propsFileProperty = System.getProperty("properties.file");
            if (propsFileProperty == null || propsFileProperty.isEmpty()) {
                propsFileProperty = initConfigFile(vr);
            }

            File confFile = new File(propsFileProperty);
            InputStream fis = new FileInputStream(confFile);

            // This should work for the normal execution
            InputStream vis = GaiaSkyDesktop.class.getResourceAsStream("/version");
            if (vis == null) {
                // In case of running in 'developer' mode
                vis = new FileInputStream(Settings.ASSETS_LOC + File.separator + "dummyversion");
            }
            vp = new Properties();
            vp.load(vis);

            initializeMapper();
            settings = mapper.readValue(fis, Settings.class);

        } catch (Exception e) {
            logger.error(e);
        }
    }

    public SettingsManager(InputStream fis, InputStream vis) {
        super();
        try {
            vp = new Properties();
            vp.load(vis);

            initializeMapper();
            settings = mapper.readValue(fis, Settings.class);

        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void initializeMapper() {
        YAMLFactory yaml = new YAMLFactory();
        yaml.disable(Feature.WRITE_DOC_START_MARKER).enable(Feature.MINIMIZE_QUOTES).enable(Feature.INDENT_ARRAYS);
        mapper = new ObjectMapper(yaml);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.findAndRegisterModules();
    }

    public void initSettings() {

        // Initialize version
        VersionSettings versionSettings = new VersionSettings();
        String versionStr = vp.getProperty("version");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
        Instant buildTime = null;
        try {
            buildTime = LocalDateTime.parse(vp.getProperty("buildtime"), formatter).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH);
            try {
                buildTime = LocalDateTime.parse(vp.getProperty("buildtime"), formatter).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e1) {
                logger.error(e1);
            }
        }
        versionSettings.initialize(versionStr, buildTime, vp.getProperty("builder"), vp.getProperty("system"), vp.getProperty("build"));

        settings.version = versionSettings;
        Settings.settings = settings;

        String arch = System.getProperty("sun.arch.data.model");

        // Data location
        if (settings.data.location == null || settings.data.location.isBlank()) {
            settings.data.location = SysUtils.getLocalDataDir().toAbsolutePath().toString().replaceAll("\\\\", "/");
        }

        // UI scale mapping
        settings.program.ui.scale = MathUtilsd.lint(settings.program.ui.scale, Constants.UI_SCALE_MIN, Constants.UI_SCALE_MAX, Constants.UI_SCALE_INTERNAL_MIN, Constants.UI_SCALE_INTERNAL_MAX);

        // Scripts location
        String scl = settings.program.scriptsLocation;
        scl = (scl == null || scl.isEmpty()) ? System.getProperty("user.dir") + File.separatorChar + "scripts" : scl;
        scl = scl.replaceAll("\\\\", "/");
        settings.program.scriptsLocation = scl;

        // Back buffer resolution
        settings.graphics.backBufferResolution = new int[2];
        settings.graphics.backBufferResolution[0] = (int) (settings.graphics.resolution[0] * settings.graphics.backBufferScale);
        settings.graphics.backBufferResolution[1] = (int) (settings.graphics.resolution[1] * settings.graphics.backBufferScale);

        // Minimap size
        settings.program.minimap.size = MathUtilsd.clamp(settings.program.minimap.size, Constants.MIN_MINIMAP_SIZE, Constants.MAX_MINIMAP_SIZE);

        // Limit draw distance in 32-bit JVM
        if (arch.equals("32")) {
            double delta = Math.abs(settings.scene.octree.threshold[1] - settings.scene.octree.threshold[0]);
            settings.scene.octree.threshold[0] = (float) Math.toRadians(80);
            settings.scene.octree.threshold[1] = settings.scene.octree.threshold[1] + delta;
        }
        // Limit number of stars in 32-bit JVM
        if (arch.equals("32")) {
            settings.scene.octree.maxStars = 1500000;
        }

        // Frame output location
        try {
            String fl = settings.frame.location;
            if (fl == null || fl.isBlank()) {
                Path framesDir = SysUtils.getDefaultFramesDir();
                Files.createDirectories(framesDir);
                fl = framesDir.toAbsolutePath().toString();
            }
            settings.frame.location = fl.replaceAll("\\\\", "/");
        } catch (IOException e) {
            logger.error("Error initializing frame output location", e);
        }

        // Screenshots location
        try {
            String sl = settings.screenshot.location;
            if (sl == null || sl.isBlank()) {
                Path screenshotsDir = SysUtils.getDefaultScreenshotsDir();
                Files.createDirectories(screenshotsDir);
                sl = screenshotsDir.toAbsolutePath().toString();
            }
            settings.screenshot.location = sl.replaceAll("\\\\", "/");
        } catch (IOException e) {
            logger.error("Error initializing frame output location", e);
        }

        settings.initialized = true;
    }

    public static void persistSettings(final File settingsFile) {
        if (instance != null)
            instance.persist(settingsFile);
    }

    private void persist(final File settingsFile) {
        try {
            FileOutputStream fos = new FileOutputStream(settingsFile);
            SequenceWriter sw = mapper.writerWithDefaultPrettyPrinter().writeValues(fos);
            sw.write(settings);
            logger.info("Settings saved to " + settingsFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private String initConfigFile(boolean vr) throws IOException {
        // Use user folder
        Path userFolder = SysUtils.getConfigDir();
        Path userFolderConfFile = userFolder.resolve(getConfigFileName(vr));

        if (!Files.exists(userFolderConfFile)) {
            // Copy file
            GlobalResources.copyFile(Path.of("conf", getConfigFileName(vr)), userFolderConfFile, false);
        }
        String props = userFolderConfFile.toAbsolutePath().toString();
        System.setProperty("properties.file", props);
        return props;
    }

    public static String getConfigFileName(boolean vr) {
        if (vr)
            return "config.vr.yaml";
        else
            return "config.yaml";
    }

}
