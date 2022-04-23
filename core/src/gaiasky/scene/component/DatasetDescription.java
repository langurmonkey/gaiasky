package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.CatalogInfo;
import gaiasky.util.parse.Parser;

import java.util.Map;

public class DatasetDescription extends Component {
    /**
     * Information on the catalog this fade node represents (particle group, octree, etc.)
     */
    protected CatalogInfo catalogInfo = null;

    public void setCatalogInfoBare(CatalogInfo info) {
        this.catalogInfo = info;
    }

    public void setCatalogInfo(CatalogInfo info) {
        this.catalogInfo = info;
    }

    public void setCataloginfo(Map<String, String> map) {
        String name = map.get("name");
        String desc = map.get("description");
        String source = map.get("source");
        CatalogInfo.CatalogInfoSource type = map.get("type") != null ? CatalogInfo.CatalogInfoSource.valueOf(map.get("type")) : CatalogInfo.CatalogInfoSource.INTERNAL;
        float size = map.get("size") != null ? Parser.parseFloat(map.get("size")) : 1;
        long sizeBytes = map.get("sizebytes") != null ? Parser.parseLong(map.get("sizebytes")) : -1;
        long nObjects = map.get("nobjects") != null ? Parser.parseLong(map.get("nobjects")) : -1;
        this.catalogInfo = new CatalogInfo(name, desc, source, type, size, null);
        this.catalogInfo.sizeBytes = sizeBytes;
        this.catalogInfo.nParticles = nObjects;
    }
}
