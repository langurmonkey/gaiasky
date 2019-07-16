/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.datadesc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatasetDesc {
    private final Log logger = Logger.getLogger(DatasetDesc.class);

    private final JsonReader reader;
    public final JsonValue source;
    public String name;
    public String description;
    public String shortDescription;
    public String link;
    public String type;
    public String file;
    public DatasetType datasetType;

    public Path check;

    public String size;
    public long sizeBytes;

    public String sha256;

    public boolean exists;
    public int myVersion, serverVersion;
    public boolean outdated;
    public boolean baseData;

    public boolean mustDownload;
    public boolean cbDisabled;
    public String[] filesToDelete;


    public DatasetDesc(JsonReader reader, JsonValue source){
        this.reader = reader;
        this.source = source;

        // Check if we have it
        this.check = Paths.get(GlobalConf.data.DATA_LOCATION, source.getString("check"));
        this.exists = Files.exists(check) && Files.isReadable(check);
        this.serverVersion = source.getInt("version", 0);
        if(this.exists) {
            this.myVersion = checkJsonVersion(check);
            this.outdated = serverVersion > myVersion;
        } else {
            this.outdated = false;
        }

        this.mustDownload = (!exists || outdated) && baseData;
        this.cbDisabled = baseData || (exists && !outdated);

        this.name = source.getString("name");
        this.file = source.getString("file");
        this.baseData = name.equals("default-data");

        // Description
        this.description = source.getString("description");
        if (description.contains("-")) {
            this.shortDescription = description.substring(0, description.indexOf("-"));
        } else {
            this.shortDescription = description;
        }

        // Link
        if(source.has("link"))
        this.link = source.getString("link");
        else
            this.link = null;

        // Type
        this.type = source.getString("type");

        // Size
        try {
            sizeBytes = source.getLong("size");
            size = GlobalResources.humanReadableByteCount(sizeBytes, true);
        } catch (IllegalArgumentException e) {
            sizeBytes = -1;
            size = "?";
        }

        // Digest
        if(source.has("sha256"))
            sha256 = source.getString("sha256");
        else
            sha256 = null;

        // Data
        if(source.has("data")){
            JsonValue data = source.get("data");
            this.filesToDelete = data.asStringArray();
        }else{
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
                    if(jf.has("version")){
                        try {
                            return jf.getInt("version", 0);
                        }catch(Exception e){
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
}
