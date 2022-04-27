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
 * Represents a generic catalog of objects.
 * This entity loads catalog data given a provider and a file, initializes the objects
 * and adds them as children.
 */
public class GenericCatalog extends FadeNode {

    protected String description;

    /**
     * Fully qualified name of data provider class
     */
    protected String provider;

    /**
     * Path of data file
     */
    protected String datafile;

    /**
     * STIL data source, if no data file exists
     */
    protected DataSource ds;

    protected Array<? extends SceneGraphNode> objects;

    public GenericCatalog() {
        super();
        this.description = "-";
    }

    @Override
    public void initialize() {
        initialize(provider != null && !provider.isBlank(), true);
    }

    public void initialize(boolean dataLoad, boolean createCatalogInfo) {
        super.initialize();
        // Load data
        try {
            String dsName = this.getName();

            // Load data and add
            if (dataLoad) {
                Class<?> clazz = Class.forName(provider);
                ISceneGraphLoader provider = (ISceneGraphLoader) clazz.getConstructor().newInstance();
                if(datafile != null)
                    provider.initialize(new String[]{datafile});
                else if(ds!= null)
                    provider.initialize(ds);

                provider.setName(dsName);
                objects = provider.loadData();
                objects.forEach(object -> {
                    object.setParent(dsName);
                    object.setColor(this.cc);
                    object.setLabelcolor(this.labelcolor != null ? this.labelcolor.clone() : this.cc.clone());
                    object.initialize();
                });
            }

            // Create catalog info
            initializeCatalogInfo(createCatalogInfo, dsName, description, -1, null);
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if (children != null)
            children.forEach(child -> child.doneLoading(manager));
    }

    @Override
    public void setUp(ISceneGraph sceneGraph) {
        super.setUp(sceneGraph);
        if (objects != null) {
            objects.forEach(object -> {
                sceneGraph.insert(object, true);
            });
            objects.clear();
            objects = null;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDatafile() {
        return datafile;
    }

    public void setDatafile(String datafile) {
        this.datafile = datafile;
    }
    public void setDataSource(DataSource ds) {
        this.ds = ds;
    }
}
