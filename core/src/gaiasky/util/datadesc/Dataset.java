/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.GaiaSky;
import gaiasky.data.util.GlobalResources;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.update.VersionChecker;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/**
 * Contains the metadata of a single dataset, constructed from a JSON file or object.
 */
public class Dataset implements Comparable<Dataset> {
    private static final Log logger = Logger.getLogger(Dataset.class);

    public static boolean verifyDatasetImage(@NotNull Texture tex) {
        final int MAX_IMAGE_SIDE = 800;
        final float MAX_AR_DELTA = 0.1f;
        float ar = (float) tex.getWidth() / (float) tex.getHeight();
        return Math.abs(ar - 1.0) < MAX_AR_DELTA && tex.getWidth() <= MAX_IMAGE_SIDE;
    }

    /** Dataset key. It is a kebab-case string without whitespaces. **/
    public String key;
    /** Common name of the dataset. **/
    public String name;
    /** Text description of the dataset. **/
    public String description;
    /** Link(s) to get more information about this dataset. **/
    public String[] links;
    /** Credit(s) for this dataset. These usually point to the data sources. **/
    public String[] credits;
    /** Keys of the datasets that this dataset replaces, if any. **/
    public String[] replaces;
    /** Contains the key of the dataset that replaces this one, if any. If this is non-null, the current dataset is obsolete. **/
    public String replacedBy;
    /**
     * The 'feature' that this dataset provides. For instance, all star catalogs would provide 'starfield'.
     * This is useful to detect possible errors in the case of several datasets providing the same
     * feature being enabled.
     **/
    public String provides;
    /** Creator of the dataset. Usually "Name - email" is sufficient. **/
    public String creator;
    /** URL (possibly with @mirror-url@ wildcard) of the gzipped dataset tarball. **/
    public String file;
    /** Dataset type. **/
    public DatasetType datasetType;
    /** Dataset type string. **/
    public String type;
    /** Dataset status. **/
    public DatasetStatus status;
    /** Path of the <code>dataset.json</code> file for this dataset. **/
    public Path checkPath;
    /** Path of the <code>dataset.json</code> file, as a string. **/
    public String checkStr;
    /** File handle to the <code>dataset.json</code> file. **/
    public FileHandle catalogFile;
    /** Dataset pretty size, as a string. **/
    public String sizeString;
    /**
     * Dataset raw size in bytes. This is the package file ({@link #file} size in server datasets,
     * and the uncompressed disk size in installed datasets.
     **/
    public long sizeBytes;
    /** Number of objects, as a pretty string (i.e. 3B). **/
    public String nObjectsStr;
    /** Raw number of objects. **/
    public long nObjects;
    /** SHA256 checksum. **/
    public String sha256;
    /** Flag that signals whether the dataset is in the local file system. **/
    public boolean exists;
    /** Local dataset version. **/
    public int myVersion = -1;
    /** Server dataset version. **/
    public int serverVersion;
    /** Minimum Gaia Sky version needed for this dataset. **/
    public int minGsVersion = -1;
    /** Flag that marks the dataset as outdated: {@link #replacedBy} is not empty. **/
    public boolean outdated;
    /** Flag set to true in the base data pack. **/
    public boolean baseData;
    /** List of release notes for this version. **/
    public String[] releaseNotes;
    /** List of files or directories included in this dataset. **/
    public String[] files;
    /** URLs to dataset images. These are displayed in the dataset manager. **/
    public String[] images;
    /** In case of local datasets, this links to the server description. **/
    public Dataset server;

    /** Source JSON object. **/
    private JsonValue source;
    /** JSON reader. **/
    private JsonReader reader;

    public Dataset() {
    }

    public Dataset(JsonReader reader, JsonValue source) {
        this(reader, source, null);
    }

