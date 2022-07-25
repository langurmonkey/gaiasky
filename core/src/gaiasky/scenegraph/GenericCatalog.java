package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.ISceneGraphLoader;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Logger;
import uk.ac.starlink.util.DataSource;

/**
 * Represents a generic catalog of objects. Only difference with FadeNode is that the generic
 * catalog creates a catalog info object if it does not exist.
 */
public class GenericCatalog extends FadeNode {

    public GenericCatalog() {
        super();
    }

}
