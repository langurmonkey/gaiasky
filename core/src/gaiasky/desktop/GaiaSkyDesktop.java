/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.GLEmulation;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import gaiasky.ErrorDialog;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.ConsoleLogger;
import gaiasky.gui.KeyBindings;
import gaiasky.render.ScreenModeCmd;
import gaiasky.rest.RESTServer;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Logger.LoggerLevel;
import gaiasky.util.Settings.ElevationType;
import gaiasky.util.camera.rec.CamRecorder;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathManager;
import org.yaml.snakeyaml.Yaml;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Main class for the Gaia Sky desktop and VR launcher.
 */
public class GaiaSkyDesktop implements IObserver {
    private static final Log logger = Logger.getLogger(GaiaSkyDesktop.class);

    /*
     * Source version to compare to config file and datasets.
     * This is usually tag where each number is allocated 2 digits.
     * Version = major.minor.rev -> 1.2.5 major=1; minor=2; rev=5
     * Version = major * 10000 + minor * 100 + rev
     * So 1.2.5 -> 10205
     *    2.1.7 -> 20107
     *
     * Leading zeroes are omitted to avoid octal literal interpretation.
     */
    public static final int SOURCE_VERSION = 30300;
    private static boolean REST_ENABLED = false;
    private static boolean JAVA_VERSION_FLAG = false;

    private static final String REQUIRED_JAVA_VERSION = "15";

    private static CLIArgs cliArgs;

    private static final int DEFAULT_OPENGL_MAJOR = 4;
    private static final int DEFAULT_OPENGL_MINOR = 1;
    private static final String DEFAULT_OPENGL = DEFAULT_OPENGL_MAJOR + "." + DEFAULT_OPENGL_MINOR;

    private static final int MIN_OPENGL_MAJOR = 3;
    private static final int MIN_OPENGL_MINOR = 2;
    private static final String MIN_OPENGL = MIN_OPENGL_MAJOR + "." + MIN_OPENGL_MINOR;
    private static final int MIN_GLSL_MAJOR = 3;
    private static final int MIN_GLSL_MINOR = 3;
    private static final String MIN_GLSL = MIN_GLSL_MAJOR + "." + MIN_GLSL_MINOR;

    /**
     * Program CLI arguments.
     */
    private static class CLIArgs {
        @Parameter(names = { "-h", "--help" }, description = "Show program options and usage information.", help = true, order = 0) private boolean help = false;

        @Parameter(names = { "-v", "--version" }, description = "List Gaia Sky version and relevant information.", order = 1) private boolean version = false;

        @Parameter(names = { "-i", "--asciiart" }, description = "Add nice ascii art to --version information.", order = 1) private boolean asciiart = false;

        @Parameter(names = { "-s", "--skip-welcome" }, description = "Skip the welcome screen if possible (base-data package must be present).", order = 2) private boolean skipWelcome = false;

        @Parameter(names = { "-p", "--properties" }, description = "Specify the location of the properties file.", order = 4) private String propertiesFile = null;

        @Parameter(names = { "-a", "--assets" }, description = "Specify the location of the assets folder. If not present, the default assets location (in the installation folder) is used.", order = 5) private String assetsLocation = null;

        @Parameter(names = { "-vr", "--openvr" }, description = "Launch in Virtual Reality mode. Gaia Sky will attempt to create a VR context through OpenVR.", order = 6) private boolean vr = false;

        @Parameter(names = { "-e", "--externalview" }, description = "Create a window with a view of the scene and no UI.", order = 7) private boolean externalView = false;

        @Parameter(names = { "-n", "--noscript" }, description = "Do not start the scripting server. Useful to run more than one Gaia Sky instance at once in the same machine.", order = 8) private boolean noScriptingServer = false;

        @Parameter(names = { "-d", "--debug" }, description = "Launch in debug mode. Prints out debug information from Gaia Sky to the logs.", order = 9) private boolean debug = false;

        @Parameter(names = { "-g", "--gpudebug" }, description = "Activate OpenGL debug mode. Prints out debug information from OpenGL to the standard output.", order = 10) private boolean debugGpu = false;

