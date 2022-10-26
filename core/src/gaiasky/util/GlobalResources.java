/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.DistanceUnits;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;
import org.apfloat.Apfloat;
import org.lwjgl.opengl.GL30;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Holds and initialises resources utilised globally.
 */
public class GlobalResources {
    private static final Log logger = Logger.getLogger(GlobalResources.class);

    private AssetManager manager;
    private ShaderProgram shapeShader;
    private ShaderProgram spriteShader;

    // Global all-purpose sprite batch
    private final SpriteBatch spriteBatch;
    private SpriteBatch spriteBatchVR;

    private final ExtShaderProgram extSpriteShader;

    // Sprite batch using int indices
    private final ExtSpriteBatch extSpriteBatch;

    // Cursors
    private Cursor linkCursor;
    private Cursor resizeXCursor;
    private Cursor resizeYCursor;
    private Cursor emptyCursor;

    // The global skin
    private Skin skin;

    private static final Vector3d aux = new Vector3d();

    public GlobalResources(AssetManager manager) {
        this.manager = manager;
        // Shape shader
        this.shapeShader = new ShaderProgram(Gdx.files.internal("shader/2d/shape.vertex.glsl"), Gdx.files.internal("shader/2d/shape.fragment.glsl"));
        if (!shapeShader.isCompiled()) {
            logger.info("ShapeRenderer shader compilation failed: " + shapeShader.getLog());
        }
        // Sprite shader
        this.spriteShader = new ShaderProgram(Gdx.files.internal("shader/2d/spritebatch.vertex.glsl"), Gdx.files.internal("shader/2d/spritebatch.fragment.glsl"));
        if (!spriteShader.isCompiled()) {
            logger.info("SpriteBatch shader compilation failed: " + spriteShader.getLog());
        }
        // Sprite batch - uses screen resolution
        this.spriteBatch = new SpriteBatch(500, getSpriteShader());

        // ExtSprite shader
        this.extSpriteShader = new ExtShaderProgram(Gdx.files.internal("shader/2d/spritebatch.vertex.glsl"), Gdx.files.internal("shader/2d/spritebatch.fragment.glsl"));
        if (!extSpriteShader.isCompiled()) {
            logger.info("ExtSpriteBatch shader compilation failed: " + extSpriteShader.getLog());
        }
        // Sprite batch
        this.extSpriteBatch = new ExtSpriteBatch(1000, getExtSpriteShader());

        reloadDataFiles();
        updateSkin();
    }

    public void reloadDataFiles() {
        // Star group textures
        manager.load(Settings.settings.data.dataFile("tex/base/star.jpg"), Texture.class);
        manager.load(Settings.settings.data.dataFile("tex/base/lut.jpg"), Texture.class);
    }

    public void updateSkin() {
        initCursors();
        FileHandle fh = Gdx.files.internal("skins/" + Settings.settings.program.ui.theme + "/" + Settings.settings.program.ui.theme + ".json");
        if (!fh.exists()) {
            // Default to dark-green
            logger.info("User interface theme '" + Settings.settings.program.ui.theme + "' not found, using 'dark-green' instead");
            Settings.settings.program.ui.theme = "dark-green";
            fh = Gdx.files.internal("skins/" + Settings.settings.program.ui.theme + "/" + Settings.settings.program.ui.theme + ".json");
        }
        setSkin(new Skin(fh));
        ObjectMap<String, BitmapFont> fonts = getSkin().getAll(BitmapFont.class);
        for (String key : fonts.keys()) {
            fonts.get(key).getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        }
    }

