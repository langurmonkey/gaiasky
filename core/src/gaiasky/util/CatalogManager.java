/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;

import java.util.*;

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
    private List<CatalogInfo> cis;

    CatalogManager() {
        super();
        ciMap = new HashMap<>();
        cis = new ArrayList(5);
        EventManager.instance.subscribe(this, Events.CATALOG_ADD, Events.CATALOG_REMOVE, Events.CATALOG_VISIBLE, Events.CATALOG_HIGHLIGHT);
    }

    public Collection<CatalogInfo> getCatalogInfos() {
        return cis;
    }

    public boolean contains(String dsName) {
        return ciMap.containsKey(dsName);
    }

    /**
     * Gets the CatalogInfo with the given name, if any
     * @param dsName The name of the dataset
     * @return The CatalogInfo object, null if it does not exist
     */
    public CatalogInfo get(String dsName) {
        return ciMap.get(dsName);
    }

    public Set<String> getDatasetNames() {
        if (ciMap != null) {
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
            // Add to map and list
            ciMap.put(ci.name, ci);
            cis.add(ci);
            break;
        case CATALOG_REMOVE:
            String dsName = (String) data[0];
            if (ciMap.containsKey(dsName)) {
                ci = ciMap.get(dsName);
                EventManager.instance.post(Events.FOCUS_NOT_AVAILABLE, ci.object);
                ci.removeCatalog();
                ciMap.remove(dsName);
                cis.remove(ci);
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
        case CATALOG_HIGHLIGHT:
            dsName = (String) data[0];
            boolean highlight = (Boolean) data[1];
            float[] col = (float[]) data[2];
            if (ciMap.containsKey(dsName)) {
                ci = ciMap.get(dsName);
                if (col == null)
                    ci.highlight(highlight);
                else
                    ci.highlight(highlight, col);

                if (ci.highlighted)
                    logger.info(I18n.txt("notif.highlight.on", ci.name));
                else
                    logger.info(I18n.txt("notif.highlight.off", ci.name));
            }
            break;
        default:
            break;
        }
    }
}
