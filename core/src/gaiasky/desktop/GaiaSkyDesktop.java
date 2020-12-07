/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import gaiasky.GaiaSky;
import gaiasky.data.DesktopSceneGraphImplementationProvider;
import gaiasky.data.SceneGraphImplementationProvider;
import gaiasky.desktop.format.DesktopDateFormatFactory;
import gaiasky.desktop.format.DesktopNumberFormatFactory;
import gaiasky.desktop.render.DesktopPostProcessorFactory;
import gaiasky.desktop.render.ScreenModeCmd;
import gaiasky.desktop.util.*;
import gaiasky.desktop.util.camera.CamRecorder;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interafce.ConsoleLogger;
import gaiasky.interafce.KeyBindings;
import gaiasky.interafce.MusicActorsManager;
import gaiasky.interafce.NetworkCheckerManager;
import gaiasky.render.PostProcessorFactory;
import gaiasky.rest.RESTServer;
import gaiasky.screenshot.ScreenshotsManager;
import gaiasky.util.*;
import gaiasky.util.GlobalConf.SceneConf.ElevationType;
import gaiasky.util.Logger.Log;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.math.MathManager;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Main class for the desktop launcher
 */
public class GaiaSkyDesktop implements IObserver {
    private static final Log logger = Logger.getLogger(GaiaSkyDesktop.class);

    /*
     * Source version to compare to config file and datasets.
     * This is usually tag where each chunk takes 2 spaces.
     * Version = major.minor.rev -> 1.2.5 major=1; minor=2; rev=5
     * Version = major * 10000 + minor * 100 + rev
     * So 1.2.5 -> 10205
     *    2.1.7 -> 20107
     *
     * Leading zeroes are omitted to avoid octal literal interpretation.
     */
    public static int SOURCE_VERSION = 30000;
    private static GaiaSkyDesktop gsd;
    private static boolean REST_ENABLED = false;
    private static boolean JAVA_VERSION_FLAG = false;

    private static final String REQUIRED_JAVA_VERSION = "11";

    private static GaiaSkyArgs gsArgs;

    /**
     * Program arguments
     */
    private static class GaiaSkyArgs {
        @Parameter(names = {"-h", "--help"}, description = "Show program options and usage information.", help = true, order = 0)
        private boolean help = false;

        @Parameter(names = {"-v", "--version"}, description = "List Gaia Sky version and relevant information.", order = 1)
        private boolean version = false;

        @Parameter(names = {"-i", "--asciiart"}, description = "Add nice ascii art to --version information.", order = 1)
        private boolean asciiart = false;

        @Parameter(names = {"-s", "--skip-welcome"}, description = "Skip the welcome screen if possible (base-data package must be present).", order = 2)
        private boolean skipWelcome = false;

        @Parameter(names = {"-p", "--properties"}, description = "Specify the location of the properties file.", order = 4)
        private String propertiesFile = null;

        @Parameter(names = {"-a", "--assets"}, description = "Specify the location of the assets folder. If not present, the default assets location (in the installation folder) is used.", order = 5)
        private String assetsLocation = null;

        @Parameter(names = {"-vr", "--openvr"}, description = "Launch in Virtual Reality mode. Gaia Sky will attempt to create a VR context through OpenVR.", order = 6)
        private boolean vr = false;

        @Parameter(names = {"-e", "--externalview"}, description = "Create a window with a view of the scene and no UI.", order = 7)
        private boolean externalView = false;

        @Parameter(names = {"-n", "--noscript"}, description = "Do not start the scripting server. Useful to run more than one Gaia Sky instance at once in the same machine.", order = 8)
        private boolean noScriptingServer = false;

        @Parameter(names = {"-d", "--debug"}, description = "Launch in debug mode. Prints out debug information from Gaia Sky to the logs.", order = 9)
        private boolean debug = false;

        @Parameter(names = {"-g", "--gpudebug"}, description = "Activate OpenGL debug mode. Prints out debug information from OpenGL to the standard output.", order = 9)
        private boolean debugGpu = false;
    }

    /**
     * Formats the regular usage so that it removes the left padding characters.
     * This is necessary so that help2man recognizes the OPTIONS block.
     *
     * @param jc The JCommander object
     */
    private static void printUsage(JCommander jc) {
        jc.usage();
    }

    /**
     * UTF-8 output stream printer
     **/
    private static PrintStream out;

