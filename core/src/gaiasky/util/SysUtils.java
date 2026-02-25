/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.screenshot.JPGWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * Some handy system utilities and constants.
 */
public class SysUtils {
    private static final Log logger = Logger.getLogger(SysUtils.class);
    private static final String OS;
    private static final boolean linux;
    private static final boolean steamDeck;
    private static final boolean mac;
    private static final boolean windows;
    private static final boolean unix;
    private static final boolean solaris;
    private static final boolean flatpak;
    private static final String ARCH;
    private static final boolean aArch64;
    private static final String GAIASKY_DIR_NAME = "gaiasky";
    private static final String DOTGAIASKY_DIR_NAME = ".gaiasky";
    private static final String CAMERA_DIR_NAME = "camera";
    private static final String SCREENSHOTS_DIR_NAME = "screenshots";
    private static final String FRAMES_DIR_NAME = "frames";
    private static final String MAPPINGS_DIR_NAME = "mappings";
    private static final String BOOKMARKS_DIR_NAME = "bookmarks";
    private static final String MPCDI_DIR_NAME = "mpcdi";
    private static final String DATA_DIR_NAME = "data";
    private static final String CRASHREPORTS_DIR_NAME = "crashreports";
    private static final String SHADER_OUT_DIR_NAME = "shaders";
    private static final String SHADER_CACHE_DIR_NAME = "shadercache";
    private static final String LOG_DIR_NAME = "log";
    public static final String TMP_DIR_NAME = "tmp";
    public static final String CACHE_DIR_NAME = "cache";
    public static final String PROCEDURAL_TEX_DIR_NAME = "procedural-planet-textures";


    static {
        OS = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        linux = OS.contains("linux");
        mac = OS.contains("macos") || OS.contains("mac os");
        windows = OS.contains("win");
        unix = OS.contains("unix");
        solaris = OS.contains("sunos");

        final var sysInfo = new SystemInfo();
        final var codeName = sysInfo.getOperatingSystem().getVersionInfo().getCodeName();
        flatpak = codeName != null && codeName.contains("Flatpak");

        if (linux) {
            // Check Steam Deck.
            String boardVendor = null, boardName = null;
            try {
                var vendorPath = Paths.get("/sys/devices/virtual/dmi/id/board_vendor");
                var namePath = Paths.get("/sys/devices/virtual/dmi/id/board_name");
                boardVendor = Files.exists(vendorPath) && Files.isReadable(vendorPath) ? new String(Files.readAllBytes(vendorPath)) : null;
                boardName = Files.exists(namePath) && Files.isReadable(namePath) ? new String(Files.readAllBytes(namePath)) : null;
            } catch (IOException ignored) {
                // Nothing.
            }
            // Check APU.
            CentralProcessor cp = (new SystemInfo()).getHardware().getProcessor();
            boolean deckAPU = cp.getProcessorIdentifier().getName().equalsIgnoreCase("AMD Custom APU 0405");

            steamDeck = (boardVendor != null && boardVendor.equalsIgnoreCase("Valve")) || (boardName != null && boardName.equalsIgnoreCase("Jupiter")) || deckAPU;

        } else {
            steamDeck = false;
        }

        ARCH = System.getProperty("os.arch");
        aArch64 = ARCH.contains("aarch64");
    }

