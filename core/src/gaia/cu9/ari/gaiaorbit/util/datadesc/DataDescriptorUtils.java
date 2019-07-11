/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.datadesc;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaia.cu9.ari.gaiaorbit.desktop.GaiaSkyDesktop;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

import java.util.*;

public class DataDescriptorUtils {
    private static Log logger = Logger.getLogger(DataDescriptorUtils.class);

    private static DataDescriptorUtils instance;
    public static DataDescriptorUtils instance(){
        if(instance == null)
            instance = new DataDescriptorUtils();
        return instance;
    }

    private FileHandle fh;
    private JsonReader reader;
    private DataDescriptorUtils(){
        super();
        this.reader = new JsonReader();
    }

    public DataDescriptor buildDatasetsDescriptor(FileHandle fh){
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
            if (!hasMinGsVersion || GaiaSkyDesktop.SOURCE_CONF_VERSION >= minGsVersion) {
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

}
