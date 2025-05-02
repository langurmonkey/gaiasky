/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.util.i18n.I18n;
import gaiasky.util.tree.OctreeNode;

import java.util.*;

/**
 * Central manager for {@link CatalogInfo} objects.
 */
public class CatalogManager implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(CatalogManager.class);

    private final Map<String, CatalogInfo> ciMap;
    private final List<CatalogInfo> cis;

    public CatalogManager() {
        super();
        ciMap = new HashMap<>();
        cis = new ArrayList<>(5);
        EventManager.instance.subscribe(this, Event.CATALOG_ADD, Event.CATALOG_REMOVE, Event.CATALOG_VISIBLE, Event.CATALOG_HIGHLIGHT, Event.CATALOG_POINT_SIZE_SCALING_CMD);
    }

    public Collection<CatalogInfo> getCatalogInfos() {
        return cis;
    }

    public boolean contains(String dsName) {
        return ciMap.containsKey(dsName);
    }

    /**
     * Gets the CatalogInfo with the given name, if any.
     *
     * @param dsName The name of the dataset.
     *
     * @return The CatalogInfo object, null if it does not exist.
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

    public Optional<CatalogInfo> getByEntity(Entity entity) {
        OctreeNode octant = null;
        if (Mapper.octant.has(entity)) {
            var oct = Mapper.octant.get(entity);
            octant = oct.octant != null ? oct.octant.getRoot() : null;
        }
        for (CatalogInfo ci : cis) {
            if (ci.entity != null) {
                if (octant != null) {
                    // Octree branch
                    if (Mapper.octree.has(ci.entity) && Mapper.octant.get(ci.entity).octant == octant)
                        return Optional.of(ci);
                } else {
                    if (ci.entity == entity)
                        return Optional.of(ci);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case CATALOG_ADD -> {
            CatalogInfo ci = (CatalogInfo) data[0];
            boolean addToSg = (Boolean) data[1];
            if (addToSg) {
                // Insert object into scene graph
                if (ci.entity != null) {
                    EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, ci.entity, true);
                }
            }
            String key = ci.name;
            if (ciMap.containsKey(key)) {
                int i = 1;
                String newKey = ci.name + " (" + i + ")";
                while (ciMap.containsKey(newKey)) {
                    i++;
                    newKey = ci.name + " (" + i + ")";
                }
                ci.name = newKey;
                key = newKey;
            }
            // Add to map and list
            ciMap.put(key, ci);
            cis.add(ci);
        }
        case CATALOG_REMOVE -> {
            CatalogInfo ci;
            String dsName = (String) data[0];
            if (ciMap.containsKey(dsName)) {
                ci = ciMap.get(dsName);
                EventManager.publish(Event.FOCUS_NOT_AVAILABLE, this, ci.entity);
                ci.removeCatalog();
                ciMap.remove(dsName);
                cis.remove(ci);
            }
        }
        case CATALOG_VISIBLE -> {
            CatalogInfo ci;
            String dsName;
            dsName = (String) data[0];
            boolean visible = (Boolean) data[1];
            if (ciMap.containsKey(dsName)) {
                ci = ciMap.get(dsName);
                if (!visible)
                    EventManager.publish(Event.FOCUS_NOT_AVAILABLE, this, ci.entity);
                ci.setVisibility(visible);
                logger.info(I18n.msg("notif.visibility." + (visible ? "on" : "off"), ci.name));
            }
        }
        case CATALOG_HIGHLIGHT -> {
            CatalogInfo ci;
            ci = (CatalogInfo) data[0];
            boolean highlight = (Boolean) data[1];
            if (ci != null) {
                ci.highlight(highlight);

                    if (ci.highlighted)
                        logger.info(I18n.msg("notif.highlight.on", ci.name));
                    else
                        logger.info(I18n.msg("notif.highlight.off", ci.name));
                }
            }
            case CATALOG_POINT_SIZE_SCALING_CMD -> {
                CatalogInfo ci;
                String dsName;
                dsName = (String) data[0];
                double scaling = (Double) data[1];
                if (ciMap.containsKey(dsName)) {
                    ci = ciMap.get(dsName);
                    if (ci.entity != null) {
                        var hl = Mapper.highlight.get(ci.entity);
                        hl.pointscaling = (float) scaling;
                    }
                }
            }
        }
    }
}
