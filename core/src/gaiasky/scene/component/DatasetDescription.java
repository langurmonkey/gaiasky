package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scenegraph.FadeNode;
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
        this.catalogInfo = new CatalogInfo(name, desc, source, type, size, (FadeNode) null);
        this.catalogInfo.sizeBytes = sizeBytes;
        this.catalogInfo.nParticles = nParticles;

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

    public void setCataloginfo(Map<String, Object> map) {
        setCatalogInfo(map);
    }
}
