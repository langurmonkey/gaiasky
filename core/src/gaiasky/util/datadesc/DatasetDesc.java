/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatasetDesc {
    private final Log logger = Logger.getLogger(DatasetDesc.class);

    private JsonReader reader;
    public JsonValue source;
    public String key;
    public String name;
    public String description;
    public String shortDescription;
    public String link;
    public String type;
    public String file;
    public DatasetType datasetType;
    public DatasetStatus status;

    public Path check;
    public Path path;
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

    public String releaseNotes;

    public String[] filesToDelete;

    // In case of local datasets, this links to the server description
    public DatasetDesc server;

    public DatasetDesc(){}

    public DatasetDesc(JsonReader reader, JsonValue source) {
        this(reader, source, false);
    }

    public DatasetDesc(JsonReader reader, JsonValue source, boolean baseData) {
        this.reader = reader;
        this.source = source;

        // Check if we have it
        if (source.has("check")) {
            this.check = Paths.get(Settings.settings.data.location, source.getString("check"));
            this.exists = Files.exists(check) && Files.isReadable(check);
            this.serverVersion = source.getInt("version", 0);
            if (this.exists) {
                this.myVersion = checkJsonVersion(check);
                this.outdated = serverVersion > myVersion;
            } else {
                this.outdated = false;
            }
        }

        this.status = exists ? DatasetStatus.INSTALLED : DatasetStatus.AVAILABLE;
        if (source.has("name")) {
            this.name = source.getString("name");
        } else if (baseData) {
            this.name = "default-data";
        }
        this.key = this.name;
        this.baseData = baseData || name.equals("default-data");

        if (source.has("version") && this.myVersion == -1)
            this.myVersion = source.getInt("version");

        if (source.has("mingsversion"))
            this.minGsVersion = source.getInt("mingsversion");

        if (source.has("file"))
            this.file = source.getString("file");


        // Description
        if (source.has("description")) {
            this.description = source.getString("description");

            if (description.contains("-")) {
                this.shortDescription = description.substring(0, description.indexOf("-"));
            } else {
                this.shortDescription = description;
            }
        }

        // Release notes
        if (source.has("releasenotes")) {
            this.releaseNotes = source.getString("releasenotes");
        } else {
            this.releaseNotes = null;
        }

        // Link
        if (source.has("link"))
            this.link = source.getString("link");
        else
            this.link = null;

        // Type
        if (source.has("type")) {
            this.type = source.getString("type");
        } else {
            this.type = "other";
        }

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
            nObjectsStr = I18n.txt("gui.dataset.nobjects", GlobalResources.nObjectsToString(nObjects));
        } catch (IllegalArgumentException e) {
            nObjects = -1;
            nObjectsStr = "N/A";
        }

        // Digest
        if (source.has("sha256"))
            sha256 = source.getString("sha256");
        else
            sha256 = null;

        // Data
        if (source.has("data")) {
            JsonValue data = source.get("data");
            try {
                this.filesToDelete = data.asStringArray();
            } catch (Exception e) {
            }
        } else {
            this.filesToDelete = null;
        }
    }

    /**
     * Checks the version file of the given path, if it is a correct JSON
     * file and contains a top-level "version" attribute. Otherwise, it
     * returns the default lowest version (0)
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

    public boolean isStarDataset() {
        return this.type != null && (this.type.equals("catalog-lod") || this.type.equals("catalog-gaia") || this.type.equals("catalog-star"));
    }

    public enum DatasetStatus {
        AVAILABLE,
        INSTALLED,
        DOWNLOADING
    }

    public DatasetDesc getLocalCopy(){
        DatasetDesc copy = this.copy();
        copy.name = copy.shortDescription;
        copy.description = copy.description.substring(copy.description.indexOf(" - ") + 3);
        copy.catalogFile = Gdx.files.absolute(copy.check.toAbsolutePath().toString());
        copy.path = copy.check.toAbsolutePath();
        return copy;
    }

    public DatasetDesc copy() {
        DatasetDesc copy = new DatasetDesc();
        copy.reader = this.reader;
        copy.source = this.source;
        copy.key = this.key;
        copy.name = this.name;
        copy.description = this.description;
        copy.shortDescription = this.shortDescription;
        copy.link = this.link;
        copy.type = this.type;
        copy.file = this.file;
        copy.datasetType = this.datasetType;
        copy.status = this.status;
        copy.check = this.check;
        copy.path = this.path;
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
        copy.filesToDelete = this.filesToDelete;
        copy.server = this.server;
        return copy;
    }

}
