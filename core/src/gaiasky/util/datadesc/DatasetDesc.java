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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatasetDesc {
    private final Log logger = Logger.getLogger(DatasetDesc.class);

    private JsonReader reader;
    public JsonValue source;
    public String name;
    public String description;
    public String shortDescription;
    public String link;
    public String type;
    public String file;
    public DatasetType datasetType;

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

    public boolean mustDownload;
    public boolean cbDisabled;
    public String[] filesToDelete;

    public DatasetDesc(JsonReader reader, JsonValue source) {
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

        this.name = source.getString("name");
        this.baseData = name.equals("default-data");
        this.mustDownload = (!exists || outdated) && baseData;
        this.cbDisabled = baseData || (exists && !outdated);

        if (source.has("version") && this.myVersion == -1)
            this.myVersion = source.getInt("version");

        if (source.has("mingsversion"))
            this.minGsVersion = source.getInt("mingsversion");

        if (source.has("file"))
            this.file = source.getString("file");

        // Description
        this.description = source.getString("description");
        if (description.contains("-")) {
            this.shortDescription = description.substring(0, description.indexOf("-"));
        } else {
            this.shortDescription = description;
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
}