        @Parameter(names = { "-l", "--headless" }, description = "Use headless (windowless) mode, for servers.", order = 11) private boolean headless = false;

        @Parameter(names = { "--safemode" }, description = "Activate safe graphics mode. This forces the creation of an OpenGL 3.2 context, and disables float buffers and tessellation.", order = 12) private boolean safeMode = false;
    }

    /**
     * Formats the regular usage so that it removes the left padding characters.
     * This is necessary so that help2man recognizes the OPTIONS block.
     *
     * @param jc The JCommander object.
     */
    private static void printUsage(JCommander jc) {
        jc.usage();
    }

    /**
     * UTF-8 output stream printer.
     **/
    private static PrintStream out;

    /**
     * Main method.
     *
     * @param args CLI arguments (see {@link CLIArgs}).
     */
    public static void main(final String[] args) {
        Thread.currentThread().setName("gaiasky-main-thread");
        out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        cliArgs = new CLIArgs();
        JCommander jc = JCommander.newBuilder().addObject(cliArgs).build();
        jc.setProgramName("gaiasky");
        try {
            jc.parse(args);

            if (cliArgs.help) {
                printUsage(jc);
                return;
            }
        } catch (Exception e) {
            out.print("gaiasky: bad program arguments\n\n");
            printUsage(jc);
            return;
        }

        try {
            // Check java version.
            javaVersionCheck();

            // Experimental features.
            experimentalCheck();

            // Set properties file from arguments to VM params if needed.
            if (cliArgs.propertiesFile != null && !cliArgs.propertiesFile.isEmpty()) {
                System.setProperty("properties.file", cliArgs.propertiesFile);
            }

            // Set assets location to VM params if needed.
            if (cliArgs.assetsLocation != null && !cliArgs.assetsLocation.isEmpty()) {
                System.setProperty("assets.location", cliArgs.assetsLocation);
            }

            if (cliArgs.vr) {
                Settings.APPLICATION_NAME += " VR";
            }

            GaiaSkyDesktop gaiaSkyDesktop = new GaiaSkyDesktop();

            Gdx.files = new Lwjgl3Files();

            // Init Gaia Sky directories.
            SysUtils.mkdirs();

            // Init properties file.
            String props = System.getProperty("properties.file");
            if (props == null || props.isEmpty()) {
                initConfigFile(cliArgs.vr);
            }

            // Init global configuration.
            SettingsManager.initialize(cliArgs.vr);

            // Initialize i18n (only for global config logging).
            I18n.initialize(Gdx.files.internal("i18n/gsbundle"), Gdx.files.internal("i18n/objects"));

            // Safe mode active if specified in CLI arg, or in config. Force safe mode for M1 macOS.
            if (SysUtils.isM1Mac() || (cliArgs.safeMode && !Settings.settings.program.safeMode)) {
                Settings.settings.program.safeMode = true;
                Settings.settings.program.safeModeFlag = true;
            }

            // Reinitialize with user-defined locale.
            I18n.initialize(Gdx.files.absolute(Settings.ASSETS_LOC + File.separator + "i18n/gsbundle"), Gdx.files.absolute(Settings.ASSETS_LOC + File.separator + "i18n/objects"));

            // -v or --version
            if (cliArgs.version) {
                out.println(Settings.getShortApplicationName());
                if (cliArgs.asciiart) {
                    BufferedReader ascii = new BufferedReader(new InputStreamReader(Gdx.files.internal("icon/gsascii.txt").read()));
                    out.println();
                    String line;
                    while ((line = ascii.readLine()) != null) {
                        out.println(line);
                    }
                }
                out.println();
                out.println(I18n.msg("gui.help.license"));
                out.println(I18n.msg("gui.help.writtenby", Settings.AUTHOR_NAME, Settings.AUTHOR_EMAIL));
                out.println();
                out.println(I18n.msg("gui.help.homepage") + "\t<" + Settings.WEBPAGE + ">");
                out.println(I18n.msg("gui.help.docs") + "\t\t<" + Settings.DOCUMENTATION + ">");
                out.println();
                out.println(I18n.msg("gui.help.javaversion") + " " + System.getProperty("java.vm.version"));
                out.println();
                out.println("ZAH/DLR/BWT/DPAC");
                return;
            }

            // Set log level
            Logger.level = cliArgs.debug ? LoggerLevel.DEBUG : LoggerLevel.INFO;
            // Create logger
            ConsoleLogger consoleLogger = new ConsoleLogger();

            // REST API server.
            REST_ENABLED = Settings.settings.program.net.restPort >= 0 && checkRestDependenciesInClasspath();
            if (REST_ENABLED) {
                RESTServer.initialize(Settings.settings.program.net.restPort);
            }

            // Slave manager.
            SlaveManager.initialize();

            // Full screen command.
            ScreenModeCmd.initialize();

            // Init cam recorder.
            CamRecorder.initialize();

            // Init music manager.
            MusicManager.initialize(Paths.get(Settings.ASSETS_LOC, "music"), SysUtils.getDefaultMusicDir());

            // Key mappings.
            KeyBindings.initialize();

            // Math.
            MathManager.initialize();

            consoleLogger.dispose();

            gaiaSkyDesktop.init();

            // Write session log.
            CrashReporter.writeLastSessionLog(logger);
        } catch (Exception e) {
            CrashReporter.reportCrash(e, logger);
        }
    }

