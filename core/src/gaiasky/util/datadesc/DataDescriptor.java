/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Contains a list of dataset descriptors, as {@link DatasetDesc}s.
 */
public class DataDescriptor {

    public static DataDescriptor localDataDescriptor, serverDataDescriptor;

    /** View organised by types, where each time has a list of datasets **/
    public final List<DatasetType> types;
    /** Raw datasets list, where each dataset has a type **/
    public final List<DatasetDesc> datasets;

    /** Recommended datasets as an array of keys. **/
    public String[] recommended;

    public boolean updatesAvailable = false;
    public int numUpdates = 0;

    public DataDescriptor(List<DatasetType> types, List<DatasetDesc> datasets, String[] recommended) {
        this.types = types;
        this.datasets = datasets;
        this.recommended = recommended;

        this.initialize();

    }

    public DataDescriptor(List<DatasetType> types, List<DatasetDesc> datasets) {
        this(types, datasets, null);
    }

    private void initialize() {
        // Index and number of updates.
        for (DatasetDesc ds : datasets) {
            if (ds != null) {
                updatesAvailable = updatesAvailable || ds.outdated;
                if (ds.outdated)
                    numUpdates++;
            }
        }
    }

    /**
     * Finds the dataset with the given name in the dataset descriptor list.
     *
     * @param name The name of the dataset.
     *
     * @return The dataset descriptor or null if it was not found.
     */
    public DatasetDesc findDatasetByName(String name) {
        for (DatasetDesc dd : datasets) {
            if (dd.name.equalsIgnoreCase(name))
                return dd;
        }
        return null;
    }

    /**
     * Finds the dataset with the given key in the dataset descriptor list.
     *
     * @param key The key of the dataset.
     *
     * @return The dataset descriptor or null if it was not found.
     */
    public DatasetDesc findDatasetByKey(String key) {
        for (DatasetDesc dd : datasets) {
            if (dd.key.equalsIgnoreCase(key))
                return dd;
        }
        return null;
    }

    /**
     * Checks whether the dataset with the given name is present in the
     * data folder.
     *
     * @param key The dataset key.
     *
     * @return True if the dataset is present, false otherwise.
     */
    public boolean datasetPresent(String key) {
        DatasetDesc dd = findDatasetByKey(key);
        if (dd != null) {
            return dd.exists;
        }
        return false;
    }

    /**
     * Finds the dataset with the given descriptor file in the dataset descriptor list.
     *
     * @param descriptorFile The filename of the descriptor file.
     *
     * @return The dataset descriptor or null if it was not found.
     */
    public DatasetDesc findDatasetByDescriptor(Path descriptorFile) throws IOException {
        if (Files.exists(descriptorFile))
            for (DatasetDesc dd : datasets) {
                if (dd.checkPath != null && Files.exists(dd.checkPath) && Files.isSameFile(dd.checkPath, descriptorFile))
                    return dd;
            }
        return null;
    }

}
