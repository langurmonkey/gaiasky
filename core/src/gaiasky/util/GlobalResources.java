/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.DistanceUnits;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import net.jafama.FastMath;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Contains resources that don't change during the runtime of the application.
 */
public class GlobalResources {
    private static final Log logger = Logger.getLogger(GlobalResources.class);
    private static final Vector3D aux = new Vector3D();
    private static final IntBuffer buf = BufferUtils.newIntBuffer(16);
    // Global all-purpose sprite batch.
    private final SpriteBatch spriteBatch;
    // Sprite batch using int indices.
    private final ExtSpriteBatch extSpriteBatch;
    private final ShaderProgram shapeShader;
    private final ShaderProgram spriteShader;
    // The UI skin.
    private Skin skin;
    /**
     * Decimal format.
     **/
    public static final DecimalFormat nf;
    /**
     * Scientific format.
     */
    public static final DecimalFormat nfSci;
    /** Textures. **/
    private final Map<String, Texture> textures;

    static {
        nf = new DecimalFormat("#########.###");
        nfSci = new DecimalFormat("0.###E0");
    }

    public GlobalResources() {
        textures = new HashMap<>();
        // Shape shader
        this.shapeShader = new ShaderProgram(Gdx.files.internal("shader/2d/shape.vertex.glsl"), Gdx.files.internal("shader/2d/shape.fragment.glsl"));
        if (!shapeShader.isCompiled()) {
            logger.info("ShapeRenderer shader compilation failed: " + shapeShader.getLog());
        }
        // Sprite shader
        this.spriteShader = new ShaderProgram(
                ShaderTemplatingLoader.load(Gdx.files.internal("shader/2d/spritebatch.vertex.glsl")),
                ShaderTemplatingLoader.load(Gdx.files.internal("shader/2d/spritebatch.accent.fragment.glsl")));
        if (!spriteShader.isCompiled()) {
            logger.info("SpriteBatch shader compilation failed: " + spriteShader.getLog());
        }
        // Sprite batch - uses screen resolution
        this.spriteBatch = new SpriteBatch(500, getSpriteShader());

        // ExtSprite shader
        var extSpriteShader = new ExtShaderProgram("spritebatch",
                                                    ShaderTemplatingLoader.load(Gdx.files.internal("shader/2d/spritebatch.vertex.glsl")),
                                                    ShaderTemplatingLoader.load(Gdx.files.internal("shader/2d/spritebatch.fragment.glsl")));
        if (!extSpriteShader.isCompiled()) {
            logger.info("ExtSpriteBatch shader compilation failed: " + extSpriteShader.getLog());
        }
        // Sprite batch
        this.extSpriteBatch = new ExtSpriteBatch(1000, extSpriteShader);

        initSkin();
    }

    public void initialize(AssetManager manager) {
        /* TEXTURES */
        TextureLoader.TextureParameter params = new TextureLoader.TextureParameter();
        params.minFilter = TextureFilter.Linear;
        params.magFilter = TextureFilter.Linear;
        manager.load("img/markers/crosshair-focus.png", Texture.class, params);
        manager.load("img/markers/crosshair-closest.png", Texture.class, params);
        manager.load("img/markers/crosshair-home.png", Texture.class, params);
        manager.load("img/markers/crosshair-arrow.png", Texture.class, params);
        manager.load("img/markers/ai-pointer.png", Texture.class, params);
        manager.load("img/markers/ai-vel.png", Texture.class, params);
        manager.load("img/markers/ai-antivel.png", Texture.class, params);
        manager.load("img/markers/gravwave-pointer.png", Texture.class, params);
        manager.load("img/markers/loc-marker-default.png", Texture.class, params);
        manager.load("img/markers/loc-marker-flag.png", Texture.class, params);
        manager.load("img/markers/loc-marker-city.png", Texture.class, params);
        manager.load("img/markers/loc-marker-town.png", Texture.class, params);
        manager.load("img/markers/loc-marker-landmark.png", Texture.class, params);
        manager.load(Settings.settings.data.dataFile(Constants.DATA_LOCATION_TOKEN + "tex/base/attitudeindicator.png"), Texture.class, params);
    }