    public GaiaSkyDesktop() {
        super();
        EventManager.instance.subscribe(this, Event.SCENE_LOADED, Event.DISPOSE);
    }

    private void init() {
        launchMainApp();
    }

    private GaiaSky gs;

    public void launchMainApp() {
        ConsoleLogger consoleLogger = new ConsoleLogger();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        Settings s = Settings.settings;
        cfg.setTitle(Settings.APPLICATION_NAME);
        if (!cliArgs.vr) {
            if (s.graphics.fullScreen.active) {
                int[] fullScreenResolution = s.graphics.fullScreen.resolution;
                // Full screen mode.
                DisplayMode[] modes = Lwjgl3ApplicationConfiguration.getDisplayModes();
                if (cliArgs.debug) {
                    logger.debug("Full screen resolution in config file: " + fullScreenResolution[0] + "x" + fullScreenResolution[1]);
                    logger.debug("Supported full screen modes:");
                    int modeIndex = 1;
                    for (DisplayMode displayMode : modes) {
                        logger.debug("  " + modeIndex++ + ". " + displayMode.toString());
                    }
                }
                DisplayMode myMode = null;

                // Find out modes with the same resolution.
                Array<DisplayMode> fittingModes = new Array<>();
                for (DisplayMode mode : modes) {
                    if (mode.height == fullScreenResolution[1] && mode.width == fullScreenResolution[0]) {
                        fittingModes.add(mode);
                    }
                }
                if(fittingModes.size == 1) {
                    // Only one available, use it.
                    myMode = fittingModes.get(0);
                } else if (fittingModes.size > 1) {
                    // Check if bit depth and refresh rate are set.
                    for(DisplayMode fittingMode : fittingModes) {
                        if(myMode == null) {
                            myMode = fittingMode;
                        } else {
                            if(s.graphics.fullScreen.bitDepth > 0
                                    && fittingMode.bitsPerPixel == s.graphics.fullScreen.bitDepth
                                    && s.graphics.fullScreen.refreshRate > 0
                                    && fittingMode.refreshRate == s.graphics.fullScreen.refreshRate) {
                                myMode = fittingMode;
                                break;
                            } else {
                                if(fittingMode.refreshRate > myMode.refreshRate) {
                                    myMode = fittingMode;
                                }
                            }
                        }
                    }
                }

                logger.debug("Using full screen mode: " + myMode);

                if (myMode == null) {
                    // Fall back to windowed.
                    logger.warn(I18n.msg("error.fullscreen.notfound", fullScreenResolution[0], fullScreenResolution[1]));
                    cfg.setWindowedMode(s.graphics.getScreenWidth(), s.graphics.getScreenHeight());
                    cfg.setResizable(s.graphics.resizable);
                } else {
                    cfg.setFullscreenMode(myMode);
                    s.graphics.fullScreen.resolution[0] = myMode.width;
                    s.graphics.fullScreen.resolution[1] = myMode.height;
                    s.graphics.fullScreen.bitDepth = myMode.bitsPerPixel;
                    s.graphics.fullScreen.refreshRate = myMode.refreshRate;
                }
            } else {
                // Windowed mode.
                configureWindowSize(cfg);
                cfg.setResizable(s.graphics.resizable);
            }
            cfg.useVsync(s.graphics.vsync);
        } else {
            // Note that we disable VSync! The VRContext manages vsync with respect to the HMD.
            cfg.useVsync(false);
            // Always windowed, actual render sent to headset.
            configureWindowSize(cfg);
            cfg.setResizable(true);
        }
        if (cliArgs.vr) {
            cfg.setWindowIcon(FileType.Internal, "icon/gsvr_icon.png");
        } else {
            cfg.setWindowIcon(FileType.Internal, "icon/gs_icon.png");
        }
        cfg.setOpenGLEmulation(GLEmulation.GL30, DEFAULT_OPENGL_MAJOR, DEFAULT_OPENGL_MINOR);
        // Disable logical DPI modes (macOS, Windows).
        cfg.setHdpiMode(HdpiMode.Pixels);
        // Headless mode.
        cfg.setInitialVisible(!cliArgs.headless);
        // OpenGL debug.
        if (cliArgs.debugGpu) {
            cfg.enableGLDebugOutput(true, System.out);
        }
        // Color, Depth, stencil buffers, MSAA.
        cfg.setBackBufferConfig(8, 8, 8, 8, 24, 8, 0);

        // Launch app.
        try {
            if (s.program.safeMode) {
                setSafeMode(cfg);
            }
            consoleLogger.unsubscribe();

            runGaiaSky(cfg);
        } catch (GdxRuntimeException e) {
            checkLogger(consoleLogger);
            logger.error(e);
            if (gs != null) {
                gs.setCrashed(true);
                try {
                    gs.dispose();
                } catch (Exception e1) {
                    logger.error(I18n.msg("error.dispose"), e1);
                }
            }
            if (!JAVA_VERSION_FLAG) {
                if (gs != null && !gs.windowCreated) {
                    // Probably, OpenGL 4.x is not supported and window creation failed
                    logger.error(I18n.msg("error.windowcreation", DEFAULT_OPENGL, MIN_OPENGL));
                    setSafeMode(cfg);
                    consoleLogger.unsubscribe();

                    try {
                        runGaiaSky(cfg);
                    } catch (GdxRuntimeException e1) {
                        logger.error(I18n.msg("error.opengl", MIN_OPENGL, MIN_GLSL));
                        showDialogOGL(e, I18n.msg("dialog.opengl.title"), I18n.msg("dialog.opengl.message", MIN_OPENGL, MIN_GLSL));
                    }
                } else {
                    logger.error(I18n.msg("error.crash", Settings.REPO_ISSUES, SysUtils.getCrashReportsDir()));
                    showDialogOGL(e, I18n.msg("error.crash.title"), I18n.msg("error.crash.exception.2", Settings.REPO_ISSUES, SysUtils.getCrashReportsDir()));
                }
            } else {
                logger.error(I18n.msg("error.java", REQUIRED_JAVA_VERSION));
                showDialogOGL(e, I18n.msg("dialog.java.title"), I18n.msg("dialog.java.message", REQUIRED_JAVA_VERSION));
            }
        } catch (Exception e) {
            logger.error(e);
            showDialogOGL(e, I18n.msg("error.crash.title"), I18n.msg("error.crash.exception.2", SysUtils.getCrashReportsDir()));
        }
    }