    public Dataset(JsonReader reader, JsonValue source, FileHandle localCatalogFile) {
        this.reader = reader;
        this.source = source;

        // Check if we have it.
        if (source.has("check")) {
            this.checkStr = getString("check");
            if (this.checkStr != null
                    && !this.checkStr.startsWith(Constants.DATA_LOCATION_TOKEN)) {
                this.checkStr = Constants.DATA_LOCATION_TOKEN + this.checkStr;
            }
            this.checkPath = GaiaSky.settings().data.dataPath(checkStr);
            this.exists = Files.exists(checkPath) && Files.isReadable(checkPath);
            this.serverVersion = source.getInt("version", 0);
            if (this.exists) {
                this.myVersion = checkJsonVersion(checkPath);
                this.outdated = serverVersion > myVersion;
            } else {
                this.outdated = false;
            }
        } else if (localCatalogFile != null) {
            this.checkPath = localCatalogFile.file().toPath();
            Path dataLocation = Path.of(GaiaSky.settings().data.location);
            String relative = dataLocation.toUri().relativize(this.checkPath.toUri()).getPath();
            this.checkStr = Constants.DATA_LOCATION_TOKEN + relative;
            this.exists = localCatalogFile.exists();
        }

        this.status = exists ? DatasetStatus.INSTALLED : DatasetStatus.AVAILABLE;

        // The key marks the new version
        this.key = getString("key");
        boolean hasKey = this.key != null;
        hasKey = hasKey || localCatalogFile != null;
        this.name = getString("name");

        // Fill key with name
        if (this.key == null && this.name != null) {
            this.key = this.name.replaceAll("\\s+", "-");
        }
        if (this.key != null)
            this.baseData = key.equals(Constants.DEFAULT_DATASET_KEY);

        // Version
        if (source.has("version") && this.myVersion == -1)
            this.myVersion = source.getInt("version");

        // Mingsversion
        var minGsVersion = getInt("mingsversion", "minGsVersion", "minGaiaSkyVersion");
        if (minGsVersion > 0)
            this.minGsVersion = VersionChecker.correctVersionNumber(minGsVersion);

        // File
        this.file = getString("file");

        // Description
        var description = getString("description", "desc");
        if (description != null) {
            this.description = description;

            if (!hasKey && this.description.contains("-")) {
                // Old format, description=name - desc; name=key
                this.name = this.description.substring(0, this.description.indexOf("-")).trim();
                this.description = this.description.substring(this.description.indexOf("-") + 1).trim();
            }
            this.description = TextUtils.unescape(this.description);
        }

        // Release notes
        var releaseNotes = get("releasenotes", "releaseNotes");
        if (releaseNotes != null) {
            if (releaseNotes.isString()) {
                this.releaseNotes = releaseNotes.asString().split("\\r?\\n");
                // Remove possible leading list items, for compatibility.
                for (int i = 0; i < this.releaseNotes.length; i++) {
                    if (this.releaseNotes[i].startsWith("- ") || this.releaseNotes[i].startsWith("* ")) {
                        this.releaseNotes[i] = this.releaseNotes[i].substring(2);
                    }
                }
            } else if (releaseNotes.isArray()) {
                this.releaseNotes = releaseNotes.asStringArray();
            }
        } else {
            this.releaseNotes = null;
        }

        // Links
        this.links = getStringOrArray("links", "link");

        // Creator
        this.creator = getString("creator", "author");

        // Credits
        this.credits = getStringOrArray("credits", "credit");

        // Type
        this.type = getStringOr("type", "other");

        // Size
        try {
            sizeBytes = getLong("size", "sizeBytes");
            sizeString = GlobalResources.humanReadableByteCount(sizeBytes, true);
        } catch (IllegalArgumentException e) {
            sizeBytes = -1;
            sizeString = "?";
        }

        // Number objects
        try {
            nObjects = getLong("nobjects", "nObjects", "numObjects");
            nObjectsStr = I18n.msg("gui.dataset.nobjects", GlobalResources.nObjectsToString(nObjects));
        } catch (IllegalArgumentException e) {
            nObjects = -1;
            nObjectsStr = "N/A";
        }

        // Replaces
        this.replaces = getStringOrArray("replaces");

        // Replaced by
        this.replacedBy = getString("replacedby", "replacedBy");

        // Digest
        this.sha256 = getString("sha256");

        // Data
        JsonValue dataFiles = null;
        if (source.has("files")) {
            dataFiles = get("files");
        } else if (source.has("data") && get("data").isArray()) {
            dataFiles = get("data");
        }
        if (dataFiles != null) {
            try {
                this.files = dataFiles.asStringArray();
                for (int i = 0; i < this.files.length; i++) {
                    this.files[i] = GaiaSky.settings().data.dataFile(this.files[i]);
                }
            } catch (Exception ignored) {
            }
        } else {
            this.files = null;
        }

        if (exists) {
            // Discover local images.
            this.images = discoverImages();
        } else {
            // Remote images.
            this.images = getStringOrArray("images");
        }
    }

