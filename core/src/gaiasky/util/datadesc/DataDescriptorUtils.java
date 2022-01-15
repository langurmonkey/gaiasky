/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.desktop.GaiaSkyDesktop;
import gaiasky.interafce.DatasetManagerWindow;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

import java.nio.file.Path;
import java.util.*;

public class DataDescriptorUtils {
    private static final Log logger = Logger.getLogger(DataDescriptorUtils.class);

    private static DataDescriptorUtils instance;

    public static DataDescriptorUtils instance() {
        if (instance == null) {
            instance = new DataDescriptorUtils();
        }
        return instance;
    }

    private FileHandle fh;
    private final JsonReader reader;

    private DataDescriptorUtils() {
        super();
        this.reader = new JsonReader();
    }

    public int getTypeWeight(String type) {
        return switch (type) {
            case "data-pack" -> -2;
            case "texture-pack" -> -1;
            case "catalog-lod" -> 0;
            case "catalog-gaia" -> 1;
            case "catalog-star" -> 2;
            case "catalog-gal" -> 3;
            case "catalog-cluster" -> 4;
            case "catalog-other" -> 5;
            case "mesh" -> 6;
            case "other" -> 8;
            default -> 10;
        };
    }

    /**
     * Constructs a data descriptor from a server JSON file.
     *
     * @param fh The pointer to the server JSON file.
     * @return An instance of {@link DataDescriptor}.
     */
    public synchronized DataDescriptor buildServerDatasets(FileHandle fh) {
        if (fh != null) {
            this.fh = fh;
        }
        logger.info("Building data descriptor model: " + this.fh.toString());

        JsonValue dataDesc = reader.parse(this.fh);

        Map<String, JsonValue> bestDs = new HashMap<>();
        Map<String, List<JsonValue>> typeMap = new HashMap<>();
        // We don't want repeated elements but want to keep insertion order
        Set<String> types = new LinkedHashSet<>();

        JsonValue dst = dataDesc.child().child();
        while (dst != null) {
            boolean hasMinGsVersion = dst.has("mingsversion");
            int minGsVersion = dst.getInt("mingsversion", 0);
            int thisVersion = dst.getInt("version", 0);
            if (!hasMinGsVersion || GaiaSkyDesktop.SOURCE_VERSION >= minGsVersion) {
                // Dataset type
                String type = dst.getString("type");

                // Check if better option already exists
                String dsName = dst.getString("name");
                if (bestDs.containsKey(dsName)) {
                    JsonValue other = bestDs.get(dsName);
                    int otherVersion = other.getInt("version", 0);
                    if (otherVersion >= thisVersion) {
                        // Ignore this version
                        dst = dst.next();
                        continue;
                    } else {
                        // Remove other version, use this
                        typeMap.get(type).remove(other);
                        bestDs.remove(dsName);
                    }
                }

                // Add to map
                if (typeMap.containsKey(type)) {
                    typeMap.get(type).add(dst);
                } else {
                    List<JsonValue> aux = new ArrayList<>();
                    aux.add(dst);
                    typeMap.put(type, aux);
                }

                // Add to set
                types.add(type);
                // Add to bestDs
                bestDs.put(dsName, dst);
            }
            // Next
            dst = dst.next();
        }

        // Convert to model
        List<DatasetType> typesList = new ArrayList<>(types.size());
        List<DatasetDesc> datasetsList = new ArrayList<>();
        for (String typeStr : types) {
            List<JsonValue> datasets = typeMap.get(typeStr);
            DatasetType currentType = new DatasetType(typeStr);

            for (JsonValue dataset : datasets) {
                DatasetDesc dd = new DatasetDesc(reader, dataset);
                dd.datasetType = currentType;
                currentType.addDataset(dd);
                datasetsList.add(dd);
            }
            typesList.add(currentType);
        }

        DataDescriptor desc = new DataDescriptor(typesList, datasetsList);
        return desc;
    }

