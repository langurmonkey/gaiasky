package gaia.cu9.ari.gaiaorbit.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.brsanthu.googleanalytics.GoogleAnalyticsResponse;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.analytics.AnalyticsPermission;
import gaia.cu9.ari.gaiaorbit.analytics.AnalyticsReporting;
import gaia.cu9.ari.gaiaorbit.data.DesktopSceneGraphImplementationProvider;
import gaia.cu9.ari.gaiaorbit.data.SceneGraphImplementationProvider;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.render.DesktopPostProcessorFactory;
import gaia.cu9.ari.gaiaorbit.desktop.render.ScreenModeCmd;
import gaia.cu9.ari.gaiaorbit.desktop.util.*;
import gaia.cu9.ari.gaiaorbit.desktop.util.camera.CamRecorder;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.KeyBindings;
import gaia.cu9.ari.gaiaorbit.interfce.MusicActorsManager;
import gaia.cu9.ari.gaiaorbit.interfce.NetworkCheckerManager;
import gaia.cu9.ari.gaiaorbit.interfce.ConsoleLogger;
import gaia.cu9.ari.gaiaorbit.render.PostProcessorFactory;
import gaia.cu9.ari.gaiaorbit.screenshot.ScreenshotsManager;
import gaia.cu9.ari.gaiaorbit.script.JythonFactory;
import gaia.cu9.ari.gaiaorbit.script.ScriptingFactory;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.MathManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Main class for the desktop launcher
 *
 * @author Toni Sagrista
 */
public class GaiaSkyDesktop implements IObserver {
    private static final Log logger = Logger.getLogger(GaiaSkyDesktop.class);

    /* Configuration file version of the source code */
    public static int SOURCE_CONF_VERSION = 260;
    private static GaiaSkyDesktop gsd;
    private static boolean REST_ENABLED = false;
    private static Class<?> REST_SERVER_CLASS = null;

    private static GaiaSkyArgs gsargs;

    /**
     * Program arguments
     *
     * @author Toni Sagrista
     */
    private static class GaiaSkyArgs {
        @Parameter(names = {"-h", "--help"}, description = "Show program options and usage information", help = true, order = 0)
        private boolean help = false;

        @Parameter(names = {"-v", "--version"}, description = "List Gaia Sky version and relevant information.", order = 1)
        private boolean version = false;

        @Parameter(names = {"-d", "--ds-download"}, description = "Display the data download dialog at startup. If no data is found, the download dialog is shown automatically.", order = 2)
        private boolean download = false;

        @Parameter(names = {"-c", "--cat-chooser"}, description = "Display the catalog chooser dialog at startup. This enables the selection of different available catalogs when Gaia Sky starts.", order = 3)
        private boolean catalogChooser = false;

        @Parameter(names = {"-p", "--properties"}, description = "Specify the location of the properties file. Default: ~/.gaiasky/global.properties.", order = 4)
        private String propertiesFile = null;

        @Parameter(names = {"-a", "--assets"}, description = "Specify the location of the assets folder. If not present, the default assets location is used.", order = 5)
        private String assetsLocation = null;
    }

