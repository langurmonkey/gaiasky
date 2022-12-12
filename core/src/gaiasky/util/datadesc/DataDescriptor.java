/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DataDescriptor {

    public static DataDescriptor localDataDescriptor, serverDataDescriptor;

    /** View organised by types, where each time has a list of datasets **/
    public List<DatasetType> types;
    /** Raw datasets list, where each dataset has a type **/
    public List<DatasetDesc> datasets;

    public boolean updatesAvailable = false;
    public int numUpdates = 0;

    public DataDescriptor(List<DatasetType> types, List<DatasetDesc> datasets) {
        this.types = types;
        this.datasets = datasets;

        for (DatasetDesc ds : datasets) {
            updatesAvailable = updatesAvailable || ds.outdated;
            if (ds.outdated)
                numUpdates++;
        }
    }

    /**
     * Finds the dataset with the given name in the dataset descriptor list.
     *
     * @param name The name of the dataset
     *
     * @return The dataset descriptor or null if it was not found
     */
    public DatasetDesc findDataset(String name) {
        for (DatasetDesc dd : datasets) {
            if (dd.name.equals(name))
                return dd;
        }
        return null;
    }

    /**
     * Finds the dataset with the given key in the dataset descriptor list.
     *
     * @param key The key of the dataset
     *
     * @return The dataset descriptor or null if it was not found
     */
    public DatasetDesc findDatasetByKey(String key) {
        for (DatasetDesc dd : datasets) {
            if (dd.key.equals(key))
                return dd;
        }
        return null;
    }

    /**
     * Checks whether the dataset with the given name is present in the
     * data folder.
     *
     * @param name The dataset name.
     *
     * @return True if the dataset is present, false otherwise.
     */
    public boolean datasetPresent(String name) {
        DatasetDesc dd = findDataset(name);
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