    /**
     * Discovers image files in the dataset directory. Looks for files matching:
     * <ul>
     *     <li>image.jpg, image.png</li>
     *     <li>image00.jpg, image01.jpg, ..., image99.jpg (and .png variants)</li>
     * </ul>
     * <p>
     * Images are returned as absolute file paths.
     *
     * @return An array of image file paths, or null if none found.
     */
    private String[] discoverImages() {
        if (checkPath == null) {
            return null;
        }
        // Get parent to `dataset.json` file, this is the base location.
        var basePath = checkPath.getParent();

        if (!Files.isDirectory(basePath)) {
            return null;
        }

        java.util.List<String> discoveredImages = new java.util.ArrayList<>();

        try {
            File dir = basePath.toFile();
            File[] files = dir.listFiles();

            if (files == null) {
                return null;
            }

            // Helper to normalize extensions for comparison.
            var supportedExtensions = new HashSet<>(Arrays.asList("jpg", "png"));

            // Discover image.jpg, image.png
            for (File file : files) {
                String fileName = file.getName().toLowerCase(Locale.ROOT);
                if (fileName.startsWith("image.")) {
                    String ext = getExtension(fileName);
                    if (supportedExtensions.contains(ext) && file.isFile()) {
                        discoveredImages.add(file.getAbsolutePath());
                    }
                }
            }

            // Discover imageXX.jpg, imageXX.png where XX is [00,99]
            for (File file : files) {
                String fileName = file.getName().toLowerCase(Locale.ROOT);
                if (fileName.matches("image\\d{2}\\.(jpg|png)")) {
                    if (file.isFile()) {
                        discoveredImages.add(file.getAbsolutePath());
                    }
                }
            }

            // Sort for consistent ordering
            discoveredImages.sort(String::compareTo);

            return discoveredImages.isEmpty() ? null : discoveredImages.toArray(new String[0]);

        } catch (Exception e) {
            logger.warn("Error discovering images in dataset directory: " + basePath, e);
            return null;
        }
    }

