package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.CatalogInfo;

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
}
