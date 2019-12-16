/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop;

import com.badlogic.gdx.Files;
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
import gaiasky.interfce.ConsoleLogger;
import gaiasky.interfce.KeyBindings;
import gaiasky.interfce.MusicActorsManager;
import gaiasky.interfce.NetworkCheckerManager;
import gaiasky.render.PostProcessorFactory;
import gaiasky.rest.RESTServer;
import gaiasky.screenshot.ScreenshotsManager;
import gaiasky.util.*;
import gaiasky.util.GlobalConf.SceneConf.ElevationType;
import gaiasky.util.Logger.Log;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.math.MathManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Properties;

/**
 * Main class for the desktop launcher
 *
 * @author Toni Sagrista
 */
public class GaiaSkyDesktop implements IObserver {
    private static final Log logger = Logger.getLogger(GaiaSkyDesktop.class);

    /*
     * Configuration file version of the source code
     * This is usually tag where each chunk takes 2 spaces.
     * Version = major.minor.rev -> 1.2.5 major=1; minor=2; rev=5
     * Version = major * 10000 + minor * 100 + rev
     * So 1.2.5 -> 10205
     *    2.1.7 -> 20107
     *
     * Leading zeroes are omitted to avoid octal literal interpretation.
     */
    public static int SOURCE_CONF_VERSION = 20204;
    private static GaiaSkyDesktop gsd;
    private static boolean REST_ENABLED = false;
    private static Class<?> REST_SERVER_CLASS = null;
    private static boolean JAVA_VERSION_FLAG = false;

    private static final String REQUIRED_JAVA_VERSION = "11";

    private static GaiaSkyArgs gsArgs;

    /**
     * Program arguments
     *
     * @author Toni Sagrista
     */
    private static class GaiaSkyArgs {
        @Parameter(names = { "-h", "--help" }, description = "Show program options and usage information", help = true, order = 0) private boolean help = false;

        @Parameter(names = { "-v", "--version" }, description = "List Gaia Sky version and relevant information.", order = 1) private boolean version = false;

        @Parameter(names = { "-d", "--ds-download" }, description = "Display the data download dialog at startup. If no data is found, the download dialog is shown automatically.", order = 2) private boolean download = false;

        @Parameter(names = { "-c", "--cat-chooser" }, description = "Display the catalog chooser dialog at startup. This enables the selection of different available catalogs when Gaia Sky starts.", order = 3) private boolean catalogChooser = false;

        @Parameter(names = { "-p", "--properties" }, description = "Specify the location of the properties file.", order = 4) private String propertiesFile = null;

        @Parameter(names = { "-a", "--assets" }, description = "Specify the location of the assets folder. If not present, the default assets location is used.", order = 5) private String assetsLocation = null;

        @Parameter(names = { "-vr", "--openvr" }, description = "Launch in Virtual Reality mode. Gaia Sky will attempt creating a VR context through OpenVR.", order = 6) private boolean vr = false;

        @Parameter(names = { "-e", "--externalview" }, description = "Create a window with a view of the scene and no UI.", order = 7) private boolean externalView = false;

        @Parameter(names = { "-n", "--noscript" }, description = "Do not start the scripting server. Useful to run more than one Gaia Sky instance at once.", order = 7) private boolean noScriptingServer = false;
    }