    private void initCursors() {
        // Create skin right now, it is needed.
        if (Settings.settings.program.ui.scale > 0.8) {
            setLinkCursor(Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-link-x2.png")), 8, 0));
            setResizeXCursor(Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizex-x2.png")), 16, 16));
            setResizeYCursor(Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizey-x2.png")), 16, 16));
        } else {
            setLinkCursor(Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-link.png")), 4, 0));
            setResizeXCursor(Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizex.png")), 8, 8));
            setResizeYCursor(Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizey.png")), 8, 8));
        }
        setEmptyCursor(Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-empty.png")), 0, 5));

    }

    public static void doneLoading(AssetManager manager) {
    }

    public static Pair<Double, String> doubleToDistanceString(Apfloat d, DistanceUnits du) {
        return doubleToDistanceString(d.doubleValue(), du);
    }

    /**
     * Converts this double to the string representation of a distance
     *
     * @param d  Distance in internal units
     * @param du The distance units to use
     *
     * @return An array containing the float number and the string units
     */
    public static Pair<Double, String> doubleToDistanceString(double d, DistanceUnits du) {
        d = d * Constants.U_TO_KM;
        if (Math.abs(d) < 1f) {
            // m
            return new Pair<>((d * 1000), I18n.msg("gui.unit.m"));
        }
        if (Math.abs(d) < 0.1 * Nature.AU_TO_KM) {
            // km
            return new Pair<>(d, I18n.msg("gui.unit.km"));
        } else if (Math.abs(d) < 0.1 * du.toKm) {
            // AU
            return new Pair<>(d * Nature.KM_TO_AU, I18n.msg("gui.unit.au"));
        } else {
            // distance units
            return new Pair<>((d * du.fromKm), du.getUnitString());
        }
    }

    /**
     * Converts the double to the string representation of a velocity (always in
     * seconds)
     *
     * @param d  Distance in internal units
     * @param du The distance units to use
     *
     * @return Array containing the number and the units
     */
    public static Pair<Double, String> doubleToVelocityString(double d, DistanceUnits du) {
        Pair<Double, String> res = doubleToDistanceString(d, du);
        res.setSecond(res.getSecond().concat("/s"));
        return res;
    }

    /**
     * Converts this float to the string representation of a distance
     *
     * @param f  Distance in internal units
     * @param du The distance units to use
     *
     * @return An array containing the float number and the string units
     */
    public static Pair<Float, String> floatToDistanceString(float f, DistanceUnits du) {
        Pair<Double, String> result = doubleToDistanceString(f, du);
        return new Pair<>(result.getFirst().floatValue(), result.getSecond());
    }

    /**
     * Transforms the given double array into a float array by casting each of
     * its numbers
     *
     * @param array The array of doubles
     *
     * @return The array of floats
     */
    public static float[] toFloatArray(double[] array) {
        float[] res = new float[array.length];
        for (int i = 0; i < array.length; i++)
            res[i] = (float) array[i];
        return res;
    }

    /**
     * Computes whether a body with the given position is visible by a camera
     * with the given direction and angle. Coordinates are assumed to be in the
     * camera-origin system
     *
     * @param point     The position of the body in the reference system of the camera
     *                  (i.e. camera is at origin)
     * @param len       The point length
     * @param coneAngle The cone angle of the camera
     * @param dir       The direction
     *
     * @return True if the body is visible
     */
    public static boolean isInView(Vector3b point, double len, float coneAngle, Vector3d dir) {
        return FastMath.acos(point.tov3d().dot(dir) / len) < coneAngle;
    }

    /**
     * Computes whether a body with the given position is visible by a camera
     * with the given direction and angle. Coordinates are assumed to be in the
     * camera-origin system
     *
     * @param point     The position of the body in the reference system of the camera
     *                  (i.e. camera is at origin)
     * @param len       The point length
     * @param coneAngle The cone angle of the camera
     * @param dir       The direction
     *
     * @return True if the body is visible
     */
    public static boolean isInView(Vector3d point, double len, float coneAngle, Vector3d dir) {
        return FastMath.acos(point.dot(dir) / len) < coneAngle;
    }

    /**
     * Computes whether any of the given points is visible by a camera with the
     * given direction and the given cone angle. Coordinates are assumed to be
     * in the camera-origin system
     *
     * @param points    The array of points to check
     * @param coneAngle The cone angle of the camera (field of view)
     * @param dir       The direction
     *
     * @return True if any of the points is in the camera view cone
     */
    public static boolean isAnyInView(Vector3d[] points, float coneAngle, Vector3d dir) {
        boolean inView = false;
        int size = points.length;
        for (Vector3d point : points) {
            inView = inView || FastMath.acos(point.dot(dir) / point.len()) < coneAngle;
        }
        return inView;
    }

    /**
     * Compares a given buffer with another buffer.
     *
     * @param buf       Buffer to compare against
     * @param compareTo Buffer to compare to (content should be ASCII lowercase if
     *                  possible)
     *
     * @return True if the buffers compare favourably, false otherwise
     */
    public static boolean equal(String buf, char[] compareTo, boolean ignoreCase) {
        if (buf == null || compareTo == null || buf.length() == 0)
            return false;
        char a, b;
        int len = Math.min(buf.length(), compareTo.length);
        if (ignoreCase) {
            for (int i = 0; i < len; i++) {
                a = buf.charAt(i);
                b = compareTo[i];
                if (a == b || (a - 32) == b)
                    continue; // test a == a or A == a;
                return false;
            }
        } else {
            for (int i = 0; i < len; i++) {
                a = buf.charAt(i);
                b = compareTo[i];
                if (a == b)
                    continue; // test a == a
                return false;
            }
        }
        return true;
    }

    public static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets all the files with the given extension in the given path f.
     *
     * @param f          The directory to get all the files
     * @param l          The list with the results
     * @param extensions The allowed extensions
     *
     * @return The list l
     */
    public static Array<Path> listRecursive(Path f, final Array<Path> l, String... extensions) {
        if (Files.exists(f)) {
            if (Files.isDirectory(f)) {
                try {
                    Stream<Path> partial = Files.list(f);
                    partial.forEachOrdered(p -> listRecursive(p, l, extensions));
                } catch (IOException e) {
                    logger.error(e);
                }

            } else {
                if (endsWithAny(f.getFileName().toString(), extensions)) {
                    l.add(f);
                }
            }
        }

        return l;
    }

    private static boolean endsWithAny(String str, String... extensions) {
        for (String ext : extensions) {
            if (str.endsWith(ext))
                return true;
        }
        return false;
    }

    /**
     * Deletes recursively all non-partial files from the path.
     *
     * @param path the path to delete.
     *
     * @throws IOException if an I/O error is thrown when accessing the starting file.
     */
    public static void deleteRecursively(Path path) throws IOException {
        Files.walk(path).sorted(Comparator.reverseOrder()).filter(p -> !p.toString().endsWith(".part") && !Files.isDirectory(p)).map(Path::toFile).forEach(java.io.File::delete);
    }

    public static void copyFile(Path sourceFile, Path destFile, boolean ow) throws IOException {
        if (!Files.exists(destFile) || ow)
            Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public static Array<Path> listRecursive(Path f, final Array<Path> l, DirectoryStream.Filter<Path> filter) {
        if (Files.exists(f)) {
            if (Files.isDirectory(f)) {
                try {
                    Stream<Path> partial = Files.list(f);
                    partial.forEachOrdered(p -> listRecursive(p, l, filter));
                } catch (IOException e) {
                    logger.error(e);
                }

            } else {
                try {
                    if (filter.accept(f))
                        l.add(f);
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
        return l;
    }

    /**
     * Recursively count files in a directory
     *
     * @param dir The directory
     *
     * @return The number of files
     *
     * @throws IOException if an I/O error is thrown when accessing the starting file.
     */
    public static long fileCount(Path dir) throws IOException {
        return fileCount(dir, null);
    }

    /**
     * Count files matching a certain ending in a directory, recursively
     *
     * @param dir The directory
     *
     * @return The number of files
     *
     * @throws IOException if an I/O error is thrown when accessing the starting file.
     */
    public static long fileCount(Path dir, String[] extensions) throws IOException {
        return Files.walk(dir).parallel().filter(p -> (!p.toFile().isDirectory() && endsWith(p.toFile().getName(), extensions))).count();
    }

    /**
     * Returns true if the string ends with any of the endings
     *
     * @param s       The string
     * @param endings The endings
     *
     * @return True if the string ends with any of the endings
     */
    public static boolean endsWith(String s, String[] endings) {
        if (endings == null) {
            return true;
        }
        for (String ending : endings) {
            if (s.endsWith(ending))
                return true;
        }
        return false;
    }

    public static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** Gets the angle in degrees between the two vectors **/
    public static float angle2d(Vector3 v1, Vector3 v2) {
        return (float) (MathUtilsd.radiansToDegrees * FastMath.atan2(v2.y - v1.y, v2.x - v1.x));
    }

    public static synchronized Vector3d applyRelativisticAberration(Vector3d pos, ICamera cam) {
        // Relativistic aberration
        if (Settings.settings.runtime.relativisticAberration) {
            Vector3d cdir = aux;
            if (cam.getVelocity() != null)
                cdir.set(cam.getVelocity()).nor();
            else
                cdir.set(1, 0, 0);

            double vc = cam.getSpeed() / Constants.C_KMH;
            if (vc > 0) {
                cdir.scl(-1);
                double cosThS = cdir.dot(pos) / pos.len();
                double th_s = Math.acos(cosThS);

                double cosThO = (cosThS - vc) / (1 - vc * cosThS);
                double th_o = Math.acos(cosThO);

                pos.rotate(cdir.crs(pos).nor(), Math.toDegrees(th_o - th_s));
            }
        }
        return pos;
    }

    /**
     * Converts bytes to a human-readable format
     *
     * @param bytes The bytes
     * @param si    Whether to use SI units (1000-multiples) or binary (1024-multiples)
     *
     * @return The size in a human-readable form
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private static String generateMD5(FileInputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            FileChannel channel = inputStream.getChannel();
            ByteBuffer buff = ByteBuffer.allocate(2048);
            while (channel.read(buff) != -1) {
                buff.flip();
                md.update(buff);
                buff.clear();
            }
            byte[] hashValue = md.digest();
            return new String(hashValue);
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error(e);
            return null;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    /**
     * Attempts to calculate the size of a file or directory.
     *
     * <p>
     * Since the operation is non-atomic, the returned value may be inaccurate.
     * However, this method is quick and does its best.
     */
    public static long size(Path path) throws IOException {

        final AtomicLong size = new AtomicLong(0);

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {

                System.out.println("skipped: " + file + " (" + exc + ")");
                // Skip folders that can't be traversed
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                if (exc != null)
                    System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                // Ignore errors traversing a folder
                return FileVisitResult.CONTINUE;
            }
        });

        return size.get();
    }

    /**
     * Parses the string and creates a string array. The string is a list of whitespace-separated
     * tokens, each surrounded by double quotes '"':
     * str = '"a" "bc" "d" "efghi"'
     *
     * @param str The string
     *
     * @return The resulting array
     */
    public static String[] parseWhitespaceSeparatedList(String str) {
        if (str == null || str.isEmpty())
            return null;

        List<String> l = new ArrayList<>();
        int n = str.length();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < n; i++) {
            char c = str.charAt(i);
            if (c == '"') {
                if (inString) {
                    l.add(current.toString());
                    current = new StringBuilder();
                    inString = false;
                } else {
                    inString = true;
                }
            } else {
                if (inString)
                    current.append(c);
            }
        }
        return l.toArray(new String[0]);
    }

    /**
     * Converts the string array into a whitespace-separated string
     * where each element is double-quoted.
     *
     * @param l The string array
     *
     * @return The resulting string
     */
    public static String toWhitespaceSeparatedList(String[] l) {
        return toString(l, "\"", " ");
    }

    /**
     * Converts a string array into a string, optionally quoting each entry and with
     * a given separator.
     *
     * @param l         The list
     * @param quote     The quote string to use
     * @param separator The separator
     *
     * @return The resulting string
     */
    public static String toString(String[] l, String quote, String separator) {
        if (l == null || l.length == 0)
            return null;

        if (quote == null)
            quote = "";
        if (separator == null)
            separator = "";

        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            sb.append(quote).append(s).append(quote).append(separator);
        }
        return sb.toString().trim();

    }

    public static String unpackAssetPath(String path, GraphicsQuality gq) {
        if (path.contains(Constants.STAR_SUBSTITUTE)) {
            // Start with current quality and scan to lower ones
            for (int i = gq.ordinal(); i >= 0; i--) {
                GraphicsQuality quality = GraphicsQuality.values()[i];
                String suffix = quality.suffix;

                String texSuffix = path.replace(Constants.STAR_SUBSTITUTE, suffix);
                if (Settings.settings.data.dataFileHandle(texSuffix).exists()) {
                    return texSuffix;
                }
            }
            // Try with no suffix
            String texNoSuffix = path.replace(Constants.STAR_SUBSTITUTE, "");
            if (Settings.settings.data.dataFileHandle(texNoSuffix).exists()) {
                return texNoSuffix;
            }
            // Try higher qualities
            int n = GraphicsQuality.values().length;
            for (int i = gq.ordinal(); i < n; i++) {
                GraphicsQuality quality = GraphicsQuality.values()[i];
                String suffix = quality.suffix;

                String texSuffix = path.replace(Constants.STAR_SUBSTITUTE, suffix);
                if (Settings.settings.data.dataFileHandle(texSuffix).exists()) {
                    return texSuffix;
                }
            }
            logger.error("Texture not found: " + path);
            return null;
        } else {
            return path;
        }
    }

    public static String unpackAssetPath(String tex) {
        return GlobalResources.unpackAssetPath(tex, Settings.settings.graphics.quality);
    }

    public static String resolveCubemapSide(String baseLocation, String... sideSuffixes) throws RuntimeException {
        FileHandle loc = Gdx.files.absolute(baseLocation);
        FileHandle[] files = loc.list();
        for (FileHandle file : files) {
            for (String suffix : sideSuffixes) {
                if (file.name().contains("_" + suffix + ".")) {
                    // Found!
                    return file.file().getAbsolutePath().replaceAll("\\\\", "/");
                }
            }
        }
        throw new RuntimeException("Cubemap side '" + TextUtils.arrayToStr(sideSuffixes) + "' not found in folder: " + baseLocation);
    }

    private static final IntBuffer buf = BufferUtils.newIntBuffer(16);

    public static synchronized String getGLExtensions() {
        String extensions = Gdx.gl.glGetString(GL30.GL_EXTENSIONS);
        if (extensions == null || extensions.isEmpty()) {
            Gdx.gl.glGetIntegerv(GL30.GL_NUM_EXTENSIONS, buf);
            int next = buf.get(0);
            String[] extensionsstr = new String[next];
            for (int i = 0; i < next; i++) {
                extensionsstr[i] = Gdx.gl30.glGetStringi(GL30.GL_EXTENSIONS, i);
            }
            extensions = TextUtils.arrayToStr(extensionsstr);
        } else {
            extensions = extensions.replaceAll(" ", "\n");
        }
        return extensions;
    }

    /**
     * Generates all combinations of all sizes of all the strings given in values.
     *
     * @param values The input strings to combine.
     *
     * @return The resulting combinations.
     */
    public static String[] combinations(String[] values) {
        Array<String> combinations = new Array<>();
        int n = values.length;
        int top = (int) Math.pow(2, n);

        // bits holds the integer whose binary representation indicates the state for each value (on/off).
        for (int bits = 0; bits < top; bits++) {
            StringBuilder defines = new StringBuilder();
            String bin = TextUtils.padString(Integer.toBinaryString(bits), n, '0');

            // Enable defines.
            for (int bit = 0; bit < n; bit++) {
                int idx = (n - 1) - bit;
                if(bin.charAt(idx) == '1') {
                    // Enable define at bit position.
                    defines.append(values[bit]);
                }
            }

            combinations.add(defines.toString());
        }

        return combinations.toArray(String.class);
    }

    /**
     * Generates all combinations of the given size using the elements in values.
     *
     * @param values The elements to combine
     * @param size   The size of the combinations
     * @param <T>    The type
     *
     * @return The combinations
     */
    public static <T> List<List<T>> combination(List<T> values, int size) {

        if (0 == size) {
            return Collections.singletonList(Collections.emptyList());
        }

        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> combination = new LinkedList<>();

        T actual = values.iterator().next();

        List<T> subSet = new LinkedList<T>(values);
        subSet.remove(actual);

        List<List<T>> subSetCombination = combination(subSet, size - 1);

        for (List<T> set : subSetCombination) {
            List<T> newSet = new LinkedList<T>(set);
            newSet.add(0, actual);
            combination.add(newSet);
        }

        combination.addAll(combination(subSet, size));

        return combination;
    }

    public static String nObjectsToString(long objects) {
        if (objects > 1e18) {
            return String.format("%1$.1f %2$s", objects / 1.0e18, I18n.msg("gui.unit.exa"));
        } else if (objects > 1e15) {
            return String.format("%1$.1f %2$s", objects / 1.0e15, I18n.msg("gui.unit.peta"));
        } else if (objects > 1e12) {
            return String.format("%1$.1f %2$s", objects / 1.0e12, I18n.msg("gui.unit.tera"));
        } else if (objects > 1e9) {
            return String.format("%1$.1f %2$s", objects / 1.0e9, I18n.msg("gui.unit.giga"));
        } else if (objects > 1e6) {
            return String.format("%1$.1f %2$s", objects / 1.0e6, I18n.msg("gui.unit.mega"));
        } else if (objects > 1e3) {
            return String.format("%1$.1f %2$s", objects / 1.0e3, I18n.msg("gui.unit.kilo"));
        } else if (objects > 1e2) {
            return String.format("%1$.1f %2$s", objects / 1.0e2, I18n.msg("gui.unit.hecto"));
        } else if (objects > 1e1) {
            return String.format("%1$.1f %2$s", objects / 1.0e1, I18n.msg("gui.unit.deca"));
        } else {
            return objects + "";
        }
    }

    public static String msToTimeString(long ms) {
        double seconds = ms / 1000d;
        double minutes = seconds / 60d;
        double hours = minutes / 60d;
        double days = hours / 24d;
        double years = days / AstroUtils.JD_TO_Y;
        if (seconds < 60) {
            return String.format("%1$.0f %2$s", seconds, I18n.msg("gui.unit.second"));
        } else if (minutes < 60) {
            return String.format("%1$.0f %2$s", minutes, I18n.msg("gui.unit.minute"));
        } else if (hours < 24) {
            return String.format("%1$.0f %2$s", hours, I18n.msg("gui.unit.hour"));
        } else if (days < AstroUtils.JD_TO_Y) {
            return String.format("%1$.0f %2$s", days, I18n.msg("gui.unit.day"));
        } else {
            return String.format("%1$.0f %2$s", years, I18n.msg("gui.unit.year"));
        }

    }

    public ShaderProgram getShapeShader() {
        return shapeShader;
    }

    public void setShapeShader(ShaderProgram shapeShader) {
        this.shapeShader = shapeShader;
    }

    public ShaderProgram getSpriteShader() {
        return spriteShader;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public SpriteBatch getSpriteBatchVR() {
        return spriteBatchVR;
    }

    public void setSpriteBatchVR(SpriteBatch sb) {
        spriteBatchVR = sb;
    }

    public ExtShaderProgram getExtSpriteShader() {
        return extSpriteShader;
    }

    public ExtSpriteBatch getExtSpriteBatch() {
        return extSpriteBatch;
    }

    public Cursor getLinkCursor() {
        return linkCursor;
    }

    public void setLinkCursor(Cursor linkCursor) {
        this.linkCursor = linkCursor;
    }

    public Cursor getResizeXCursor() {
        return resizeXCursor;
    }

    public void setResizeXCursor(Cursor resizeXCursor) {
        this.resizeXCursor = resizeXCursor;
    }

    public Cursor getResizeYCursor() {
        return resizeYCursor;
    }

    public void setResizeYCursor(Cursor resizeYCursor) {
        this.resizeYCursor = resizeYCursor;
    }

    public Cursor getEmptyCursor() {
        return emptyCursor;
    }

    public void setEmptyCursor(Cursor emptyCursor) {
        this.emptyCursor = emptyCursor;
    }

    public Skin getSkin() {
        return skin;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }
}
