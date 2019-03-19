package gaia.cu9.ari.gaiaorbit.util;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CatalogManager implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(CatalogManager.class);

    private static CatalogManager instance;

    public static void initialize() {
        instance();
    }

    public static CatalogManager instance() {
        if (instance == null)
            instance = new CatalogManager();
        return instance;
    }

    private Map<String, CatalogInfo> ciMap;

    CatalogManager() {
        super();
        ciMap = new HashMap<>();
        EventManager.instance.subscribe(this, Events.CATALOG_ADD, Events.CATALOG_REMOVE, Events.CATALOG_VISIBLE);
    }

    public Collection<CatalogInfo> getCatalogInfos() {
        return ciMap.values();
    }

    public boolean contains(String dsName) {
        return ciMap.containsKey(dsName);
    }

    public Set<String> getDatasetNames(){
        if(ciMap != null){
            return ciMap.keySet();
        }
        return null;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case CATALOG_ADD:
            CatalogInfo ci = (CatalogInfo) data[0];
            boolean addToSg = (Boolean) data[1];
            if (addToSg) {
                // Insert object into scene graph
                EventManager.instance.post(Events.SCENE_GRAPH_ADD_OBJECT_CMD, ci.object, true);
            }
            // Add to map
            ciMap.put(ci.name, ci);
            break;
        case CATALOG_REMOVE:
            String dsName = (String) data[0];
            if (ciMap.containsKey(dsName)) {
                ci = ciMap.get(dsName);
                EventManager.instance.post(Events.FOCUS_NOT_AVAILABLE, ci.object);
                ci.removeCatalog();
                ciMap.remove(dsName);
            }
            break;
        case CATALOG_VISIBLE:
            dsName = (String) data[0];
            boolean visible = (Boolean) data[1];
            if (ciMap.containsKey(dsName)) {
                ci = ciMap.get(dsName);
                if (!visible)
                    EventManager.instance.post(Events.FOCUS_NOT_AVAILABLE, ci.object);
                ci.setVisibility(visible);
                logger.info(I18n.txt("notif.visibility." + (visible ? "on" : "off"), ci.name));
            }
            break;
        default:
            break;
        }
    }
}
