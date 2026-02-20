/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.update.VersionChecker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Contains a datasets' description, constructed from a JSON file or object.
 */
public class DatasetDesc implements Comparable<DatasetDesc> {
    private final Log logger = Logger.getLogger(DatasetDesc.class);
    public JsonValue source;
    public String key;
    public String name;
    public String description;
    public String[] links;
    public String[] credits;
    public String[] replaces;
    public String replacedBy;
    public String creator;
    public String type;
    public String file;
    public DatasetType datasetType;
    public DatasetStatus status;
    public Path checkPath;
    public String checkStr;
    public FileHandle catalogFile;
    public String size;
    public long sizeBytes;
    public String nObjectsStr;
    public long nObjects;
    public String sha256;
    public boolean exists;
    public int myVersion = -1, serverVersion;
    public int minGsVersion = -1;
    public boolean outdated;
    public boolean baseData;
    public String[] releaseNotes;
    public String[] files;
    // In case of local datasets, this links to the server description
    public DatasetDesc server;
    private JsonReader reader;

    public DatasetDesc() {
    }

    public DatasetDesc(JsonReader reader, JsonValue source) {
        this(reader, source, null);
    }

    public DatasetDesc(JsonReader reader, JsonValue source, FileHandle localCatalogFile) {
        this.reader = reader;
        this.source = source;

        // Check if we have it.
        if (source.has("check")) {
            this.checkStr = source.getString("check");
            if (!this.checkStr.startsWith(Constants.DATA_LOCATION_TOKEN)) {
                this.checkStr = Constants.DATA_LOCATION_TOKEN + this.checkStr;
            }
            this.checkPath = Settings.settings.data.dataPath(checkStr);
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
            Path dataLocation = Path.of(Settings.settings.data.location);
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
        if (source.has("mingsversion"))
            this.minGsVersion = VersionChecker.correctVersionNumber(source.getInt("mingsversion"));

        // File
        this.file = getString("file");

        // Description
        if (source.has("description")) {
            this.description = source.getString("description");

            if (!hasKey && description.contains("-")) {
                // Old format, description=name - desc; name=key
                this.name = description.substring(0, description.indexOf("-")).trim();
                this.description = description.substring(description.indexOf("-") + 1).trim();
            }
            this.description = TextUtils.unescape(this.description);
        }

        // Release notes
        var releaseNotes = source.get("releasenotes");
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
            sizeBytes = source.getLong("size");
            size = GlobalResources.humanReadableByteCount(sizeBytes, true);
        } catch (IllegalArgumentException e) {
            sizeBytes = -1;
            size = "?";
        }

        // Number objects
        try {
            nObjects = source.getLong("nobjects");
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
            dataFiles = source.get("files");
        } else if (source.has("data") && source.get("data").isArray()) {
            dataFiles = source.get("data");
        }
        if (dataFiles != null) {
            try {
                this.files = dataFiles.asStringArray();
                for (int i = 0; i < this.files.length; i++) {
                    this.files[i] = Settings.settings.data.dataFile(this.files[i]);
                }
            } catch (Exception ignored) {
            }
        } else {
            this.files = null;
        }
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
            var c = source.get(attrName);
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
                String fname = file.getName();
                String extension = fname.substring(fname.lastIndexOf(".") + 1);
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

    public DatasetDesc getLocalCopy() {
        DatasetDesc copy = this.copy();
        copy.catalogFile = Gdx.files.absolute(copy.checkPath.toAbsolutePath().toString());
        return copy;
    }

    public DatasetDesc copy() {
        DatasetDesc copy = new DatasetDesc();
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
        copy.size = this.size;
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
        copy.server = this.server;
        copy.replaces = this.replaces;
        copy.replacedBy = this.replacedBy;
        return copy;
    }

    @Override
    public int compareTo(DatasetDesc other) {
        return this.name.compareTo(other.name);
    }

    public enum DatasetStatus {
        AVAILABLE,
        INSTALLED,
        DOWNLOADING
    }

}
