/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Wee utility class to check the operating system and the desktop environment.
 * It also offers retrieval of common system folders.
 */
public class SysUtils {
    private static final Log logger = Logger.getLogger(SysUtils.class);

    /**
     * Initialise directories.
     */
    public static void mkdirs() {
        // Top level.
        try {
            Files.createDirectories(getDataDir());
            Files.createDirectories(getConfigDir());
            // Bottom level.
            Files.createDirectories(getDefaultCameraDir());
            Files.createDirectories(getDefaultMusicDir());
            Files.createDirectories(getDefaultFramesDir());
            Files.createDirectories(getDefaultScreenshotsDir());
            Files.createDirectories(getDefaultMappingsDir());
            Files.createDirectories(getDefaultBookmarksDir());
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public static void mkdir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private static final String OS;
    private static final boolean linux;
    private static final boolean mac;
    private static final boolean windows;
    private static final boolean unix;
    private static final boolean solaris;

    static {
        OS = System.getProperty("os.name").toLowerCase();
        linux = OS.contains("linux");
        mac = OS.contains("macos") || OS.contains("mac os");
        windows = OS.contains("win");
        unix = OS.contains("unix");
        solaris = OS.contains("sunos");
    }

    public static String getXdgDesktop() {
        return System.getenv("XDG_CURRENT_DESKTOP");
    }

    public static boolean checkLinuxDesktop(String desktop) {
        try {
            String value = getXdgDesktop();
            return value != null && !value.isEmpty() && value.equalsIgnoreCase(desktop);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    public static boolean checkUnity() {
        return isLinux() && checkLinuxDesktop("ubuntu");
    }

    public static boolean checkGnome() {
        return isLinux() && checkLinuxDesktop("gnome");
    }

    public static boolean checkKDE() {
        return isLinux() && checkLinuxDesktop("kde");
    }

    public static boolean checkXfce() {
        return isLinux() && checkLinuxDesktop("xfce");
    }

    public static boolean checkBudgie() {
        return isLinux() && checkLinuxDesktop("budgie:GNOME");
    }

    public static boolean checkI3() {
        return isLinux() && checkLinuxDesktop("i3");
    }

    public static String getOSName() {
        return OS;
    }

    public static String getOSFamily() {
        if (isLinux())
            return "linux";
        if (isWindows())
            return "win";
        if (isMac())
            return "macos";
        if (isUnix())
            return "unix";
        if (isSolaris())
            return "solaris";

        return "unknown";
    }

    public static boolean isLinux() {
        return linux;
    }

    public static boolean isWindows() {
        return windows;
    }

    public static boolean isMac() {
        return mac;
    }

    public static boolean isUnix() {
        return unix;
    }

    public static boolean isSolaris() {
        return solaris;
    }

    public static boolean launchedViaInstall4j() {
        return System.getProperty("install4j.appDir") != null;
    }

    public static String getOSArchitecture() {
        return System.getProperty("os.arch");
    }

    public static String getOSVersion() {
        return System.getProperty("os.version");
    }

    private static final String GAIASKY_DIR_NAME = "gaiasky";
    private static final String DOTGAIASKY_DIR_NAME = ".gaiasky";
    private static final String CAMERA_DIR_NAME = "camera";
    private static final String SCREENSHOTS_DIR_NAME = "screenshots";
    private static final String FRAMES_DIR_NAME = "frames";
    private static final String MUSIC_DIR_NAME = "music";
    private static final String MAPPINGS_DIR_NAME = "mappings";
    private static final String BOOKMARKS_DIR_NAME = "bookmarks";
    private static final String MPCDI_DIR_NAME = "mpcdi";
    private static final String DATA_DIR_NAME = "data";
    private static final String TMP_DIR_NAME = "tmp";
    private static final String CRASHREPORTS_DIR_NAME = "crashreports";

    /**
     * Gets a file pointer to the camera directory.
     *
     * @return A pointer to the Gaia Sky camera directory.
     */
    public static Path getDefaultCameraDir() {
        return getDataDir().resolve(CAMERA_DIR_NAME);
    }

    /**
     * Gets a file pointer to the default screenshots directory.
     *
     * @return A pointer to the Gaia Sky screenshots directory.
     */
    public static Path getDefaultScreenshotsDir() {
        return getDataDir().resolve(SCREENSHOTS_DIR_NAME);
    }

    /**
     * Gets a file pointer to the frames directory.
     *
     * @return A pointer to the Gaia Sky frames directory.
     */
    public static Path getDefaultFramesDir() {
        return getDataDir().resolve(FRAMES_DIR_NAME);
    }

    /**
     * Gets a file pointer to the music directory.
     *
     * @return A pointer to the Gaia Sky music directory.
     */
    public static Path getDefaultMusicDir() {
        return getDataDir().resolve(MUSIC_DIR_NAME);
    }

    /**
     * Gets a file pointer to the mappings directory.
     *
     * @return A pointer to the Gaia Sky mappings directory.
     */
    public static Path getDefaultMappingsDir() {
        return getConfigDir().resolve(MAPPINGS_DIR_NAME);
    }

    public static String getMappingsDirName() {

        return MAPPINGS_DIR_NAME;
    }

    /**
     * Gets a file pointer to the bookmarks directory.
     *
     * @return A pointer to the Gaia Sky bookmarks directory.
     */
    public static Path getDefaultBookmarksDir() {
        return getConfigDir().resolve(BOOKMARKS_DIR_NAME);
    }

    public static String getBookmarksDirName() {
        return BOOKMARKS_DIR_NAME;
    }

    /**
     * Gets a file pointer to the mpcdi directory.
     *
     * @return A pointer to the Gaia Sky mpcdi directory.
     */
    public static Path getDefaultMpcdiDir() {
        return getDataDir().resolve(MPCDI_DIR_NAME);
    }

    /**
     * Gets a file pointer to the local data directory where the data files are downloaded and stored.
     *
     * @return A pointer to the local data directory where the data files are.
     */
    public static Path getLocalDataDir() {
        return getDataDir().resolve(DATA_DIR_NAME);
    }

    /**
     * Gets a file pointer to the crash reports directory, where crash reports are stored.
     *
     * @return A pointer to the crash reports directory.
     */
    public static Path getCrashReportsDir() {
        return getDataDir().resolve(CRASHREPORTS_DIR_NAME);
    }

    /**
     * Gets the path to the actual temporary directory in the data folder. It needs the location of
     * the user-configured data folder as input.
     *
     * @param dataLocation The user-defined data location.
     *
     * @return A path that points to the temporary directory.
     */
    public static Path getTempDir(String dataLocation) {
        return Path.of(dataLocation).resolve(TMP_DIR_NAME);
    }

    /**
     * Returns the default data directory. That is ~/.gaiasky/ in Windows and macOS, and ~/.local/share/gaiasky
     * in Linux.
     *
     * @return Default data directory.
     */
    public static Path getDataDir() {
        if (isLinux()) {
            return getXdgDataHome().resolve(GAIASKY_DIR_NAME);
        } else {
            return getUserHome().resolve(DOTGAIASKY_DIR_NAME);
        }
    }

    /**
     * Returns the default cache directory, for non-essential data. This is ~/.gaiasky/ in Windows and macOS, and ~/.cache/gaiasky
     * in Linux.
     *
     * @return The default cache directory.
     */
    public static Path getCacheDir() {
        if (isLinux()) {
            return getXdgCacheHome().resolve(GAIASKY_DIR_NAME);
        } else {
            return getDataDir();
        }
    }

    public static Path getConfigDir() {
        if (isLinux()) {
            return getXdgConfigHome().resolve(GAIASKY_DIR_NAME);
        } else {
            return getUserHome().resolve(DOTGAIASKY_DIR_NAME);
        }
    }

    public static Path getHomeDir() {
        return getUserHome();
    }

    public static Path getUserHome() {
        return Paths.get(System.getProperty("user.home"));
    }

    private static Path getXdgDataHome() {
        String dataHome = System.getenv("XDG_DATA_HOME");
        if (dataHome == null || dataHome.isEmpty()) {
            return Paths.get(System.getProperty("user.home"), ".local", "share");
        } else {
            return Paths.get(dataHome);
        }
    }

    private static Path getXdgConfigHome() {
        String configHome = System.getenv("XDG_CONFIG_HOME");
        if (configHome == null || configHome.isEmpty()) {
            return Paths.get(System.getProperty("user.home"), ".config");
        } else {
            return Paths.get(configHome);
        }
    }

    private static Path getXdgCacheHome() {
        String cacheHome = System.getenv("XDG_CACHE_HOME");
        if (cacheHome == null || cacheHome.isEmpty()) {
            return Paths.get(System.getProperty("user.home"), ".cache");
        } else {
            return Paths.get(cacheHome);
        }
    }

    public static double getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.contains(("."))) {
            int pos = version.indexOf('.');
            pos = version.indexOf('.', pos + 1);
            return Double.parseDouble(version.substring(0, pos));
        } else {
            return Double.parseDouble(version);
        }
    }

    /**
     * Gets the path to the file containing the release notes.
     *
     * @return Path to the release notes file
     */
    public static Path getReleaseNotesFile() {
        Path workDir = Path.of(System.getProperty("user.dir"));
        return workDir.resolve("releasenotes.txt");
    }

    /**
     * Gets the path to the file containing the last shown
     * release notes version. This file is typically in the
     * configuration directory.
     *
     * @return Path to the release notes revision file
     */
    public static Path getReleaseNotesRevisionFile() {
        return getConfigDir().resolve(".releasenotes.rev");
    }

    public static Path getProceduralPixmapDir(){
        return Settings.settings.data.dataPath("tex").resolve("procedural");
    }

    /**
     * Saves the given procedurally generated pixmap as a PNG image
     * to disk using the given name and timestamp.
     *
     * @param p         The pixmap.
     * @param name      The name of the pixmap.
     */
    public static void saveProceduralPixmap(Pixmap p, String name) {
        if (p != null) {
            Path proceduralDir = getProceduralPixmapDir();
            Path file = proceduralDir.resolve(name + ".png");
            PixmapIO.writePNG(Gdx.files.absolute(file.toAbsolutePath().toString()), p);
            logger.info(TextUtils.capitalise(name) + " texture written to " + file);
        }
    }
}
