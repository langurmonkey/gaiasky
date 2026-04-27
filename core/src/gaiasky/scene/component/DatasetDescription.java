/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.DatasetCard;
import gaiasky.util.parse.Parser;

import java.util.Map;

public class DatasetDescription implements Component {
    /**
     * Information on the catalog this fade node represents (particle group, octree, etc.)
     */
    public DatasetCard datasetCard = null;

    /**
     * A description.
     */
    public String description;

    /**
     * Internal attribute to keep track of previous opacity to know
     * when to update children with {@link gaiasky.scene.component.tag.TagNoProcess}.
     */
    public float previousAlpha = 1f;

    /** Whether to add this as a dataset. **/
    public boolean addDataset = true;

    public void setCatalogInfoBare(DatasetCard info) {
        this.datasetCard = info;
    }

    public void setCatalogInfo(DatasetCard info) {
        this.datasetCard = info;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDatasetCard(Map<String, Object> map) {
        String key = (String) map.get("dsKey");
        String name = (String) map.get("name");
        String desc = (String) map.get("description");
        String source = (String) map.get("source");
        DatasetCard.DatasetSourceType type = map.containsKey("type") ? DatasetCard.DatasetSourceType.valueOf((String) map.get("type")) : DatasetCard.DatasetSourceType.INTERNAL;
        // Highlight size factor.
        float size = getFloat(map, 1, "size", "hlSizeFactor", "highlightSizeFactor");
        // Size in bytes.
        long sizeBytes = getLong(map, -1, "sizebytes", "sizeBytes");
        // Number of particles/objects.
        long nParticles = getLong(map, -1, "nParticles");
        if (nParticles <= 0) {
            nParticles = getLong(map, -1, "nobjects", "nObjects", "numObjects");
        }

        this.datasetCard = new DatasetCard(key, name, desc, source, type, size);
        this.datasetCard.sizeBytes = sizeBytes;
        this.datasetCard.nParticles = nParticles;
    }


    public void setCatalogInfo(Map<String, Object> map) {
        setDatasetCard(map);
    }

    public void setCataloginfo(Map<String, Object> map) {
        setDatasetCard(map);
    }

    public void setDatasetInfo(Map<String, Object> map) {
        setDatasetCard(map);
    }

    public float getFloat(Map<String, Object> map, float defaultValue, String... keys) {
        for (var key : keys) {
            if (map.containsKey(key)) {
                Object value = map.get(key);
                if (value instanceof Float || value instanceof Double) {
                    return (float) value;
                } else if (value instanceof String) {
                    return Parser.parseFloat((String) value);
                }
            }
        }
        return defaultValue;
    }

    public long getLong(Map<String, Object> map, long defaultValue, String... keys) {
        for (var key : keys) {
            if (map.containsKey(key)) {
                Object value = map.get(key);
                if (value instanceof Long || value instanceof Integer) {
                    return (long) value;
                } else if (value instanceof String) {
                    return Parser.parseLong((String) value);
                }
            }
        }
        return defaultValue;
    }

    public void setAddToDatasetManager(Boolean b) {
        this.addDataset = b;
    }

}
