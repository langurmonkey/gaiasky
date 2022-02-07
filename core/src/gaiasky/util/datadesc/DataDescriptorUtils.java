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
            case "satellites" -> 7;
            case "systems" -> 8;
            case "other" -> 9;
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
                String dsKey = dst.has("key") ? dst.getString("key") : dst.getString("name");
                if (bestDs.containsKey(dsKey)) {
                    JsonValue other = bestDs.get(dsKey);
                    int otherVersion = other.getInt("version", 0);
                    if (otherVersion >= thisVersion) {
                        // Ignore this version
                        dst = dst.next();
                        continue;
                    } else {
                        // Remove other version, use this
                        typeMap.get(type).remove(other);
                        bestDs.remove(dsKey);
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
                bestDs.put(dsKey, dst);
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
        DataDescriptor.serverDataDescriptor = desc;
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
        // Get all server datasets that exist locally
        List<DatasetDesc> existing = new ArrayList<>();
        if (server != null) {
            for (DatasetDesc dd : server.datasets) {
                if (dd.exists) {
                    existing.add(dd.getLocalCopy());
                }
            }
        }

        // Get all local catalogs
        Array<FileHandle> catalogLocations = new Array<>();
        catalogLocations.add(Gdx.files.absolute(Settings.settings.data.location));

        Array<FileHandle> catalogFiles = new Array<>();

        for (FileHandle catalogLocation : catalogLocations) {
            FileHandle[] cfs = catalogLocation.list(pathname -> (pathname.getName().startsWith("catalog-") || pathname.getName().startsWith("dataset-")) && pathname.getName().endsWith(".json"));
            catalogFiles.addAll(cfs);
        }

        JsonReader reader = new JsonReader();
        List<DatasetType> types = new ArrayList<>();
        List<DatasetDesc> datasets = new ArrayList<>();
        for (FileHandle catalogFile : catalogFiles) {
            JsonValue val = reader.parse(catalogFile);
            Path path = Path.of(catalogFile.path());

            DatasetDesc dd = null;
            Iterator<DatasetDesc> it = existing.iterator();
            while (it.hasNext()) {
                DatasetDesc remote = it.next();
                if (remote.checkPath.getFileName().toString().equals(path.getFileName().toString())) {
                    // Found in remotes
                    dd = remote;
                    it.remove();
                    break;
                }
            }
            if (dd == null) {
                // Not found, create it
                dd = new DatasetDesc(reader, val, catalogFile);
            }
            dd.catalogFile = catalogFile;
            dd.exists = true;
            dd.status = DatasetDesc.DatasetStatus.INSTALLED;

            datasets.add(dd);
        }
        datasets.addAll(existing);

        // Default values in case this is a totally offline dataset
        for (DatasetDesc dd : datasets) {
            if (dd.description == null)
                dd.description = dd.checkPath.toString();
            if (dd.name == null)
                dd.name = dd.catalogFile.nameWithoutExtension();
        }

        // Create types
        Map<String, DatasetType> typeMap = new HashMap<>();
        for (DatasetDesc dd : datasets) {
            DatasetType dt;
            if (typeMap.containsKey(dd.type)) {
                dt = typeMap.get(dd.type);
            } else {
                dt = new DatasetType(dd.type);
                typeMap.put(dd.type, dt);
                types.add(dt);
            }
            dd.datasetType = dt;
            dt.datasets.add(dd);
        }
        // Sort
        Comparator<DatasetType> byType = Comparator.comparing(datasetType -> DatasetManagerWindow.getTypeWeight(datasetType.typeStr));
        types.sort(byType);

        DataDescriptor desc = new DataDescriptor(types, datasets);
        DataDescriptor.localDataDescriptor = desc;
        return desc;
    }
}
