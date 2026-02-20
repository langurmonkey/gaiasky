/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.datasets.DatasetManagerWindow;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.update.VersionChecker;
import org.apache.commons.io.FileUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class DataDescriptorUtils {
    private static final Log logger = Logger.getLogger(DataDescriptorUtils.class);

    private static DataDescriptorUtils instance;
    private final JsonReader reader;
    private FileHandle fh;

    private static final Set<String> specialDirectories = Set.of(SysUtils.TMP_DIR_NAME, SysUtils.CACHE_DIR_NAME, SysUtils.PROCEDURAL_TEX_DIR_NAME);

    private DataDescriptorUtils() {
        super();
        this.reader = new JsonReader();
    }

    public static DataDescriptorUtils instance() {
        if (instance == null) {
            instance = new DataDescriptorUtils();
        }
        return instance;
    }

    /**
     * Checks whether the current data location contains old-version (pre-3.3.1) datasets.
     *
     * @return True if the data location contains old datasets.
     */
    public static boolean dataLocationOldVersionDatasetsCheck() {
        final var dataLocation = Path.of(Settings.settings.data.location);

        // Check presence of data-main.json
        if (Files.exists(dataLocation.resolve("data-main.json"))) {
            return true;
        }

        // Check presence of .json files, or directories without dataset.json.
        try (Stream<Path> stream = Files.list(dataLocation)) {
            var num = stream.filter(p -> {
                if (p.toFile().isFile() && p.getFileName().toString().endsWith(".json")) {
                    return true;
                } else
                    return p.toFile().isDirectory()
                            && !p.equals(dataLocation)
                            && !p.resolve("dataset.json").toFile().exists()
                            && !specialDirectories.contains(p.getFileName().toString());
            }).count();
            return num > 0;
        } catch (IOException e) {
            logger.warn(e);
        }

        return false;
    }

    public static void cleanDataLocationOldDatasets() {
        final var dataLocation = Path.of(Settings.settings.data.location);
        try (Stream<Path> stream = Files.list(dataLocation)) {
            var toDelete = stream.filter(p -> {
                if (p.toFile().isFile() && p.getFileName().toString().endsWith(".json")) {
                    return true;
                } else
                    return p.toFile().isDirectory()
                            && !p.equals(dataLocation)
                            && !p.resolve("dataset.json").toFile().exists()
                            && !specialDirectories.contains(p.getFileName().toString());
            }).toList();
            if (!toDelete.isEmpty()) {
                for (var delete : toDelete) {
                    if (Files.exists(delete)) {
                        String message = I18n.msg("gui.dscheck.deleting", ": data/" + delete.getFileName().toString());
                        logger.info(message);
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, dataLocation, message, 5f);
                        FileUtils.deleteQuietly(delete.toFile());
                    }
                }
                String message = I18n.msg("gui.dscheck.finish");
                logger.info(message);
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, dataLocation, message, 5f);
            }
        } catch (Exception e) {
            logger.warn(e);
        }

    }

    /**
     * Constructs a data descriptor from a server JSON file. If the file is null and <code>fh</code> is also null,
     * it returns null.
     *
     * @param fh The pointer to the server JSON file.
     *
     * @return An instance of {@link DataDescriptor}.
     */
    public synchronized DataDescriptor buildServerDatasets(FileHandle fh) {
        if (fh != null) {
            this.fh = fh;
        }
        if (this.fh != null) {
            logger.info("Building data descriptor model from file: " + this.fh.file().toPath());


            InputStream inputStream;
            try {
                FileInputStream fis = new FileInputStream(this.fh.file());
                try {
                    inputStream = new GZIPInputStream(fis);
                } catch (IOException e) {
                    logger.info("Not a gzipped file, trying uncompressed.");
                    // Re-open the file to get a fresh, unconsumed stream
                    inputStream = new FileInputStream(this.fh.file());
                }
            } catch (FileNotFoundException e) {
                logger.error("Error reading file: " + this.fh.file().toPath());
                return null;
            }

            try {
                JsonValue dataDesc = reader.parse(inputStream);

                Map<String, JsonValue> bestDs = new HashMap<>();
                Map<String, List<JsonValue>> typeMap = new HashMap<>();
                // We don't want repeated elements but want to keep insertion order
                Set<String> types = new LinkedHashSet<>();

                // Parse "recommended".
                var rec = dataDesc.get("recommended");
                String[] recommended = null;
                if (rec != null) {
                    try {
                        recommended = rec.asStringArray();
                    } catch (IllegalStateException ignored) {
                        // Nothing.
                    }
                }

                // Parse "files".
                var item = dataDesc.get("files").child();
                while (item != null) {
                    boolean hasMinGsVersion = item.has("mingsversion");
                    int minGsVersion = VersionChecker.correctVersionNumber(item.getInt("mingsversion", 0));
                    int thisVersion = item.getInt("version", 0);

                    // Only datasets with minGsVersion are supported.
                    // Only datasets with new format in 3.3.1 supported.
                    if (hasMinGsVersion && Settings.SOURCE_VERSION >= minGsVersion && minGsVersion >= 3030100) {
                        // Dataset type
                        String type = item.getString("type");

                        // Check if better option already exists
                        String dsKey = item.has("key") ? item.getString("key") : item.getString("name");
                        if (bestDs.containsKey(dsKey)) {
                            JsonValue other = bestDs.get(dsKey);
                            int otherVersion = other.getInt("version", 0);
                            if (otherVersion >= thisVersion) {
                                // Ignore this version
                                item = item.next();
                                continue;
                            } else {
                                // Remove other version, use this
                                typeMap.get(type).remove(other);
                                bestDs.remove(dsKey);
                            }
                        }

                        // Add to map
                        if (typeMap.containsKey(type)) {
                            typeMap.get(type).add(item);
                        } else {
                            List<JsonValue> aux = new ArrayList<>();
                            aux.add(item);
                            typeMap.put(type, aux);
                        }

                        // Add to set
                        types.add(type);
                        // Add to bestDs
                        bestDs.put(dsKey, item);
                    }
                    // Next
                    item = item.next();
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

                DataDescriptor desc = new DataDescriptor(typesList, datasetsList, recommended);
                DataDescriptor.serverDataDescriptor = desc;
                updateReplacedBy();
                return desc;
            } catch (Exception e) {
                logger.error(e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Constructs a list of local catalogs found in the current data location and combines
     * them with the server data.
     * Local catalogs are JSON files in the data directory that start with either 'catalog-'
     * or 'dataset-'.
     *
     * @param server The server data descriptor, for combining with the local catalogs.
     *
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
            final var cfs = catalogLocation.list(pathname -> (pathname.canRead()
                    && pathname.isDirectory()
                    && !pathname.getName().equals(Constants.DEFAULT_DATASET_KEY)
                    && pathname.toPath().resolve("dataset.json").toFile().exists()));

            for (FileHandle fh : cfs) {
                var fhDescriptor = new FileHandle(fh.file().toPath().resolve("dataset.json").toFile());
                catalogFiles.add(fhDescriptor);
            }

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
                if (remote.checkPath.equals(path)) {
                    // Found in remotes.
                    dd = remote;
                    it.remove();
                    break;
                }
            }
            if (dd == null) {
                // Not found in remote (server) dataset list, create it.
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

            // Only datasets without "mingsversion" or with new format in 3.3.1 supported.
            if (dd.minGsVersion < 0 || Settings.SOURCE_VERSION >= dd.minGsVersion && dd.minGsVersion >= 3030100) {
                dt.datasets.add(dd);
            }
        }
        // Sort
        Comparator<DatasetType> byType = Comparator.comparing(datasetType -> DatasetManagerWindow.getTypeWeight(datasetType.typeStr));
        types.sort(byType);

        DataDescriptor desc = new DataDescriptor(types, datasets);
        DataDescriptor.localDataDescriptor = desc;
        updateReplacedBy();
        return desc;
    }

    /**
     * Updates the {@link DatasetDesc#replacedBy} attribute of datasets by looking at the {@link DatasetDesc#replaces} attributes
     * and cross-referencing them. This function works across both local and server datasets
     */
    private void updateReplacedBy() {
        // Build full list.
        var list = new ArrayList<DatasetDesc>();
        var index = new FastStringObjectMap<>(100, Set.class);
        if (DataDescriptor.serverDataDescriptor != null) {
            list.addAll(DataDescriptor.serverDataDescriptor.datasets);
        }
        if (DataDescriptor.localDataDescriptor != null) {
            list.addAll(DataDescriptor.localDataDescriptor.datasets);
        }
        // Fill index.
        list.forEach((ds) -> {
            if (index.containsKey(ds.key)) {
                index.get(ds.key).add(ds);
            } else {
                var set = new HashSet<DatasetDesc>();
                set.add(ds);
                index.put(ds.key, set);
            }
        });

        // Update 'replacedBy' attribute in datasets.
        for (var ds : list) {
            if (ds.replaces != null) {
                for (var k : ds.replaces) {
                    var others = (HashSet<DatasetDesc>) index.get(k);
                    if (others != null && !others.isEmpty()) {
                        for (var other : others) {
                            if (other != null && !other.isReplacedBy(ds.key)) {
                                other.replacedBy = ds.key;
                            }
                        }
                    }
                }
            }
        }
        // Update 'replaces' attribute if needed.
        for (var ds : list) {
            if (ds.replacedBy != null && !ds.replacedBy.isBlank()) {
                var others = (HashSet<DatasetDesc>) index.get(ds.replacedBy);
                if (others != null && !others.isEmpty()) {
                    for (var other : others) {
                        if (other != null) {
                            other.addReplacesEntry(ds.key);
                        }
                    }
                }
            }
        }
    }
}