    private void configureWindowSize(final Lwjgl3ApplicationConfiguration cfg) {
        configureWindowSize(cfg, 1f, 1f);
    }

    private void configureWindowSize(final Lwjgl3ApplicationConfiguration cfg, float widthFactor, float heightFactor) {
        int w = Settings.settings.graphics.getScreenWidth();
        int h = Settings.settings.graphics.getScreenHeight();
        if (!SysUtils.isMac()) {
            // Graphics device method.
            if (w <= 0 || h <= 0) {
                try {
                    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                    java.awt.DisplayMode dm = gd.getDisplayMode();
                    w = (int) Math.max(1600, (dm.getWidth() * 0.85f));
                    h = (int) Math.max(900, (dm.getHeight() * 0.85f));
                    Settings.settings.graphics.resolution[0] = w;
                    Settings.settings.graphics.resolution[1] = h;
                } catch (HeadlessException he) {
                    logger.error(I18n.msg("error.screensize.gd"));
                    logger.debug(he);
                }
            }
            // Toolkit method.
            if (w <= 0 || h <= 0) {
                try {
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    w = (int) Math.max(1600, (screenSize.getWidth() * 0.85f));
                    h = (int) Math.max(900, (screenSize.getHeight() * 0.85f));
                    Settings.settings.graphics.resolution[0] = w;
                    Settings.settings.graphics.resolution[1] = h;
                } catch (Exception e) {
                    // Default.
                    w = 1600;
                    h = 900;
                    Settings.settings.graphics.resolution[0] = w;
                    Settings.settings.graphics.resolution[1] = h;
                    logger.error(I18n.msg("error.screensize.toolkit", w, h));
                    logger.debug(e);
                }
            }
        } else {
            // macOS is retarded and only likes headless mode, using default.
            w = 1600;
            h = 900;
            Settings.settings.graphics.resolution[0] = w;
            Settings.settings.graphics.resolution[1] = h;
        }

        // Apply factors.
        Settings.settings.graphics.resolution[0] = (int) (Settings.settings.graphics.resolution[0] * widthFactor);
        Settings.settings.graphics.resolution[1] = (int) (Settings.settings.graphics.resolution[1] * heightFactor);
        w = (int) (w * widthFactor);
        h = (int) (h * heightFactor);

        // Set to config.
        if (cfg != null)
            cfg.setWindowedMode(w, h);
    }