    /**
     * Extracts the file extension from a filename (lowercase).
     *
     * @param fileName The filename.
     *
     * @return The extension without the dot, or empty string if no extension.
     */
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    /**
     * Gets the JSON value attribute with one of the given names from the current source JSON value object.
     *
     * @param names The possible names of the attribute. The method does a linear search until one of them is present.
     *
     * @return The JSON value, or null if no attributes with the given names exist.
     */
    private JsonValue get(String... names) {
        if (names == null) {
            return null;
        }
        return Arrays.stream(names)
                .filter(source::has)
                .map(source::get)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the attribute with one of the given names from the current source JSON value object as an integer.
     *
     * @param names The possible names of the attribute. The method does a linear search until one of them is present.
     *
     * @return The integer attribute, or -1 if none of the names exist.
     */
    private int getInt(String... names) {
        if (names == null) {
            return -1;
        }
        return Arrays.stream(names)
                .filter(source::has)
                .map(source::getInt)
                .findFirst()
                .orElse(-1);
    }

    /**
     * Gets the attribute with one of the given names from the current source JSON value object as a long integer.
     *
     * @param names The possible names of the attribute. The method does a linear search until one of them is present.
     *
     * @return The long integer attribute, or -1 if none of the names exist.
     */
    private long getLong(String... names) {
        if (names == null) {
            return -1L;
        }
        return Arrays.stream(names)
                .filter(source::has)
                .map(source::getLong)
                .findFirst()
                .orElse(-1L);
    }

    @Override
    public String toString() {
        return key != null ? key : name;
    }

    private String getString(String... attrNames) {
        for (var name : attrNames) {
            var result = getString(name);
            if (result != null && !result.isBlank()) {
                return result;
            }
        }
        return null;

    }

    private String getString(String attrName) {
        if (source.has(attrName)) {
            var c = get(attrName);
            if (c.isString()) {
                return c.asString();
            } else {
                logger.warn(String.format("Attribute '%s' must be a String.", attrName));
            }
        }
        return null;
    }

    private String getStringOr(String attrName, String defaultValue) {
        if (source.has(attrName)) {
            var c = source.get(attrName);
            if (c.isString()) {
                return c.asString();
            } else {
                logger.warn(String.format("Attribute '%s' must be a String.", attrName));
            }
        }
        return defaultValue;
    }

    private String[] getStringOrArray(String... attrNames) {
        for (var name : attrNames) {
            var result = getStringOrArray(name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private String[] getStringOrArray(String attrName) {
        if (source.has(attrName)) {
            var c = source.get(attrName);
            if (c.isArray()) {
                return c.asStringArray();
            } else if (c.isString()) {
                return new String[]{c.asString()};
            } else {
                logger.warn(String.format("Attribute '%s' must be a String or String[].", attrName));
            }
        } else {
            this.credits = null;
        }
        return null;
    }

    /**
     * Checks if the current dataset is replaced by the dataset with the given key.
     *
     * @param key The key.
     *
     * @return True if the current dataset is replaced by the dataset with the given key.
     */
    public boolean isReplacedBy(String key) {
        return replacedBy != null && replacedBy.equals(key);
    }

    /**
     * Checks whether the current dataset replaces the dataset with the given key.
     *
     * @param key The key.
     *
     * @return True if the current dataset replaces the dataset with the given key.
     */
    public boolean replaces(String key) {
        if (replaces != null) {
            for (var k : replaces) {
                if (k.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds the key to the {@link #replaces} array of this dataset.
     *
     * @param key The key to add.
     */
    public void addReplacesEntry(String key) {
        // Add key only if it is not yet there.
        if (!replaces(key)) {
            replaces = ArrayUtils.addString(replaces, key);
        }
    }

    /**
     * Checks the version file of the given path, if it is a correct JSON
     * file and contains a top-level "version" attribute. Otherwise, it
     * returns the default the lowest version (0)
     *
     * @param path The path with the file to check
     *
     * @return The version, if it exists, or 0
     */
    private int checkJsonVersion(Path path) throws RuntimeException {
        if (path != null) {
            File file = path.toFile();
            if (file.exists() && file.canRead() && file.isFile()) {
                String fileName = file.getName();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                if (extension.equalsIgnoreCase("json")) {
                    JsonValue jf = reader.parse(Gdx.files.absolute(file.getAbsolutePath()));
                    if (jf.has("version")) {
                        try {
                            return jf.getInt("version", 0);
                        } catch (Exception e) {
                            logger.error(e, "The 'version' attribute must be an integer: " + path);
                        }
                    }
                }
            }
            return 0;
        } else {
            throw new RuntimeException("Path is null");
        }
    }

    /**
     * Filters the dataset using a given text. It checks the filter against
     * name, description, key and type.
     *
     * @param filterText The filter text.
     *
     * @return True if the filter passes, false otherwise.
     */
    public boolean filter(String filterText) {
        if (filterText != null) {
            filterText = filterText.toLowerCase(Locale.ROOT);
            return filterText.isBlank()
                    || (this.name != null && this.name.toLowerCase(Locale.ROOT).contains(filterText))
                    || (this.description != null && this.description.toLowerCase(Locale.ROOT).contains(filterText))
                    || (this.key != null && this.key.toLowerCase(Locale.ROOT).contains(filterText))
                    || (this.type != null && this.type.toLowerCase(Locale.ROOT).contains(filterText));
        } else {
            return true;
        }
    }

    public boolean isStarDataset() {
        return this.type != null && (this.type.equals("catalog-lod") || this.type.equals("catalog-gaia") || this.type.equals("catalog-star"));
    }

    public Dataset getLocalCopy() {
        Dataset copy = this.copy();
        copy.catalogFile = Gdx.files.absolute(copy.checkPath.toAbsolutePath().toString());
        return copy;
    }

    public Dataset copy() {
        Dataset copy = new Dataset();
        copy.reader = this.reader;
        copy.source = this.source;
        copy.key = this.key;
        copy.name = this.name;
        copy.description = this.description;
        copy.links = this.links;
        copy.creator = this.creator;
        copy.credits = this.credits;
        copy.type = this.type;
        copy.file = this.file;
        copy.datasetType = this.datasetType;
        copy.status = this.status;
        copy.checkStr = this.checkStr;
        copy.checkPath = this.checkPath;
        copy.catalogFile = this.catalogFile;
        copy.sizeString = this.sizeString;
        copy.sizeBytes = this.sizeBytes;
        copy.nObjectsStr = this.nObjectsStr;
        copy.nObjects = this.nObjects;
        copy.sha256 = this.sha256;
        copy.exists = this.exists;
        copy.myVersion = this.myVersion;
        copy.serverVersion = this.serverVersion;
        copy.minGsVersion = this.minGsVersion;
        copy.outdated = this.outdated;
        copy.baseData = this.baseData;
        copy.releaseNotes = this.releaseNotes;
        copy.files = this.files;
        copy.images = this.images;
        copy.server = this.server;
        copy.replaces = this.replaces;
        copy.replacedBy = this.replacedBy;
        copy.provides = this.provides;
        return copy;
    }

    @Override
    public int compareTo(Dataset other) {
        return this.name.compareTo(other.name);
    }

    public enum DatasetStatus {
        AVAILABLE,
        INSTALLED,
        DOWNLOADING
    }

}