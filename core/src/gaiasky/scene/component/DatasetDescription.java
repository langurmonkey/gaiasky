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

    public void setCataloginfo(Map<String, String> map) {
        String name = map.get("name");
        String desc = map.get("description");
        String source = map.get("source");
        CatalogInfo.CatalogInfoSource type = map.get("type") != null ? CatalogInfo.CatalogInfoSource.valueOf(map.get("type")) : CatalogInfo.CatalogInfoSource.INTERNAL;
        float size = map.get("size") != null ? Parser.parseFloat(map.get("size")) : 1;
        long sizeBytes = map.get("sizebytes") != null ? Parser.parseLong(map.get("sizebytes")) : -1;
        long nObjects = map.get("nobjects") != null ? Parser.parseLong(map.get("nobjects")) : -1;
        this.catalogInfo = new CatalogInfo(name, desc, source, type, size, (FadeNode) null);
        this.catalogInfo.sizeBytes = sizeBytes;
        this.catalogInfo.nParticles = nObjects;
    }
}