    /**
     * Initialize directories.
     */
    public static void mkdirs() {
        // Top level.
        try {
            Files.createDirectories(getDataDir());
            Files.createDirectories(getConfigDir());
            Files.createDirectories(getCacheDir());
            // Bottom level.
            Files.createDirectories(getDefaultCameraDir());
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
        if (isLinux()) return "linux";
        if (isWindows()) return "win";
        if (isMac()) return "macos";
        if (isUnix()) return "unix";
        if (isSolaris()) return "solaris";

        return "unknown";
    }

    public static boolean isLinux() {
        return linux;
    }

    public static boolean isX11() {
        var sessionType = System.getenv("XDG_SESSION_TYPE");
        return sessionType != null && sessionType.equals("x11");
    }

    public static boolean isGLFWX11() {
        return GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_X11;
    }

    public static boolean isWayland() {
        var sessionType = System.getenv("XDG_SESSION_TYPE");
        return sessionType != null && sessionType.equals("wayland");
    }

    public static boolean isGLFWWayland() {
        return GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND;
    }

    public static boolean isWindows() {
        return windows;
    }

    public static boolean isMac() {
        return mac;
    }

    public static boolean isFlatpak() {
        return flatpak;
    }

    public static boolean isAppleSiliconMac() {
        return isMac() && aArch64;
    }

    public static boolean isUnix() {
        return unix;
    }

    public static boolean isSolaris() {
        return solaris;
    }

    public static boolean isSteamDeck() {
        return steamDeck;
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
     * Gets a file pointer to the frames' directory.
     *
     * @return A pointer to the Gaia Sky frames directory.
     */
    public static Path getDefaultFramesDir() {
        return getDataDir().resolve(FRAMES_DIR_NAME);
    }

    /**
     * Gets a file pointer to the mappings' directory.
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
     * Gets a file pointer to the MPCDI directory.
     *
     * @return A pointer to the Gaia Sky MPCDI directory.
     */
    public static Path getDefaultMpcdiDir() {
        return getDataDir().resolve(MPCDI_DIR_NAME);
    }

    /**
     * Gets a file pointer to the default directory where datasets are downloaded and stored.
     *
     * @return A pointer to the default datasets directory.
     */
    public static Path getDefaultDatasetsDir() {
        return getDataDir().resolve(DATA_DIR_NAME);
    }

    /**
     * Gets a file pointer to the crash reports directory, where crash reports are stored.
     *
     * @return A pointer to the crash reports' directory.
     */
    public static Path getCrashReportsDir() {
        return getDataDir().resolve(CRASHREPORTS_DIR_NAME);
    }

    /**
     * Gets a pointer to the shader output directory for the crash reports.
     *
     * @return A pointer to the shader output directory.
     */
    public static Path getCrashShadersDir() {
        return getCrashReportsDir().resolve(SHADER_OUT_DIR_NAME);
    }

    /**
     * Gets a file pointer to the log directory, where the log for the last session is stored.
     *
     * @return A pointer to the log directory.
     */
    public static Path getLogDir() {
        return getDataDir().resolve(LOG_DIR_NAME);
    }

    /**
     * Gets the path to the actual temporary directory in the data folder. It needs the location of
     * the user-configured data folder as input.
     * <p>
     * The temp directory is used to store partial dataset downloads and data descriptors.
     *
     * @param dataLocation The user-defined data location.
     *
     * @return A path that points to the temporary directory.
     */
    public static Path getDataTempDir(String dataLocation) {
        return Path.of(dataLocation).resolve(TMP_DIR_NAME);
    }

    /**
     * Gets the path to the actual cache directory in the data folder. It needs the location of
     * the user-configured data folder as input.
     * <p>
     * The cache directory is used to store TLE and other time-changing data.
     *
     * @param dataLocation The user-defined data location.
     *
     * @return A path that points to the cache directory.
     */
    public static Path getDataCacheDir(String dataLocation) {
        return Path.of(dataLocation).resolve(CACHE_DIR_NAME);
    }

    /**
     * Returns the default data directory. That is <code>~/.gaiasky/</code> in Windows and macOS, and <code>~/.local/share/gaiasky</code>
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
     * Returns the default cache directory, for non-essential data. This is <code>~/.gaiasky/</code> in Windows and macOS, and
     * <code>~/.cache/gaiasky</code> in Linux.
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

    /**
     * Returns the default shader cache directory. This is <code>~/.gaiasky/shadercache/</code> in Windows and macOS, and
     * <code>~/.cache/gaiasky/shadercache/</code> in Linux.
     *
     * @return The default shader cache directory.
     */
    public static Path getShaderCacheDir() {
        if (isLinux()) {
            return getXdgCacheHome().resolve(GAIASKY_DIR_NAME).resolve(SHADER_CACHE_DIR_NAME);
        } else {
            return getDataDir().resolve(SHADER_CACHE_DIR_NAME);
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
        if (dataHome == null || dataHome.isBlank()) {
            return getUserHome().resolve(".local").resolve("share");
        } else {
            return Paths.get(dataHome);
        }
    }

    private static Path getLocalAppData() {
        String dataFolder = System.getenv("LOCALAPPDATA");
        if (dataFolder == null || dataFolder.isBlank()) {
            return getUserHome().resolve("AppData").resolve("Local");
        } else {
            return Paths.get(dataFolder);
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

    public static Path getLocalAppDataTemp() {
        return getLocalAppData().resolve("Temp");
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
     * @return Path to the release notes revision file.
     */
    public static Path getReleaseNotesRevisionFile() {
        return getConfigDir().resolve(".releasenotes.rev");
    }

    public static Path getProceduralPixmapDir() {
        return Settings.settings.data.dataPath("$data/", null).resolve(PROCEDURAL_TEX_DIR_NAME);
    }

    /**
     * Saves the given texture with the given name to disk as an image file with the given format.
     *
     * @param texture The texture to save.
     * @param name    The name of the texture.
     * @param format  The image format to use.
     */
    public static void saveProceduralGLTexture(Texture texture, String name, Settings.ImageFormat format) {
        if (texture != null && name != null) {
            Path proceduralDir = getProceduralPixmapDir();
            Path file = proceduralDir.resolve(name + "." + format.extension);
            try {
                Files.createDirectories(proceduralDir);
            } catch (IOException e) {
                logger.error("Error creating procedural textures directory: " + proceduralDir);
                return;
            }

            Pixmap pixmap = pixmapFromGLTexture(texture);

            GaiaSky.instance.getExecutorService().execute(() -> {
                switch (format) {
                    case JPG -> JPGWriter.write(Gdx.files.absolute(file.toAbsolutePath().toString()), pixmap);
                    case PNG -> PixmapIO.writePNG(Gdx.files.absolute(file.toAbsolutePath().toString()), pixmap);
                }
                logger.info(I18n.msg("gui.procedural.info.savetextures.ok", TextUtils.capitalise(name), file));
                pixmap.dispose();
            });
        }
    }

    /**
     * Saves all the given textures with the given names to disk as image files with the given format.
     * This works in two steps. First, the pixmaps are prepared
     * from the textures in the current thread. Then, the pixmaps are saved to disk in a separate thread using the
     * executor service.
     *
     * @param textures The textures to save.
     * @param names    The names of the textures.
     * @param format   The image format to use.
     */
    public static void saveProceduralGLTextures(Texture[] textures, String[] names, Settings.ImageFormat format) {
        Path proceduralDir = getProceduralPixmapDir();
        try {
            Files.createDirectories(proceduralDir);
        } catch (IOException e) {
            logger.error("Error creating procedural textures directory: " + proceduralDir);
            return;
        }
        Array<Pair<Pixmap, Integer>> pixmaps = new Array<>();
        // Prepare pixmaps in current (main) thread.
        for (int i = 0; i < textures.length; i++) {
            var t = textures[i];
            if (t != null) {
                Pixmap pixmap = pixmapFromGLTexture(t);
                pixmaps.add(new Pair<>(pixmap, i));
            }
        }

        // Save textures in new thread.
        GaiaSky.instance.getExecutorService().execute(() -> {
            for (var pair : pixmaps) {
                var pixmap = pair.getFirst();
                var name = names[pair.getSecond()];
                Path file = proceduralDir.resolve(name + "." + format.extension);
                switch (format) {
                    case JPG -> JPGWriter.write(Gdx.files.absolute(file.toAbsolutePath().toString()), pixmap);
                    case PNG -> PixmapIO.writePNG(Gdx.files.absolute(file.toAbsolutePath().toString()), pixmap);
                }
                logger.info(I18n.msg("gui.procedural.info.savetextures.ok", TextUtils.capitalise(name), file));
                pixmap.dispose();
            }
            // Post popup.
            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                 pixmaps,
                                 I18n.msg("gui.procedural.info.savetextures", SysUtils.getProceduralPixmapDir().toString()));

        });


    }

    /**
     * Creates a {@link Pixmap} from a {@link Texture} for CPU access.
     *
     * @param t The Texture.
     *
     * @return The Pixmap.
     */
    public static Pixmap pixmapFromGLTexture(Texture t) {
        int w = t.getWidth();
        int h = t.getHeight();

        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

        final Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        ByteBuffer pixels = pixmap.getPixels();

        t.bind();
        GL30.glGetTexImage(t.glTarget, 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, pixels);

        return pixmap;
    }

    /**
     * Checks if the given file path belongs to an AppImage.
     *
     * @param path The path to check.
     *
     * @return Whether the path to the file belongs to an AppImage or not.
     */
    public static boolean isAppImagePath(String path) {
        return path != null && path.contains("/tmp/.mount_");
    }

    /**
     * Returns whether we are running in an AppImage.
     *
     * @return True if we are in an AppImage package.
     */
    public static boolean isAppImage() {
        String userDir = System.getProperty("user.dir");
        return isAppImagePath(userDir);
    }

    /**
     * Gets the current display resolution.
     *
     * @return The display resolution in an array with [width, height], or null if the resolution could not be
     *         determined.
     */
    public static int[] getDisplayResolution() {
        int w, h;

        // MacOS seems to be "special", only likes headless mode.
        // If we access any of the AWT classes we get a nice black screen.
        if (isMac()) {
            return new int[]{Constants.DEFAULT_RESOLUTION_WIDTH, Constants.DEFAULT_RESOLUTION_HEIGHT};
        }

        // Graphics device method, screen device.
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            AffineTransform transform = gc.getDefaultTransform();
            double scaleX = transform.getScaleX();
            double scaleY = transform.getScaleY();
            w = (int) (gc.getBounds().getWidth() * scaleX);
            h = (int) (gc.getBounds().getHeight() * scaleY);
            if (w > 0 && h > 0) {
                return new int[]{w, h};
            } else {
                logger.warn(I18n.msg("error.screensize.gd"));
            }
        } catch (HeadlessException e) {
            logger.warn(I18n.msg("error.screensize.gd"));
            logger.debug(e);
        }

        // Graphics device method, window bounds.
        try {
            Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            double dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            double scale = dpi > 0.0 ? dpi / 96.0 : 1.0;
            w = (int) (rect.width * scale);
            h = (int) (rect.height * scale);
            if (w > 0 && h > 0) {
                return new int[]{w, h};
            } else {
                logger.warn(I18n.msg("error.screensize.gd.windowbounds"));
            }
        } catch (HeadlessException e) {
            logger.warn(I18n.msg("error.screensize.gd.windowbounds"));
            logger.debug(e);
        }

        // Toolkit method.
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            double dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            double scale = dpi > 0.0 ? dpi / 96.0 : 1.0;
            w = (int) (screenSize.getWidth() * scale);
            h = (int) (screenSize.getHeight() * scale);
            if (w > 0 && h > 0) {
                return new int[]{w, h};
            } else {
                logger.warn(I18n.msg("error.screensize.toolkit"));
            }
        } catch (Exception e) {
            logger.warn(I18n.msg("error.screensize.toolkit"));
            logger.debug(e);
        }

        return null;
    }

    private static String getJarName() {
        return new File(SysUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
    }

    public static Path getProgramDirectory() {
        if (runningFromJAR()) {
            return getCurrentJARDirectory();
        } else {
            return getCurrentProjectDirectory();
        }
    }

    private static boolean runningFromJAR() {
        String jarName = getJarName();
        return jarName.contains(".jar");
    }

    private static Path getCurrentProjectDirectory() {
        return new File("").toPath().toAbsolutePath();
    }

    private static Path getCurrentJARDirectory() {
        try {
            return Path.of(new File(SysUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent())
                    .toAbsolutePath();
        } catch (URISyntaxException exception) {
            logger.error(exception);
        }

        return null;
    }

    public static Path uniqueFileName(Path file) {
        if (!Files.isRegularFile(file)) {
            logger.error("Given path is not a file: " + file);
            return file;
        }

        var parent = file.getParent();
        var fileName = FilenameUtils.getBaseName(file.toString());
        var ext = FilenameUtils.getExtension(file.toString());

        // Parse the base name to see if it already ends with a number pattern
        var baseName = fileName;
        var pattern = Pattern.compile("^(.*?)(_\\d+)?$");
        var matcher = pattern.matcher(fileName);

        if (matcher.matches()) {
            baseName = matcher.group(1);  // Get the part before any _number
            // If there was a number suffix, we'll start counting from that number
            if (matcher.group(2) != null) {
                var currentNum = Integer.parseInt(matcher.group(2).substring(1));
                fileName = baseName + "_" + currentNum;
            }
        }

        var f = parent.resolve(fileName + "." + ext);
        int i = 1;

        // If the original file exists, find the next available number
        if (Files.exists(f) && Files.isRegularFile(f)) {
            // Look for existing files with pattern: baseName_X.ext
            while (true) {
                f = parent.resolve(baseName + "_" + i + "." + ext);
                if (!Files.exists(f) || !Files.isRegularFile(f)) {
                    break;
                }
                i++;
            }
        }

        return f;
    }

    /**
     * Given an array of string representing either directories or actual files, this method walks the directory tree and gathers an array of
     * all the files with the given extensions.
     *
     * @param files An alphabetically sorted list of files of the given type.
     */
    public static Array<String> gatherFilesExtension(String[] files, String[] extensions) {
        Array<String> actualFilePaths = new Array<>();
        if (files == null || extensions == null) {
            return actualFilePaths;
        }
        for (String textureFile : files) {
            String unpackedFile = Settings.settings.data.dataFile(textureFile);
            Path galLocationPath = Path.of(unpackedFile);
            if (Files.exists(galLocationPath)) {
                if (Files.isDirectory(galLocationPath)) {
                    // Directory.
                    Collection<File> galaxyFiles = FileUtils.listFiles(galLocationPath.toFile(), extensions, true);
                    if (!galaxyFiles.isEmpty()) {
                        for (File f : galaxyFiles) {
                            actualFilePaths.add(f.getAbsolutePath());
                        }
                    }
                } else {
                    // File.
                    actualFilePaths.add(galLocationPath.toAbsolutePath().toString());
                }
            }
        }
        // Sort using natural order.
        actualFilePaths.sort();
        return actualFilePaths;
    }

    public static void checkForOpenGLErrors(String location) {
        checkForOpenGLErrors(location, false);
    }

    public static void checkForOpenGLErrors(String location, boolean silent) {
        int error = GL43.glGetError();
        if (error != GL43.GL_NO_ERROR) {
            if (!silent)
                logger.error("OpenGL error at " + location + ": " + error);
            // Optionally clear the error so it doesn't affect future operations
            while (GL43.glGetError() != GL43.GL_NO_ERROR) ;
        }
    }

    /**
     * Checks if compute shaders (OpenGL 4.3) are supported. This method <strong>must</strong> run on the main thread. Otherwise, it will crash.
     *
     * @return True if compute shaders are supported, false otherwise.
     */
    public static boolean isComputeShaderSupported() {
        return GL.getCapabilities().OpenGL43 || GL.getCapabilities().GL_ARB_compute_shader;
    }
}

