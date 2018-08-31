package gaia.cu9.ari.gaiaorbit.desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
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
import gaia.cu9.ari.gaiaorbit.desktop.util.CamRecorder;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopMusicActors;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopNetworkChecker;
import gaia.cu9.ari.gaiaorbit.desktop.util.LogWriter;
import gaia.cu9.ari.gaiaorbit.desktop.util.MemInfoWindow;
import gaia.cu9.ari.gaiaorbit.desktop.util.RunCameraWindow;
import gaia.cu9.ari.gaiaorbit.desktop.util.RunScriptWindow;
import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.KeyBindings;
import gaia.cu9.ari.gaiaorbit.interfce.MusicActorsManager;
import gaia.cu9.ari.gaiaorbit.interfce.NetworkCheckerManager;
import gaia.cu9.ari.gaiaorbit.render.PostProcessorFactory;
import gaia.cu9.ari.gaiaorbit.screenshot.ScreenshotsManager;
import gaia.cu9.ari.gaiaorbit.script.JythonFactory;
import gaia.cu9.ari.gaiaorbit.script.ScriptingFactory;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.MusicManager;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.MathManager;

/**
 * Main class for the desktop launcher
 * 
 * @author Toni Sagrista
 *
 */
public class GaiaSkyDesktop implements IObserver {
    private static final Log logger = Logger.getLogger(GaiaSkyDesktop.class);

    /* Configuration file version of the source code */
    private static int SOURCE_CONF_VERSION = 251;
    private static GaiaSkyDesktop gsd;
    public static String ASSETS_LOC;

    private MemInfoWindow memInfoWindow;

    /**
    	 * Program arguments
    	 * 
    	 * @author Toni Sagrista
    	 *
    	 */
    private static class GaiaSkyArgs {
        @Parameter(names = { "-h", "--help" }, help = true)
        private boolean help = false;

        @Parameter(names = { "-v", "--version" }, description = "Lists version and build inforamtion")
        private boolean version = false;
    }