    public void doneLoading(AssetManager manager) {

        /* Textures */
        textures.put("crosshair-focus", manager.get("img/markers/crosshair-focus.png"));
        textures.put("crosshair-closest", manager.get("img/markers/crosshair-closest.png"));
        textures.put("crosshair-home", manager.get("img/markers/crosshair-home.png"));
        textures.put("crosshair-arrow", manager.get("img/markers/crosshair-arrow.png"));
        textures.put("ai-pointer", manager.get("img/markers/ai-pointer.png"));
        textures.put("ai-vel", manager.get("img/markers/ai-vel.png"));
        textures.put("ai-antivel", manager.get("img/markers/ai-antivel.png"));
        textures.put("gravwave-pointer", manager.get("img/markers/gravwave-pointer.png"));
        textures.put("loc-marker-default", manager.get("img/markers/loc-marker-default.png"));
        textures.put("loc-marker-flag", manager.get("img/markers/loc-marker-flag.png"));
        textures.put("loc-marker-city", manager.get("img/markers/loc-marker-city.png"));
        textures.put("loc-marker-town", manager.get("img/markers/loc-marker-town.png"));
        textures.put("loc-marker-landmark", manager.get("img/markers/loc-marker-landmark.png"));
        textures.put("attitude-indicator",
                     manager.get(Settings.settings.data.dataFile(Constants.DATA_LOCATION_TOKEN + "tex/base/attitudeindicator.png"), Texture.class));

    }

    /**
     * Gets a texture from the map by name.
     *
     * @param name The texture name.
     *
     * @return The texture.
     */
    public Texture getTexture(String name) {
        return textures != null ? textures.get(name) : null;
    }

    /**
     * Formats a given double number. Uses scientific notation for numbers in [-9999,9999], and
     * regular numbers elsewhere.
     *
     * @param number The number to format.
     *
     * @return The string representation.
     */
    public static String formatNumber(double number) {
        if (number > 99999 || number < -99999) {
            return nfSci.format(number);
        } else {
            return nf.format(number);
        }
    }

