/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import gaiasky.desktop.GaiaSkyDesktop;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Settings.DistanceUnits;
import gaiasky.util.Settings.ElevationType;
import gaiasky.util.Settings.PostprocessSettings.ChromaticAberrationSettings;
import gaiasky.util.Settings.ProxySettings.ProxyBean;
import gaiasky.util.Settings.SceneSettings.ParticleSettings;
import gaiasky.util.Settings.SceneSettings.RendererSettings.EclipseSettings;
import gaiasky.util.Settings.SceneSettings.RendererSettings.LineSettings;
import gaiasky.util.Settings.SceneSettings.RendererSettings.VirtualTextureSettings;
import gaiasky.util.Settings.VersionSettings;
import gaiasky.util.math.MathUtilsDouble;

import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
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
 * Contains utilities to initialize and manage the Gaia Sky {@link Settings} objects.
 */
public class SettingsManager {
    private static final Logger.Log logger = Logger.getLogger(SettingsManager.class);

    public static SettingsManager instance;
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

    public SettingsManager(InputStream fis,
                           InputStream vis) {
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

    public static void initialize(boolean vr) throws Exception {
        SettingsManager.instance = new SettingsManager(vr);
        instance.initSettings();
    }

    public static void initialize(FileInputStream fis,
                                   FileInputStream vis) {
        SettingsManager.instance = new SettingsManager(fis, vis);
        instance.initSettings();
    }

    private static void setProxySettings(ProxyBean proxy,
                                         String protocol) {
        setProxySettings(proxy, protocol, protocol);
    }

    private static void setProxySettings(ProxyBean proxy,
                                         String protocol,
                                         String nonProxyHostsProtocol) {
        if (proxy.host != null)
            System.setProperty(protocol + ".proxyHost", proxy.host);
        if (proxy.port != null)
            System.setProperty(protocol + ".proxyPort", Integer.toString(proxy.port));
        if (proxy.nonProxyHosts != null)
            System.setProperty(nonProxyHostsProtocol + ".nonProxyHosts", proxy.password);

        if (proxy.username != null)
            System.setProperty(protocol + ".proxyUser", proxy.username);
        if (proxy.password != null)
            System.setProperty(protocol + ".proxyPassword", proxy.password);
    }

    private static void setSocksProxySettings(ProxyBean proxy,
                                              String prefix) {
        if (proxy.version != null)
            System.setProperty(prefix + "ProxyVersion", Integer.toString(proxy.version));
        if (proxy.host != null)
            System.setProperty(prefix + "ProxyHost", proxy.host);
        if (proxy.port != null)
            System.setProperty(prefix + "ProxyPort", Integer.toString(proxy.port));
        if (proxy.username != null)
            System.setProperty("java.net.socks.username", proxy.username);
        if (proxy.password != null)
            System.setProperty("java.net.socks.password", proxy.password);
    }

    public static void initializeProxyAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() == RequestorType.PROXY) {
                    String protocol = getRequestingProtocol().toLowerCase();
                    String host = System.getProperty(protocol + ".proxyHost", "");
                    String port = System.getProperty(protocol + ".proxyPort", "80");
                    String user = System.getProperty(protocol + ".proxyUser", "");
                    String password = System.getProperty(protocol + ".proxyPassword", "");
                    if (getRequestingHost().equalsIgnoreCase(host)) {
                        if (Integer.parseInt(port) == getRequestingPort()) {
                            return new PasswordAuthentication(user, password.toCharArray());
                        }
                    }
                }
                return null;
            }
        });
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }

    public static void persistSettings(final File settingsFile) {
        if (instance != null)
            instance.persist(settingsFile);
    }

    public static String getConfigFileName(boolean vr) {
        if (vr)
            return "config.vr.yaml";
        else
            return "config.yaml";
    }

    public static boolean setSettingsInstance(Settings settings) {
        return Settings.setSettingsReference(settings);
    }

    public void setSettingsReference(Settings settings) {
        this.settings = settings;
    }


    private void initializeMapper() {
        YAMLFactory yaml = new YAMLFactory();
        yaml.disable(Feature.WRITE_DOC_START_MARKER).enable(Feature.MINIMIZE_QUOTES).enable(Feature.INDENT_ARRAYS);
        mapper = new ObjectMapper(yaml);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.findAndRegisterModules();
    }

    public void initSettings() {

        // Initialize version.
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

        // Initialize runtime.
        settings.runtime = new Settings.RuntimeSettings();

        settings.version = versionSettings;
        Settings.settings = settings;

        String arch = System.getProperty("sun.arch.data.model");

        // Data location.
        if (settings.data.location == null || settings.data.location.isBlank()) {
            settings.data.location = SysUtils.getLocalDataDir().toAbsolutePath().toString().replaceAll("\\\\", "/");
        }

        // UI scale mapping.
        settings.program.ui.scale = MathUtilsDouble.lint(settings.program.ui.scale, Constants.UI_SCALE_MIN, Constants.UI_SCALE_MAX, Constants.UI_SCALE_INTERNAL_MIN,
                Constants.UI_SCALE_INTERNAL_MAX);

        // Default distance units.
        if (settings.program.ui.distanceUnits == null) {
            settings.program.ui.distanceUnits = DistanceUnits.PC;
        }

        // Eclipses.
        if (settings.scene.renderer.eclipses == null) {
            settings.scene.renderer.eclipses = new EclipseSettings();
            settings.scene.renderer.eclipses.active = true;
            settings.scene.renderer.eclipses.outlines = false;
        }

        // Back buffer resolution.
        settings.graphics.backBufferResolution = new int[2];
        settings.graphics.backBufferResolution[0] = (int) (settings.graphics.resolution[0] * settings.graphics.backBufferScale);
        settings.graphics.backBufferResolution[1] = (int) (settings.graphics.resolution[1] * settings.graphics.backBufferScale);

        // Disable tessellation on macOS.
        if (SysUtils.isMac() && settings.scene.renderer.elevation.type.isTessellation()) {
            settings.scene.renderer.elevation.type = ElevationType.NONE;
        }

        // Minimap size.
        settings.program.minimap.size = MathUtilsDouble.clamp(settings.program.minimap.size, Constants.MIN_MINIMAP_SIZE, Constants.MAX_MINIMAP_SIZE);

        // Particle groups.
        if (settings.scene.particleGroups == null) {
            settings.scene.particleGroups = new ParticleSettings();
        }

        // Limit draw distance in 32-bit JVM.
        if (arch.equals("32")) {
            float delta = Math.abs(settings.scene.octree.threshold[1] - settings.scene.octree.threshold[0]);
            settings.scene.octree.threshold[0] = (float) Math.toRadians(80);
            settings.scene.octree.threshold[1] = settings.scene.octree.threshold[1] + delta;
        }
        // Limit number of stars in 32-bit JVM.
        if (arch.equals("32")) {
            settings.scene.octree.maxStars = 1500000;
        }

        // Frame output location.
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

        // Screenshots location.
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

        // Add chromatic aberration if not there.
        if (settings.postprocess.chromaticAberration == null) {
            settings.postprocess.chromaticAberration = new ChromaticAberrationSettings();
            if (settings.runtime.openXr) {
                settings.postprocess.chromaticAberration.amount = 0;
            }
        }

        // Add film grain if not there.
        if (settings.postprocess.filmGrain == null) {
            settings.postprocess.filmGrain = new Settings.PostprocessSettings.FilmGrainSettings();
        }

        // Set up proxy if needed.
        if (settings.proxy != null) {
            if (settings.proxy.useSystemProxies != null) {
                System.setProperty("java.net.useSystemProxies", Boolean.toString(settings.proxy.useSystemProxies));
            }
            if (settings.proxy.http != null) {
                setProxySettings(settings.proxy.http, "http");
            }
            if (settings.proxy.https != null) {
                setProxySettings(settings.proxy.https, "https", "http");
            }
            if (settings.proxy.socks != null) {
                setSocksProxySettings(settings.proxy.socks, "socks");
            }
            if (settings.proxy.ftp != null) {
                setProxySettings(settings.proxy.ftp, "ftp");
            }
        }
        // Set up proxy authenticator.
        initializeProxyAuthenticator();

        // Virtual texture settings.
        if (settings.scene.renderer.virtualTextures == null) {
            settings.scene.renderer.virtualTextures = new VirtualTextureSettings();
            settings.scene.renderer.virtualTextures.cacheSize = 8;
            settings.scene.renderer.virtualTextures.detectionBufferFactor = 8.0;
        }

        // Update visibility with new elements if needed.
        if (!settings.scene.visibility.containsKey("Keyframes")) {
            settings.scene.visibility.put("Keyframes", true);
        }

        // Grid style.
        if (settings.program.recursiveGrid.style == null) {
            settings.program.recursiveGrid.style = Settings.GridStyle.CIRCULAR;
        }

        // Line settings.
        if (settings.scene.renderer.line == null) {
            var ls = new LineSettings();
            ls.mode = Settings.LineMode.POLYLINE_QUADSTRIP;
            ls.width = 1;
            ls.glWidthBias = 0;
            settings.scene.renderer.line = ls;
        }

        settings.initialize();
    }

    private void persist(final File settingsFile) {
        try {
            boolean backup = settings.program.safeMode;
            if (settings.program.safeModeFlag) {
                settings.program.safeMode = false;
            }

            FileOutputStream fos = new FileOutputStream(settingsFile);
            SequenceWriter sw = mapper.writerWithDefaultPrettyPrinter().writeValues(fos);
            sw.write(settings);

            if (settings.program.safeModeFlag) {
                settings.program.safeMode = backup;
            }
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
}