    /**
     * Main method
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        gsArgs = new GaiaSkyArgs();
        JCommander jc = JCommander.newBuilder().addObject(gsArgs).build();
        jc.setProgramName("gaiasky");
        try {
            jc.parse(args);

            if (gsArgs.help) {
                printUsage(jc);
                return;
            }
        } catch (Exception e) {
            out.print("gaiasky: bad program arguments\n\n");
            printUsage(jc);
            return;
        }
        try {
            // Check java version
            javaVersionCheck();

            // Experimental features
            experimentalCheck();

            // Set properties file from arguments to VM params if needed
            if (gsArgs.propertiesFile != null && !gsArgs.propertiesFile.isEmpty()) {
                System.setProperty("properties.file", gsArgs.propertiesFile);
            }

            // Set assets location to VM params if needed
            if (gsArgs.assetsLocation != null && !gsArgs.assetsLocation.isEmpty()) {
                System.setProperty("assets.location", gsArgs.assetsLocation);
            }

            if (gsArgs.vr) {
                GlobalConf.APPLICATION_NAME += " VR";
            }

            gsd = new GaiaSkyDesktop();

            Gdx.files = new Lwjgl3Files();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            // Init gaiasky directories
            SysUtils.mkdirs();

            // Init properties file
            String props = System.getProperty("properties.file");
            if (props == null || props.isEmpty()) {
                initConfigFile(false, gsArgs.vr);
            }

            // Initialize i18n (only for global config logging)
            I18n.initialize(Gdx.files.internal("i18n/gsbundle"));

            // Init global configuration
            ConfInit.initialize(new DesktopConfInit(gsArgs.vr));

            // VR resolution
            if (gsArgs.vr) {
                Graphics.DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode();
                double sh = Math.min(dm.height, 1780);
                double aspect = 0.8383d;
                double scale = 1d;
                GlobalConf.screen.SCREEN_WIDTH = (int) (sh * aspect * scale);
                GlobalConf.screen.SCREEN_HEIGHT = (int) (sh * scale);
            }

            // Reinitialize with user-defined locale
            I18n.initialize(Gdx.files.absolute(GlobalConf.ASSETS_LOC + File.separator + "i18n/gsbundle"));

            if (gsArgs.version) {
                out.println(GlobalConf.getShortApplicationName());
                if (gsArgs.asciiart) {
                    BufferedReader ascii = new BufferedReader(new InputStreamReader(Gdx.files.internal("icon/gsascii.txt").read()));
                    out.println();
                    String line;
                    while ((line = ascii.readLine()) != null) {
                        out.println(line);
                    }
                }
                out.println();
                out.println("License MPL 2.0: Mozilla Public License 2.0 <https://www.mozilla.org/en-US/MPL/2.0/>");
                out.println("Written by " + GlobalConf.AUTHOR_NAME + " <" + GlobalConf.AUTHOR_EMAIL + ">");
                out.println();
                out.println(I18n.txt("gui.help.javaversion").toLowerCase() + ": " + System.getProperty("java.vm.version"));
                out.println(I18n.txt("gui.help.javavmname").toLowerCase() + ": " + System.getProperty("java.vm.name"));
                out.println();
                out.println("gaiasky homepage  <" + GlobalConf.WEBPAGE + ">");
                out.println("docs              <" + GlobalConf.DOCUMENTATION + ">");
                out.println();
                out.println("ZAH/DLR/BWT/DPAC");
                return;
            }

            ConsoleLogger consoleLogger = new ConsoleLogger();

            // REST API server
            REST_ENABLED = GlobalConf.program.REST_PORT >= 0 && checkRestDepsInClasspath();
            if (REST_ENABLED) {
                RESTServer.initialize(GlobalConf.program.REST_PORT);
            }

            // Slave manager
            SlaveManager.initialize();

            // Fullscreen command
            ScreenModeCmd.initialize();

            // Init cam recorder
            CamRecorder.initialize();

            // Music actors
            MusicActorsManager.initialize(new DesktopMusicActors());

            // Init music manager
            MusicManager.initialize(Paths.get(GlobalConf.ASSETS_LOC, "music"), SysUtils.getDefaultMusicDir());

            // Initialize post processor factory
            PostProcessorFactory.initialize(new DesktopPostProcessorFactory());

            // Key mappings
            KeyBindings.initialize();

            // Scene graph implementation provider
            SceneGraphImplementationProvider.initialize(new DesktopSceneGraphImplementationProvider());

            // Initialize screenshots manager
            ScreenshotsManager.initialize();

            // Network checker
            NetworkCheckerManager.initialize(new DesktopNetworkChecker());

            // Math
            MathManager.initialize();

            consoleLogger.dispose();

            gsd.init();
        } catch (Exception e) {
            CrashReporter.reportCrash(e, logger);
        }

    }

    public GaiaSkyDesktop() {
        super();
        EventManager.instance.subscribe(this, Events.SCENE_GRAPH_LOADED, Events.DISPOSE);
    }

    private void init() {
        launchMainApp();
    }

    public void launchMainApp() {
        ConsoleLogger consoleLogger = new ConsoleLogger();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle(GlobalConf.APPLICATION_NAME);
        if (!gsArgs.vr) {
            if (GlobalConf.screen.FULLSCREEN) {
                // Get mode
                DisplayMode[] modes = Lwjgl3ApplicationConfiguration.getDisplayModes();
                DisplayMode mymode = null;
                for (DisplayMode mode : modes) {
                    if (mode.height == GlobalConf.screen.FULLSCREEN_HEIGHT && mode.width == GlobalConf.screen.FULLSCREEN_WIDTH) {
                        mymode = mode;
                        break;
                    }
                }
                if (mymode == null) {
                    // Fall back to windowed
                    logger.warn("Warning: no full screen mode with the given resolution found (" + GlobalConf.screen.FULLSCREEN_WIDTH + "x" + GlobalConf.screen.FULLSCREEN_HEIGHT + "). Falling back to windowed mode.");
                    cfg.setWindowedMode(GlobalConf.screen.getScreenWidth(), GlobalConf.screen.getScreenHeight());
                    cfg.setResizable(GlobalConf.screen.RESIZABLE);
                } else {
                    cfg.setFullscreenMode(mymode);
                }
            } else {
                int w = GlobalConf.screen.getScreenWidth();
                int h = GlobalConf.screen.getScreenHeight();
                if (!SysUtils.isMac()) {
                    if (w <= 0 || h <= 0) {
                        try {
                            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                            w = (int) (gd.getDefaultConfiguration().getBounds().getWidth() * 0.8);
                            h = (int) (gd.getDefaultConfiguration().getBounds().getHeight() * 0.8);
                        } catch (HeadlessException he) {
                            logger.error("Error getting screen size from GraphicsDevice, trying Toolkit method");
                            logger.debug(he);
                        }
                    }
                    if (w <= 0 || h <= 0) {
                        try {
                            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                            w = (int) (screenSize.width * 0.8f);
                            h = (int) (screenSize.height * 0.8f);
                        } catch (Exception e) {
                            logger.error("Error getting screen size from Toolkit, defaulting to 1280x1024");
                            logger.debug(e);
                            // Default
                            w = 1600;
                            h = 900;
                        }
                    }
                } else {
                    // macOS is retarded and only likes headless mode, using default
                    w = 1600;
                    h = 900;
                }
                cfg.setWindowedMode(w, h);
                cfg.setResizable(GlobalConf.screen.RESIZABLE);
            }
            cfg.setBackBufferConfig(8, 8, 8, 8, 24, 8, 0);
            cfg.setIdleFPS(0);
            cfg.useVsync(GlobalConf.screen.VSYNC);
        } else {
            // Note that we disable VSync! The VRContext manages vsync with respect to the HMD
            cfg.useVsync(false);
            int w = GlobalConf.screen.SCREEN_WIDTH;
            int h = GlobalConf.screen.SCREEN_HEIGHT;
            if (w <= 0 || h <= 0) {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                w = (int) (screenSize.width * 0.85f);
                h = (int) (screenSize.height * 0.85f);
            }
            cfg.setWindowedMode(w, h);
            cfg.setResizable(true);
        }
        if (gsArgs.vr) {
            cfg.setWindowIcon(FileType.Internal, "icon/gsvr_icon.png");
        } else {
            cfg.setWindowIcon(FileType.Internal, "icon/gs_icon.png");
        }
        cfg.useOpenGL3(true, 4, 1);
        // Disable logical DPI modes (macOS, Windows)
        cfg.setHdpiMode(HdpiMode.Pixels);
        // OpenGL debug
        if (gsArgs.debugGpu) {
            cfg.enableGLDebugOutput(true, System.out);
        }

        if (consoleLogger != null && EventManager.instance.isSubscribedToAny(consoleLogger)) {
            consoleLogger.unsubscribe();
        }

        // Launch app
        GaiaSky gs = null;
        try {
            gs = new GaiaSky(gsArgs.skipWelcome, gsArgs.vr, gsArgs.externalView, gsArgs.noScriptingServer, gsArgs.debug);
            new Lwjgl3Application(gs, cfg);
        } catch (GdxRuntimeException e) {
            if (!JAVA_VERSION_FLAG) {
                if (!gs.windowCreated) {
                    // Probably, OpenGL 4.x is not supported and window creation failed
                    if (!EventManager.instance.isSubscribedToAny(consoleLogger)) {
                        consoleLogger.subscribe();
                        gs.dispose();
                    }
                    logger.error("Window creation failed (is OpenGL 4.x supported by your card?), trying with OpenGL 3.x");
                    logger.info("Disabling tessellation...");
                    consoleLogger.unsubscribe();
                    GlobalConf.scene.ELEVATION_TYPE = ElevationType.NONE;
                    cfg.useOpenGL3(true, 3, 2);

                    gs = new GaiaSky(gsArgs.skipWelcome, gsArgs.vr, gsArgs.externalView, gsArgs.noScriptingServer, gsArgs.debug);
                    new Lwjgl3Application(gs, cfg);
                } else {
                    logger.error("Gaia Sky crashed, please report the bug at " + GlobalConf.REPO_ISSUES);
                }
            } else {
                logger.error("Please update your java installation. Gaia Sky needs at least Java " + REQUIRED_JAVA_VERSION);
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    public void notify(Events event, final Object... data) {
        switch (event) {
            case SCENE_GRAPH_LOADED:
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
     * <code>$GS_CONFIG_DIR/global.properties</code>. Checks the
     * <code>properties.version</code> key and compares it with the version in
     * the default configuration file of this release
     * to determine whether the config file must be overwritten
     *
     * @param ow Whether to force overwrite
     * @return The path of the file used
     * @throws IOException
     */
    private static String initConfigFile(boolean ow, boolean vr) throws IOException {
        // Use user folder
        Path userFolderConfFile = SysUtils.getConfigDir().resolve(DesktopConfInit.getConfigFileName(vr));

        // Internal config
        Path confFolder = GlobalConf.assetsPath("conf");
        Path internalFolderConfFile = confFolder.resolve(DesktopConfInit.getConfigFileName(vr));

        boolean overwrite = ow;
        boolean userConfExists = Files.exists(userFolderConfFile);
        if (userConfExists) {
            Properties userProps = new Properties();
            userProps.load(Files.newInputStream(userFolderConfFile));
            int internalVersion = 0;
            if (Files.exists(internalFolderConfFile)) {
                Properties internalProps = new Properties();
                internalProps.load(Files.newInputStream(internalFolderConfFile));
                internalVersion = Integer.parseInt(internalProps.getProperty("properties.version"));
            }

            // Check latest version
            if (!userProps.containsKey("properties.version")) {
                out.println("Properties file version not found, overwriting with new version (" + internalVersion + ")");
                overwrite = true;
            } else if (Integer.parseInt(userProps.getProperty("properties.version")) < internalVersion) {
                out.println("Properties file version mismatch, overwriting with new version: found " + Integer.parseInt(userProps.getProperty("properties.version")) + ", required " + internalVersion);
                overwrite = true;
            }
        }

        if (overwrite || !userConfExists) {
            // Copy file
            if (Files.exists(confFolder) && Files.isDirectory(confFolder)) {
                // Running released package
                GlobalResources.copyFile(internalFolderConfFile, userFolderConfFile, overwrite);
            } else {
                logger.warn("Configuration folder does not exist: " + confFolder.toString());
            }
        }
        String props = userFolderConfFile.toAbsolutePath().toString();
        System.setProperty("properties.file", props);
        return props;
    }

    /**
     * Checks whether the REST server dependencies are in the classpath.
     *
     * @return True if REST dependencies are loaded.
     */
    private static boolean checkRestDepsInClasspath() {
        try {
            Class.forName("com.google.gson.Gson");
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
        if (gsArgs.externalView) {
            out.println("============================ WARNING ================================");
            out.println("The -e/--externalview feature is experimental and may cause problems!");
            out.println("=====================================================================");
            out.println();
        }
    }
}
