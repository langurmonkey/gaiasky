/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.datadesc;

import java.util.List;

public class DataDescriptor {

    public static DataDescriptor currentDataDescriptor;

    /** View organised by types, where each time has a list of datasets **/
    public List<DatasetType> types;
    /** Raw datasets list, where each dataset has a type **/
    public List<DatasetDesc> datasets;

    public boolean updatesAvailable = false;
    public int numUpdates = 0;

    public DataDescriptor(List<DatasetType> types, List<DatasetDesc> datasets){
        this.types = types;
        this.datasets = datasets;

        for(DatasetDesc ds : datasets){
            updatesAvailable = updatesAvailable || ds.outdated;
            if(ds.outdated)
                numUpdates++;
        }
        currentDataDescriptor = this;
    }
}