    public static void main(String[] args) {
        GaiaSkyArgs gsargs = new GaiaSkyArgs();
        try {
            JCommander jc = new JCommander(gsargs, args);
            jc.setProgramName("gaiasky");
            if (gsargs.help) {
                jc.usage();
                return;
            }
        } catch (Exception e) {
            System.out.println("Bad program arguments");
            return;
        }
        try {
            // Check java version
            javaVersionCheck();

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
                props = initConfigFile(false);
            }

            // Init global configuration
            ConfInit.initialize(new DesktopConfInit());
            GlobalConf.screen.SCREEN_WIDTH = 1080;
            GlobalConf.screen.SCREEN_HEIGHT = 1200;

            if (gsargs.version) {
                System.out.println(GlobalConf.APPLICATION_NAME + " " + GlobalConf.version.version);
                System.out.println("   version name : " + GlobalConf.version.version);
                System.out.println("   build        : " + GlobalConf.version.build);
                System.out.println("   build time   : " + GlobalConf.version.buildtime);
                System.out.println("   build system : " + GlobalConf.version.system);
                System.out.println("   builder      : " + GlobalConf.version.builder);
                return;
            }

            // Initialize i18n
            I18n.initialize(Gdx.files.internal("i18n/gsbundle"));

            // Dev mode
            I18n.initialize(Gdx.files.absolute(ASSETS_LOC + "i18n/gsbundle"));

            // Jython
            ScriptingFactory.initialize(JythonFactory.getInstance());

            // Fullscreen command
            ScreenModeCmd.initialize();

            // Init cam recorder
            CamRecorder.initialize();

            // Music actors
            MusicActorsManager.initialize(new DesktopMusicActors());

            // Init music manager
            MusicManager.initialize(Gdx.files.absolute(ASSETS_LOC + "music"), Gdx.files.absolute(SysUtils.getDefaultMusicDir().getAbsolutePath()));

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

    public static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value != null && value instanceof javax.swing.plaf.FontUIResource
                    && ((FontUIResource) value).getSize() > f.getSize()) {
                UIManager.put(key, f);
            }
        }
    }

    private LogWriter lw;

    public GaiaSkyDesktop() {
        super();
        lw = new LogWriter();
        EventManager.instance.subscribe(this, Events.SHOW_RUNSCRIPT_ACTION, Events.SHOW_PLAYCAMERA_ACTION, Events.DISPLAY_MEM_INFO_WINDOW);
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
        cfg.setTitle(GlobalConf.getFullApplicationName());
        // Note that we disable VSync! The VRContext manages vsync with respect to the HMD
        cfg.useVsync(false);
        cfg.setWindowedMode(GlobalConf.screen.SCREEN_WIDTH, GlobalConf.screen.SCREEN_HEIGHT);
        cfg.setResizable(true);

        // Launch app
        Lwjgl3Application app = new Lwjgl3Application(new GaiaSky(), cfg);
        //app.addLifecycleListener(new GaiaSkyWindowListener());

        if (lw != null)
            EventManager.instance.removeAllSubscriptions(lw);
    }

    RunScriptWindow scriptWindow = null;
    RunCameraWindow cameraWindow = null;

    @Override
    public void notify(Events event, final Object... data) {
        switch (event) {
        case SHOW_PLAYCAMERA_ACTION:
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    if (cameraWindow == null)
                        cameraWindow = new RunCameraWindow((Stage) data[0], (Skin) data[1]);
                    cameraWindow.display();
                }
            });
            break;
        case SHOW_RUNSCRIPT_ACTION:
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    if (scriptWindow == null)
                        scriptWindow = new RunScriptWindow((Stage) data[0], (Skin) data[1]);
                    scriptWindow.display();
                }
            });

            break;
        case DISPLAY_MEM_INFO_WINDOW:
            if (memInfoWindow == null) {
                memInfoWindow = new MemInfoWindow((Stage) data[0], (Skin) data[1]);
            }
            memInfoWindow.show((Stage) data[0]);
            break;
        case JAVA_EXCEPTION:
            ((Throwable) data[0]).printStackTrace(System.err);
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
     * <code>$HOME/.gaiasky/global.vr.properties</code>. Checks the
     * <code>properties.version</code> key to determine whether the file is
     * compatible or not. If it is, it uses the existing file. If it is not, it
     * replaces it with the default file.
     * 
     * @param ow
     *            Whether to overwrite
     * @return The path of the file used
     * @throws IOException
     */
    private static String initConfigFile(boolean ow) throws IOException {
        // Use user folder
        File userFolder = SysUtils.getGSHomeDir();
        userFolder.mkdirs();
        File userFolderConfFile = new File(userFolder, "global.vr.properties");

        // Internal config
        File confFolder = new File("conf" + File.separator);
        File internalFolderConfFile = new File(confFolder, "global.vr.properties");

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
            if (!userprops.containsKey("properties.version") || (userprops.containsKey("properties.version")
                    && Integer.parseInt(userprops.getProperty("properties.version")) < internalversion)) {
                System.out.println("Properties file version mismatch, overwriting with new version: found "
                        + Integer.parseInt(userprops.getProperty("properties.version")) + ", required "
                        + internalversion);
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
                copyFile(new File("../assets/conf" + File.separator + "global.vr.properties"), userFolderConfFile, overwrite);
            }
        }
        String props = userFolderConfFile.getAbsolutePath();
        System.setProperty("properties.file", props);
        return props;
    }

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
        SysUtils sys = new SysUtils();
        boolean linux = sys.isLinux();
        boolean gnome = sys.checkGnome();
        if (jv >= 10 && linux && gnome) {
            System.out.println("======================================= WARNING ========================================");
            System.out.println("It looks like you are running Gaia Sky with java " + jv + " in Linux with Gnome.\n"
                    + "This version may crash. If it does, comment out the property\n"
                    + "'assistive_technologies' in the '/etc/java-[version]/accessibility.properties' file.");
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