    /**
     * Formats the regular usage so that it removes the left padding characters.
     * This is necessary so that help2man recognizes the OPTIONS block.
     * @param jc The JCommander object
     */
    private static void printUsage(JCommander jc){
        StringBuilder sb = new StringBuilder();
        jc.usage(sb, "");
        String usage = sb.toString();

        sb = new StringBuilder();
        String[] lines = usage.split("\n");
        for(int i =0; i < lines.length; i++){
            if(i==0){
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
     * @param args Arguments
     */
    public static void main(String[] args) {
        gsargs = new GaiaSkyArgs();
        JCommander jc = JCommander.newBuilder().addObject(gsargs).build();
        jc.setProgramName("gaiasky");
        try {
            jc.parse(args);

            if (gsargs.help) {
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

            // Set properties file from arguments to VM params if needed
            if (gsargs.propertiesFile != null && !gsargs.propertiesFile.isEmpty()) {
                System.setProperty("properties.file", gsargs.propertiesFile);
            }

            // Set assets location to VM params if needed
            if (gsargs.assetsLocation != null && !gsargs.assetsLocation.isEmpty()) {
                System.setProperty("assets.location", gsargs.assetsLocation);
            }

            gsd = new GaiaSkyDesktop();
            
            Gdx.files = new Lwjgl3Files();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            // Init .gaiasky folder in user's home folder
            initUserDirectory();

            // Init properties file
            String props = System.getProperty("properties.file");
            if (props == null || props.isEmpty()) {
                initConfigFile(false);
            }

            // Initialize i18n (only for global config logging)
            I18n.initialize(Gdx.files.internal("i18n/gsbundle"));

            // Init global configuration
            ConfInit.initialize(new DesktopConfInit());

            // Reinitialize with user-defined locale
            I18n.initialize(Gdx.files.absolute(GlobalConf.ASSETS_LOC + "i18n/gsbundle"));

            if (gsargs.version) {
                System.out.println(GlobalConf.getShortApplicationName());
                System.out.println("License MPL 2.0: Mozilla Public License 2.0 <https://www.mozilla.org/en-US/MPL/2.0/>");
                System.out.println();
                System.out.println("Written by Toni Sagrista Selles <tsagrista@ari.uni-heidelberg.de>");
                return;
            }

            // Jython
            ScriptingFactory.initialize(JythonFactory.getInstance());

            // REST API server
            REST_ENABLED = GlobalConf.program.REST_PORT >= 0 && checkRestDepsInClasspath();
            if (REST_ENABLED) {
                REST_SERVER_CLASS = Class.forName("gaia.cu9.ari.gaiaorbit.rest.RESTServer");
                Method init = REST_SERVER_CLASS.getMethod("initialize", Integer.class);
                init.invoke(null, GlobalConf.program.REST_PORT);
            }

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

            // Analytics
            AnalyticsReporting.initialize(new AnalyticsPermission());
            AnalyticsReporting.getInstance().sendStartAppReport();

            // Math
            MathManager.initialize();

            gsd.init();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }

    private ConsoleLogger clogger;

    public GaiaSkyDesktop() {
        super();
        clogger = new ConsoleLogger();
        EventManager.instance.subscribe(this, Events.SCENE_GRAPH_LOADED, Events.DISPOSE);
    }

    private void init() {
        launchMainApp();
    }

    public void terminate() {
        System.exit(0);
    }

    public void launchMainApp() {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle(GlobalConf.APPLICATION_NAME);
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
            if (mymode == null)
                mymode = Lwjgl3ApplicationConfiguration.getDisplayMode(Gdx.graphics.getPrimaryMonitor());
            cfg.setFullscreenMode(mymode);
        } else {
            cfg.setWindowedMode(GlobalConf.screen.getScreenWidth(), GlobalConf.screen.getScreenHeight());
            cfg.setResizable(GlobalConf.screen.RESIZABLE);
        }
        cfg.setBackBufferConfig(8, 8, 8, 8, 24, 0, 0);
        cfg.setIdleFPS(0);
        cfg.useVsync(GlobalConf.screen.VSYNC);
        cfg.setWindowIcon(Files.FileType.Internal, "icon/ic_launcher.png");
        cfg.useOpenGL3(false, 3, 2);

        if (clogger != null) {
            clogger.unsubscribe();
            clogger = null;
        }

        // Launch app
        Lwjgl3Application app = new Lwjgl3Application(new GaiaSky(gsargs.download, gsargs.catalogChooser), cfg);
        app.addLifecycleListener(new GaiaSkyWindowListener());
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
                    Method activate;
                    try {
                        activate = REST_SERVER_CLASS.getMethod("activate");
                        activate.invoke(null, new Object[0]);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        logger.error(e);
                    }
                }
                break;
            case DISPOSE:
                if (REST_ENABLED) {
                    /* Shutdown REST server thread on termination */
                    try {
                        Method stop = REST_SERVER_CLASS.getMethod("stop");
                        stop.invoke(null, new Object[0]);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        logger.error(e);
                    }
                }
                break;
            default:
                break;
        }

    }

    private static void initUserDirectory() {
        SysUtils.getGSHomeDir().mkdirs();
        SysUtils.getDefaultFramesDir().mkdirs();
        SysUtils.getDefaultScreenshotsDir().mkdirs();
        SysUtils.getDefaultMusicDir().mkdirs();
        SysUtils.getDefaultScriptDir().mkdirs();
        SysUtils.getDefaultCameraDir().mkdirs();
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
    private static String initConfigFile(boolean ow) throws IOException {
        // Use user folder
        File userFolder = SysUtils.getGSHomeDir();
        userFolder.mkdirs();
        File userFolderConfFile = new File(userFolder, "global.properties");

        // Internal config
        File confFolder = new File("conf" + File.separator);
        File internalFolderConfFile = new File(confFolder, "global.properties");

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
            if (!userprops.containsKey("properties.version") || (userprops.containsKey("properties.version") && Integer.parseInt(userprops.getProperty("properties.version")) < internalversion)) {
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
                copyFile(new File("../assets/conf" + File.separator + "global.properties"), userFolderConfFile, overwrite);
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
            Class.forName("gaia.cu9.ari.gaiaorbit.rest.RESTServer");
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
        double jv = getVersion();
        boolean linux = SysUtils.isLinux();
        boolean gnome = SysUtils.checkGnome();
        if (jv >= 10 && linux && gnome) {
            System.out.println("======================================= WARNING ========================================");
            System.out.println("It looks like you are running Gaia Sky with java " + jv + " in Linux with Gnome.\n" + "This version may crash. If it does, comment out the property\n" + "'assistive_technologies' in the '/etc/java-[version]/accessibility.properties' file.");
            System.out.println("========================================================================================");
            System.out.println();
        }
    }

    private static double getVersion() {
        String version = System.getProperty("java.version");
        int pos = version.indexOf('.');
        pos = version.indexOf('.', pos + 1);
        return Double.parseDouble(version.substring(0, pos));
    }

    private class GaiaSkyWindowListener implements LifecycleListener {

        @Override
        public void pause() {

        }

        @Override
        public void resume() {

        }

        @Override
        public void dispose() {
            // Terminate here

            // Analytics stop event
            Future<GoogleAnalyticsResponse> f1 = AnalyticsReporting.getInstance().sendTimingAppReport();

            if (f1 != null)
                try {
                    f1.get(2000, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    logger.error(e);
                }

        }
    }
}