    /**
     * Constructs a list of local catalogs found in the current data location and combines
     * them with the server data.
     * Local catalogs are JSON files in the data directory that start with either 'catalog-'
     * or 'dataset-'.
     *
     * @param server The server data descriptor, for combining with the local catalogs.
     * @return An instance of {@link DataDescriptor}.
     */
    public synchronized DataDescriptor buildLocalDatasets(DataDescriptor server) {
        Array<FileHandle> catalogLocations = new Array<>();
        catalogLocations.add(Gdx.files.absolute(Settings.settings.data.location));

        Array<FileHandle> catalogFiles = new Array<>();

        for (FileHandle catalogLocation : catalogLocations) {
            FileHandle[] cfs = catalogLocation.list(pathname -> (pathname.getName().startsWith("catalog-") || pathname.getName().startsWith("dataset-")) && pathname.getName().endsWith(".json"));
            catalogFiles.addAll(cfs);
        }

        JsonReader reader = new JsonReader();
        Map<String, DatasetType> typeMap = new HashMap<>();
        List<DatasetType> types = new ArrayList<>();
        List<DatasetDesc> datasets = new ArrayList<>();
        for (FileHandle catalogFile : catalogFiles) {
            JsonValue val = reader.parse(catalogFile);
            DatasetDesc dd = new DatasetDesc(reader, val);
            dd.path = Path.of(catalogFile.path());
            dd.catalogFile = catalogFile;
            dd.exists = true;
            dd.status = DatasetDesc.DatasetStatus.INSTALLED;

            DatasetType dt;
            if (typeMap.containsKey(dd.type)) {
                dt = typeMap.get(dd.type);
            } else {
                dt = new DatasetType(dd.type);
                typeMap.put(dd.type, dt);
                types.add(dt);
            }
            dt.datasets.add(dd);
            datasets.add(dd);
        }

        if (server != null && server.datasets != null) {
            // Combine server with remote datasets
            for (DatasetDesc local : datasets) {
                for (DatasetDesc remote : server.datasets) {
                    if (remote.check.getFileName().toString().equals(local.path.getFileName().toString())) {
                        // Match, update local with some server info
                        if (local.name.equals(remote.name)) {
                            // Update name, as ours is the remote key
                            local.name = remote.shortDescription;
                            local.shortDescription = remote.shortDescription;
                            local.description = remote.description.substring(remote.description.indexOf(" - ") + 3);
                        }
                        local.check = remote.check;
                        local.key = remote.key;
                        local.filesToDelete = remote.filesToDelete;
                        local.file = remote.file;
                        local.serverVersion = remote.serverVersion;
                        local.outdated = remote.outdated;
                        if (local.releaseNotes == null) {
                            local.releaseNotes = remote.releaseNotes;
                        }
                        if (local.datasetType == null) {
                            local.datasetType = remote.datasetType;
                        }
                        if (local.link == null) {
                            local.link = remote.link;
                        }
                        if (local.description == null) {
                            local.description = remote.description;
                        }
                        if (local.shortDescription == null) {
                            local.shortDescription = remote.shortDescription;
                        }
                        local.server = remote;
                    }
                }
            }

            // Add remotes on disk that are not catalogs
            for (DatasetDesc remote : server.datasets) {
                if (remote.exists) {
                    boolean found = false;
                    for (DatasetDesc local : datasets) {
                        if (remote.check.getFileName().toString().equals(local.path.getFileName().toString())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // Add to local datasets
                        DatasetDesc copy = remote.getLocalCopy();
                        datasets.add(copy);
                        // Add to local types
                        DatasetType remoteType = copy.datasetType;
                        found = false;
                        for (DatasetType localType : types) {
                            if (localType.equals(remoteType)) {
                                localType.datasets.add(copy);
                                copy.datasetType = localType;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            // No type! create it
                            DatasetType newType = new DatasetType(remoteType.typeStr);
                            newType.addDataset(copy);
                            copy.datasetType = newType;
                            types.add(newType);
                        }
                    }
                }
            }
        }

        // Default values in case this is a totally offline dataset
        for (DatasetDesc dd : datasets) {
            if (dd.description == null)
                dd.description = dd.path.toString();
            if (dd.shortDescription == null)
                dd.shortDescription = dd.description;
            if (dd.name == null)
                dd.name = dd.catalogFile.nameWithoutExtension();
        }

        Comparator<DatasetType> byType = Comparator.comparing(datasetType -> DatasetManagerWindow.getTypeWeight(datasetType.typeStr));
        types.sort(byType);

        return new DataDescriptor(types, datasets);
    }
}