    private void runGaiaSky(final Lwjgl3ApplicationConfiguration cfg) {
        gs = new GaiaSky(cliArgs.skipWelcome, cliArgs.vr, cliArgs.externalView, cliArgs.headless, cliArgs.noScriptingServer, cliArgs.debug);
        new Lwjgl3Application(gs, cfg);
    }

    private void setSafeMode(final Lwjgl3ApplicationConfiguration cfg) {
        logger.info(I18n.msg("startup.safe.enable", MIN_OPENGL, MIN_GLSL));
        Settings.settings.scene.renderer.elevation.type = ElevationType.NONE;
        Settings.settings.program.safeMode = true;
        cfg.setOpenGLEmulation(GLEmulation.GL30, MIN_OPENGL_MAJOR, MIN_OPENGL_MINOR);
    }

    private void showDialogOGL(final Exception ex, final String title, final String message) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setHdpiMode(HdpiMode.Pixels);
        cfg.useVsync(true);
        configureWindowSize(cfg, 0.6f, 0.85f);
        cfg.setResizable(false);
        cfg.setTitle(title);

        new Lwjgl3Application(new ErrorDialog(ex, message), cfg);
    }

    private static void checkLogger(final ConsoleLogger consoleLogger) {
        EventManager.instance.clearAllSubscriptions();
        consoleLogger.subscribe();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case SCENE_LOADED:
            if (REST_ENABLED) {
                /*
                 * Notify REST server that GUI is loaded and everything should be in a
                 * well-defined state
                 */
                try {
                    RESTServer.activate();
                } catch (SecurityException | IllegalArgumentException e) {
                    logger.error(e);
                }
            }
            break;
        case DISPOSE:
            if (REST_ENABLED) {
                /* Shutdown REST server thread on termination */
                try {
                    RESTServer.dispose();
                } catch (SecurityException | IllegalArgumentException e) {
                    logger.error(e);
                }
            }
            break;
        default:
            break;
        }

    }

    /**
     * Initialises the configuration file. Tries to load first the file in
     * <code>$GS_CONFIG_DIR/config.yaml</code>. Checks the
     * <code>version</code> key and compares it with the version in
     * the default configuration file of this release
     * to determine whether the config file must be overwritten.
     *
     * @throws IOException If the file fails to be written successfully.
     */
    private static void initConfigFile(final boolean vr) throws IOException {
        // Use user folder
        Path userFolderConfFile = SysUtils.getConfigDir().resolve(SettingsManager.getConfigFileName(vr));

        // Internal config
        Path confFolder = Settings.assetsPath("conf");
        Path internalFolderConfFile = confFolder.resolve(SettingsManager.getConfigFileName(vr));

        boolean overwrite = false;
        boolean userConfExists = Files.exists(userFolderConfFile);
        if (userConfExists) {
            Yaml yaml = new Yaml();
            Map<String, Object> userProps = yaml.load(Files.newInputStream(userFolderConfFile));
            int internalVersion = 0;
            if (Files.exists(internalFolderConfFile)) {
                Map<String, Object> internalProps = yaml.load(Files.newInputStream(internalFolderConfFile));
                internalVersion = (Integer) internalProps.get("configVersion");
            }

            // Check latest version
            if (!userProps.containsKey("configVersion")) {
                out.println("Properties file version not found, overwriting with new version (" + internalVersion + ")");
                overwrite = true;
            } else if ((Integer) userProps.get("configVersion") < internalVersion) {
                out.println("Properties file version mismatch, overwriting with new version: found " + userProps.get("version") + ", required " + internalVersion);
                overwrite = true;
            }
        } else {
            // No user configuration exists, try to morph the old configuration into the new one
            try {
                Path propertiesFile = SysUtils.getConfigDir().resolve(vr ? "global.vr.properties" : "global.properties");
                if (Files.exists(propertiesFile)) {
                    out.println("Old properties file detected!");
                    out.println("    -> Converting " + propertiesFile + " to " + userFolderConfFile);
                    SettingsMorph.morphSettings(propertiesFile, userFolderConfFile);
                    // Move old properties file so that they are not converted on the next run
                    Files.move(propertiesFile, SysUtils.getConfigDir().resolve(vr ? "global.vr.properties.old" : "global.properties.old"));
                    userConfExists = true;
                } else {
                    // Old configuration not found!
                    out.println("Failed updating old global.properties file into new config.yaml: Old configuration file not found");
                }
            } catch (Exception e) {
                // Failed!
            }
        }

        if (overwrite || !userConfExists) {
            // Copy file
            if (Files.exists(confFolder) && Files.isDirectory(confFolder)) {
                // Running released package
                GlobalResources.copyFile(internalFolderConfFile, userFolderConfFile, overwrite);
            } else {
                logger.warn("Configuration folder does not exist: " + confFolder);
            }
        }
        String props = userFolderConfFile.toAbsolutePath().toString();
        System.setProperty("properties.file", props);
    }

    /**
     * Checks whether the REST server dependencies are in the classpath.
     *
     * @return True if REST dependencies are loaded.
     */
    private static boolean checkRestDependenciesInClasspath() {
        try {
            Class.forName("spark.Spark");
            Class.forName("gaiasky.rest.RESTServer");
            return true;
        } catch (ClassNotFoundException e) {
            // my class isn't there!
            return false;
        }
    }

    /**
     * Checks for incompatibilities between the java version and the OS. Prints the necessary warnings for known issues.
     */
    private static void javaVersionCheck() {
        double jv = SysUtils.getJavaVersion();
        boolean linux = SysUtils.isLinux();
        boolean gnome = SysUtils.checkGnome();
        if (jv >= 10 && linux && gnome) {
            out.println("======================================= WARNING ========================================");
            out.println("It looks like you are running Gaia Sky with java " + jv + " in Linux with Gnome.\n" + "This version may crash. If it does, comment out the property\n" + "'assistive_technologies' in the '/etc/java-[version]/accessibility.properties' file.");
            out.println("========================================================================================");
            out.println();
        }

        if (jv < 9) {
            out.println("========================== ERROR ==============================");
            out.println("You are using Java " + jv + ", which is unsupported by Gaia Sky");
            out.println("             Please, use at least Java " + REQUIRED_JAVA_VERSION);
            out.println("===============================================================");
            JAVA_VERSION_FLAG = true;
        }
    }

    /**
     * Checks for experimental features and issues warnings
     */
    private static void experimentalCheck() {
        if (cliArgs.externalView) {
            out.println("============================ WARNING ================================");
            out.println("The -e/--externalview feature is experimental and may cause problems!");
            out.println("=====================================================================");
            out.println();
        }
    }
}
