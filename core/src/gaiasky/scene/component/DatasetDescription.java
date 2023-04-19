/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.CatalogInfo;
import gaiasky.util.parse.Parser;

import java.util.Map;

public class DatasetDescription implements Component {
    /**
     * Information on the catalog this fade node represents (particle group, octree, etc.)
     */
    public CatalogInfo catalogInfo = null;

    /**
     * A description.
     */
    public String description;

    /**
     * Internal attribute to keep track of previous opacity to know
     * when to update children with {@link gaiasky.scene.component.tag.TagNoProcess}.
     */
    public float previousAlpha = 1f;

    public void setCatalogInfoBare(CatalogInfo info) {
        this.catalogInfo = info;
    }

    public void setCatalogInfo(CatalogInfo info) {
        this.catalogInfo = info;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCatalogInfo(Map<String, Object> map) {
        String name = (String) map.get("name");
        String desc = (String) map.get("description");
        String source = (String) map.get("source");
        CatalogInfo.CatalogInfoSource type = map.containsKey("type") ? CatalogInfo.CatalogInfoSource.valueOf((String) map.get("type")) : CatalogInfo.CatalogInfoSource.INTERNAL;
        float size = getFloat(map, "size", 1);
        long sizeBytes = getLong(map, "sizebytes", -1);
        long nParticles = getLong(map, "nParticles", -1);
        if (nParticles <= 0) {
            nParticles = getLong(map, "nobjects", -1);
        }
        this.catalogInfo = new CatalogInfo(name, desc, source, type, size);
        this.catalogInfo.sizeBytes = sizeBytes;
        this.catalogInfo.nParticles = nParticles;

    }

    public void setCataloginfo(Map<String, Object> map) {
        setCatalogInfo(map);
    }

    public void setDatasetInfo(Map<String, Object> map) {
        setCatalogInfo(map);
    }

    public float getFloat(Map<String, Object> map, String key, float defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Float || value instanceof Double) {
                return (float) value;
            } else if (value instanceof String) {
                return Parser.parseFloat((String) value);
            }
        }
        return defaultValue;
    }

    public long getLong(Map<String, Object> map, String key, long defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Long || value instanceof Integer) {
                return (long) value;
            } else if (value instanceof String) {
                return Parser.parseLong((String) value);
            }
        }
        return defaultValue;
    }

}