    /**
     * Formats the regular usage so that it removes the left padding characters.
     * This is necessary so that help2man recognizes the OPTIONS block.
     *
     * @param jc The JCommander object
     */
    private static void printUsage(JCommander jc) {
        StringBuilder sb = new StringBuilder();
        jc.usage(sb, "");
        String usage = sb.toString();

        sb = new StringBuilder();
        String[] lines = usage.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                // Add extra line between usage and options
                sb.append(lines[i] + "\n\n");
            } else {
                sb.append(lines[i].substring(2) + '\n');
            }
        }
        System.out.println(sb.toString());
    }

    /**
     * Main method
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
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
            System.out.print("gaiasky: bad program arguments\n\n");
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
                System.out.println(GlobalConf.getShortApplicationName());
                System.out.println("License MPL 2.0: Mozilla Public License 2.0 <https://www.mozilla.org/en-US/MPL/2.0/>");
                System.out.println();
                System.out.println("Written by Toni Sagrista Selles <tsagrista@ari.uni-heidelberg.de>");
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
            MusicManager.initialize(Gdx.files.absolute(GlobalConf.ASSETS_LOC + "music"), Gdx.files.absolute(SysUtils.getDefaultMusicDir().getAbsolutePath()));

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
                    logger.warn("Warning: no full screen mode with the given resolution found (" + GlobalConf.screen.FULLSCREEN_WIDTH + "x" + GlobalConf.screen.FULLSCREEN_HEIGHT+"). Falling back to windowed mode.");
                    cfg.setWindowedMode(GlobalConf.screen.getScreenWidth(), GlobalConf.screen.getScreenHeight());
                    cfg.setResizable(GlobalConf.screen.RESIZABLE);
                } else {
                    cfg.setFullscreenMode(mymode);
                }
            } else {
                cfg.setWindowedMode(GlobalConf.screen.getScreenWidth(), GlobalConf.screen.getScreenHeight());
                cfg.setResizable(GlobalConf.screen.RESIZABLE);
            }
            cfg.setBackBufferConfig(8, 8, 8, 8, 32, 0, 0);
            cfg.setIdleFPS(0);
            cfg.useVsync(GlobalConf.screen.VSYNC);
        } else {
            // Note that we disable VSync! The VRContext manages vsync with respect to the HMD
            cfg.useVsync(false);
            cfg.setWindowedMode(GlobalConf.screen.SCREEN_WIDTH, GlobalConf.screen.SCREEN_HEIGHT);
            cfg.setResizable(true);
        }
        if(gsArgs.vr){
            cfg.setWindowIcon(Files.FileType.Internal, "icon/gsvr_icon.png");
        } else {
            cfg.setWindowIcon(Files.FileType.Internal, "icon/gs_icon.png");
        }
        cfg.useOpenGL3(true, 4, 1);
        // Disable logical DPI modes (macOS, Windows)
        cfg.setHdpiMode(HdpiMode.Pixels);

        if (consoleLogger != null && EventManager.instance.isSubscribedToAny(consoleLogger)) {
            consoleLogger.unsubscribe();
        }

        // Launch app
        try {
            new Lwjgl3Application(new GaiaSky(gsArgs.download, gsArgs.catalogChooser, gsArgs.vr, gsArgs.externalView, gsArgs.noScriptingServer), cfg);
        } catch (GdxRuntimeException e) {
            if (!JAVA_VERSION_FLAG) {
                // Probably, OpenGL 4.x is not supported and window creation failed
                if (!EventManager.instance.isSubscribedToAny(consoleLogger))
                    consoleLogger.subscribe();
                logger.error("Window creation failed (is OpenGL 4.x supported by your card?), trying with OpenGL 3.x");
                logger.info("Disabling tessellation...");
                consoleLogger.unsubscribe();
                ElevationType et = GlobalConf.scene.ELEVATION_TYPE;
                if (!et.isNone()) {
                    GlobalConf.scene.ELEVATION_TYPE = ElevationType.PARALLAX_MAPPING;
                }
                cfg.useOpenGL3(true, 3, 2);

                Lwjgl3Application app = new Lwjgl3Application(new GaiaSky(gsArgs.download, gsArgs.catalogChooser, gsArgs.vr, gsArgs.externalView, gsArgs.noScriptingServer), cfg);
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
     * <code>$HOME/.gaiasky/global.properties</code>. Checks the
     * <code>properties.version</code> key to determine whether the file is
     * compatible or not. If it is, it uses the existing file. If it is not, it
     * replaces it with the default file.
     *
     * @param ow Whether to overwrite
     * @return The path of the file used
     * @throws IOException
     */
    private static String initConfigFile(boolean ow, boolean vr) throws IOException {
        // Use user folder
        File userFolderConfFile = new File(SysUtils.getConfigDir(), DesktopConfInit.getConfigFileName(vr));

        // Internal config
        File confFolder = new File("conf" + File.separator);
        File internalFolderConfFile = new File(confFolder, DesktopConfInit.getConfigFileName(vr));

        boolean overwrite = ow;
        if (userFolderConfFile.exists()) {
            Properties userprops = new Properties();
            userprops.load(new FileInputStream(userFolderConfFile));
            int internalversion = SOURCE_CONF_VERSION;
            if (internalFolderConfFile.exists()) {
                Properties internalprops = new Properties();
                internalprops.load(new FileInputStream(internalFolderConfFile));
                internalversion = Integer.parseInt(internalprops.getProperty("properties.version"));
            }

            // Check latest version
            if (!userprops.containsKey("properties.version") || Integer.parseInt(userprops.getProperty("properties.version")) < internalversion) {
                System.out.println("Properties file version mismatch, overwriting with new version: found " + Integer.parseInt(userprops.getProperty("properties.version")) + ", required " + internalversion);
                overwrite = true;
            }
        }

        if (overwrite || !userFolderConfFile.exists()) {
            // Copy file
            if (confFolder.exists() && confFolder.isDirectory()) {
                // Running released package
                copyFile(internalFolderConfFile, userFolderConfFile, overwrite);
            } else {
                // Running from code?
                if (!new File("../assets/conf" + File.separator).exists()) {
                    throw new IOException("File ../assets/conf does not exist!");
                }
                copyFile(new File("../assets/conf" + File.separator + DesktopConfInit.getConfigFileName(vr)), userFolderConfFile, overwrite);
            }
        }
        String props = userFolderConfFile.getAbsolutePath();
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

    @SuppressWarnings("resource")
    private static void copyFile(File sourceFile, File destFile, boolean ow) throws IOException {
        if (destFile.exists()) {
            if (ow) {
                // Overwrite, delete file
                destFile.delete();
            } else {
                return;
            }
        }
        // Create new
        destFile.createNewFile();

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
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
            System.out.println("======================================= WARNING ========================================");
            System.out.println("It looks like you are running Gaia Sky with java " + jv + " in Linux with Gnome.\n" + "This version may crash. If it does, comment out the property\n" + "'assistive_technologies' in the '/etc/java-[version]/accessibility.properties' file.");
            System.out.println("========================================================================================");
            System.out.println();
        }

        if (jv < 9) {
            System.out.println("========================== ERROR ==============================");
            System.out.println("You are using Java " + jv + ", which is unsupported by Gaia Sky");
            System.out.println("             Please, use at least Java " + REQUIRED_JAVA_VERSION);
            System.out.println("===============================================================");
            JAVA_VERSION_FLAG = true;
        }
    }

    /**
     * Checks for experimental features and issues warnings
     */
    private static void experimentalCheck(){
        if(gsArgs.externalView){
            System.out.println("============================ WARNING ================================");
            System.out.println("The -e/--externalview feature is experimental and may cause problems!");
            System.out.println("=====================================================================");
            System.out.println();
        }
    }
}
