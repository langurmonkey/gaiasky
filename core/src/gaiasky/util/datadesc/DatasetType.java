/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import java.util.ArrayList;
import java.util.List;

public class DatasetType {
    public String typeStr;
    public List<DatasetDesc> datasets;

    public DatasetType(String typeStr) {
        this.typeStr = typeStr;
        this.datasets = new ArrayList<>();
    }

    public void addDataset(DatasetDesc dd) {
        this.datasets.add(dd);
    }
}