    /**
     * Converts this double to the string representation of a distance.
     *
     * @param d  Distance in internal units.
     * @param du The distance units to use.
     *
     * @return An array containing the float number and the string units.
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
     * seconds).
     *
     * @param d  Velocity in internal units per second.
     * @param du The distance units to use.
     *
     * @return Array containing the number and the units.
     */
    public static Pair<Double, String> doubleToVelocityString(double d, DistanceUnits du) {
        Pair<Double, String> res = doubleToDistanceString(d, du);
        res.setSecond(res.getSecond().concat("/").concat(I18n.msg("gui.unit.second")));
        return res;
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
     * Transforms the given long array into a float array by casting each of
     * its numbers
     *
     * @param array The array of longs
     *
     * @return The array of floats
     */
    public static float[] toFloatArray(long[] array) {
        float[] res = new float[array.length];
        for (int i = 0; i < array.length; i++)
            res[i] = (float) array[i];
        return res;
    }

    /**
     * Transforms the given integer array into a float array by casting each of
     * its numbers
     *
     * @param array The array of ints
     *
     * @return The array of floats
     */
    public static float[] toFloatArray(int[] array) {
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
    public static boolean isInView(Vector3Q point, double len, float coneAngle, Vector3D dir) {
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
    public static boolean isInView(Vector3D point, double len, float coneAngle, Vector3D dir) {
        return FastMath.acos(point.dot(dir) / len) < coneAngle;
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
        if (buf == null || compareTo == null || buf.isEmpty())
            return false;
        char a, b;
        int len = FastMath.min(buf.length(), compareTo.length);
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
     */
    public static void listRecursive(Path f, final Array<Path> l, String... extensions) {
        if (Files.exists(f)) {
            if (Files.isDirectory(f)) {
                try (Stream<Path> partial = Files.list(f)) {
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
    }

    private static boolean endsWithAny(String str, String... extensions) {
        for (String ext : extensions) {
            if (str.endsWith(ext))
                return true;
        }
        return false;
    }

    /**
     * Deletes recursively all non-partial files from the path, and all partial files older than
     * {@link Constants#getPartFileMaxAgeMs()}.
     *
     * @param path the path to delete.
     *
     * @throws IOException if an I/O error is thrown when accessing the starting file.
     */
    public static void deleteRecursively(Path path) throws IOException {
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    // It is not a .part file, or it is a .part file older than 6 hours, and it is not a directory.
                    .filter(p -> (!p.toString().endsWith(".part") ||
                            (p.toString().endsWith(".part") && (TimeUtils.millis() - p.toFile().lastModified() > Constants.getPartFileMaxAgeMs())))
                            && !Files.isDirectory(p))
                    .map(Path::toFile)
                    .forEach(f -> {
                        if (!f.delete()) {
                            logger.debug(I18n.msg("error.file.delete.fail", f));
                        }
                    });
        }
    }

    public static void copyFile(Path sourceFile, Path destinationFile, boolean ow) throws IOException {
        if (!Files.exists(destinationFile) || ow)
            Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
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
        return (float) (MathUtilsDouble.radiansToDegrees * FastMath.atan2(v2.y - v1.y, v2.x - v1.x));
    }

    public static synchronized void applyRelativisticAberration(Vector3D pos, ICamera cam) {
        // Relativistic aberration
        if (Settings.settings.runtime.relativisticAberration) {
            Vector3D camDir = aux;
            if (cam.getVelocity() != null)
                camDir.set(cam.getVelocity()).nor();
            else
                camDir.set(1, 0, 0);

            double vc = cam.getSpeed() / Constants.C_KMH;
            if (vc > 0) {
                camDir.scl(-1);
                double cosThS = camDir.dot(pos) / pos.len();
                double th_s = FastMath.acos(cosThS);

                double cosThO = (cosThS - vc) / (1 - vc * cosThS);
                double th_o = FastMath.acos(cosThO);

                pos.rotate(camDir.crs(pos).nor(), FastMath.toDegrees(th_o - th_s));
            }
        }
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
        int exp = (int) (Math.log(bytes) / FastMath.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / FastMath.pow(unit, exp), pre);
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

                logger.info("skipped: " + file + " (" + exc + ")");
                // Skip folders that can't be traversed
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                if (exc != null)
                    logger.info("had trouble traversing: " + dir + " (" + exc + ")");
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

    public static String unpackAssetPath(String path, GraphicsQuality gq, String... extensions) {
        if (!path.contains(Constants.STAR_SUBSTITUTE)) {
            for (String extension : extensions) {
                String tex = path + extension;
                if (Settings.settings.data.dataFileHandle(tex).exists()) {
                    return tex;
                }
            }
        } else {
            // Try all graphics qualities.
            for (String extension : extensions) {
                for (int i = gq.ordinal(); i >= 0; i--) {
                    GraphicsQuality quality = GraphicsQuality.values()[i];
                    String suffix = quality.suffix;

                    String texSuffix = path.replace(Constants.STAR_SUBSTITUTE, suffix) + extension;
                    if (Settings.settings.data.dataFileHandle(texSuffix).exists()) {
                        return texSuffix;
                    }
                }
            }
            // Try with no suffix.
            for (String extension : extensions) {
                String texNoSuffix = path.replace(Constants.STAR_SUBSTITUTE, "") + extension;
                if (Settings.settings.data.dataFileHandle(texNoSuffix).exists()) {
                    return texNoSuffix;
                }
            }
            // Try higher qualities.
            for (String extension : extensions) {
                int n = GraphicsQuality.values().length;
                for (int i = gq.ordinal(); i < n; i++) {
                    GraphicsQuality quality = GraphicsQuality.values()[i];
                    String suffix = quality.suffix;

                    String texSuffix = path.replace(Constants.STAR_SUBSTITUTE, suffix) + extension;
                    if (Settings.settings.data.dataFileHandle(texSuffix).exists()) {
                        return texSuffix;
                    }
                }
            }
        }
        return null;
    }

    public static String unpackAssetPath(String tex) {
        return GlobalResources.unpackAssetPath(tex, Settings.settings.graphics.quality);
    }

    public static String unpackAssetPathExtensions(String tex, String... extensions) {
        return GlobalResources.unpackAssetPath(tex, Settings.settings.graphics.quality, extensions);
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

    public static synchronized String getGLExtensions() {
        Gdx.gl.glGetIntegerv(GL30.GL_NUM_EXTENSIONS, buf);
        int extensionCount = buf.get(0);
        if (extensionCount > 0) {
            String[] extensionStrings = new String[extensionCount];
            for (int i = 0; i < extensionCount; i++) {
                extensionStrings[i] = Gdx.gl30.glGetStringi(GL30.GL_EXTENSIONS, i);
            }
            return TextUtils.arrayToStr(extensionStrings);
        }
        return "";
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
        int top = (int) FastMath.pow(2, n);

        // bits holds the integer whose binary representation indicates the state for each value (on/off).
        for (int bits = 0; bits < top; bits++) {
            StringBuilder defines = new StringBuilder();
            String bin = TextUtils.padString(Integer.toBinaryString(bits), n, '0');

            // Enable defines.
            for (int bit = 0; bit < n; bit++) {
                int idx = (n - 1) - bit;
                if (bin.charAt(idx) == '1') {
                    // Enable define at bit position.
                    defines.append(values[bit]);
                }
            }

            combinations.add(defines.toString());
        }

        return combinations.toArray(String[]::new);
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

    private void initSkin() {
        var skin = new Skin();
        skin.addRegions(new TextureAtlas(Gdx.files.internal("skins/default/default.atlas")));

        // Inject fonts programmatically.
        String locale = I18n.locale.getLanguage();
        FontFactory.generateFonts(skin, locale);

        // Load the JSON (now it finds the injected fonts by name)
        skin.load(Gdx.files.internal("skins/default/default.json"));

        setSkin(skin);
    }

    public ShaderProgram getShapeShader() {
        return shapeShader;
    }

    public ShaderProgram getSpriteShader() {
        return spriteShader;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public ExtSpriteBatch getExtSpriteBatch() {
        return extSpriteBatch;
    }

    public Skin getSkin() {
        return skin;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    public void resize(int width, int height) {
        if (spriteBatch != null) {
            spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        }
    }
}
