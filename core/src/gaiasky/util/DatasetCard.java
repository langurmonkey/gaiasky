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
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DatasetUtils;
import gaiasky.util.datadesc.Dataset;
import gaiasky.util.filter.Filter;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.i18n.I18n;

import java.time.Instant;

/**
 * Saves the metadata on a particular catalog or dataset loaded into Gaia Sky, and implements
 * some common operations like highlighting.
 */
public class DatasetCard {
    private static final Log logger = Logger.getLogger(DatasetCard.class);
    private static int colorIndexSequence = 0;
    private final FocusView view;

    /** Dataset key. A dataset may have multiple cards, in this case all point to the same dataset. **/
    public String dsKey;
    /** Dataset name. **/
    public String name;
    /** Dataset description. **/
    public String description;
    /** Dataset source string. **/
    public String source;
    public long nParticles;
    public long sizeBytes;
    public Instant loadDateUTC;
    public Dataset dd;

    // Highlight
    public boolean highlighted;
    public boolean plainColor;
    public float[] hlColor;
    public float hlSizeFactor;
    public boolean hlAllVisible;
    public int hlCmapIndex = 0;
    public float hlCmapAlpha = 1f;
    public IAttribute hlCmapAttribute;
    public double hlCmapMin = 0, hlCmapMax = 0;

    // The filtering object. May be null.
    public Filter filter;

    // Catalog source type
    public DatasetSourceType type;
    // Catalog content type
    public DatasetContentType objectType;

    // Reference to the entity
    public Entity entity;

    public DatasetCard(String dsKey,
                       String name,
                       String description,
                       String source,
                       DatasetSourceType type,
                       float hlSizeFactor,
                       Entity entity) {
        this(dsKey, name, description, source, type, hlSizeFactor);
        setEntity(entity);
    }

    public DatasetCard(String dsKey,
                        String name,
                       String description,
                       String source,
                       DatasetSourceType type,
                       float hlSizeFactor) {
        super();
        this.dsKey = dsKey;
        this.name = name;
        this.description = description;
        this.source = source;
        this.type = type;
        this.loadDateUTC = Instant.now();
        this.plainColor = true;
        this.hlColor = new float[4];
        this.hlSizeFactor = hlSizeFactor;
        this.hlAllVisible = true;
        this.view = new FocusView();
        System.arraycopy(ColorUtils.getColorFromIndex(colorIndexSequence++), 0, this.hlColor, 0, 4);

        // Set descriptor, if any.
        this.dd = DatasetUtils.instance().getMatchByKey(dsKey);

    }

    public void setEntity(Entity entity) {
        if (entity != null) {
            this.entity = entity;
            this.view.setEntity(this.entity);
            if (Mapper.datasetDescription.has(entity)) {
                Mapper.datasetDescription.get(entity).setCatalogInfo(this);
            }

            if (this.objectType == null) {
                if (isEntitySatellite()) {
                    this.objectType = DatasetContentType.MISSION;
                } else if (isEntityStarSet()) {
                    this.objectType = DatasetContentType.STAR_SET;
                } else if (isEntityParticleSet()) {
                    this.objectType = DatasetContentType.PARTICLE_SET;
                } else {
                    this.objectType = DatasetContentType.OTHER;
                }
            }
        }
    }

    public void setVisibility(boolean visibility) {
        if (this.entity != null) {
            synchronized (view) {
                view.setEntity(this.entity);
                view.setVisibleGroup(visibility);
            }
        }
    }

    public boolean isVisible() {
        return isVisible(false);
    }

    public boolean isVisible(boolean attributeValue) {
        if (this.entity != null) {
            boolean ret;
            synchronized (view) {
                view.setEntity(this.entity);
                ret = view.isVisibleGroup(attributeValue);
            }
            return ret;
        }
        return true;
    }

    public void setColor(float r,
                         float g,
                         float b,
                         float a) {
        this.hlColor[0] = r;
        this.hlColor[1] = g;
        this.hlColor[2] = b;
        this.hlColor[3] = a;
        highlight(highlighted);
    }

    public float[] getHlColor() {
        return hlColor;
    }

    public void setHlColor(float[] hlColor) {
        this.plainColor = true;
        setColor(hlColor[0], hlColor[1], hlColor[2], hlColor[3]);
    }

    public void setHlColormap(int cmapIndex,
                              IAttribute cmapAttribute,
                              double cmapMin,
                              double cmapMax) {
        this.plainColor = false;
        this.hlCmapIndex = cmapIndex;
        this.hlCmapAttribute = cmapAttribute;
        this.hlCmapMin = cmapMin;
        this.hlCmapMax = cmapMax;
        highlight(highlighted);
    }

    public void setHlSizeFactor(float hlSizeFactor) {
        this.hlSizeFactor = hlSizeFactor;
        highlight(highlighted);
    }

    public void setHlAllVisible(boolean allVisible) {
        this.hlAllVisible = allVisible;
        highlight(highlighted);
    }

    /**
     * Unloads and removes the catalog described by this catalog info
     */
    public void removeCatalog() {
        if (this.entity != null) {
            EventManager.publish(Event.SCENE_REMOVE_OBJECT_CMD, this, this.entity, true);
            logger.info(I18n.msg("gui.dataset.remove.info", name));

            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.dataset.remove.info", name));

        }
    }

    /**
     * Highlight the dataset using the dataset's own color index
     *
     * @param hl Whether to highlight or not
     */
    public void highlight(boolean hl) {
        this.highlighted = hl;
        if (entity != null) {
            synchronized (view) {
                view.setEntity(entity);
                if (plainColor) {
                    view.highlight(hl, hlColor, hlAllVisible);
                } else {
                    view.highlight(hl, hlCmapIndex, hlCmapAlpha, hlCmapAttribute, hlCmapMin, hlCmapMax, hlAllVisible);
                }
            }

        }
    }

    /**
     * @return True if this is a highlightable catalog, false otherwise.
     */
    public boolean isHighlightable() {
        return isParticleDataset(entity);
    }

    private boolean isParticleDataset(Entity entity) {
        if (entity != null) {
            return Mapper.particleSet.has(entity) || Mapper.starSet.has(entity) || Mapper.octree.has(entity) || Mapper.orbitElementsSet.has(entity);
        }
        return false;
    }

    /**
     * @return True if this catalog's particles have attributes (they are stars), false otherwise.
     */
    public boolean hasParticleAttributes() {
        if (this.entity != null) {
            return Mapper.particleSet.has(entity) || Mapper.starSet.has(entity) || Mapper.octree.has(entity);
        }
        return false;
    }

    public boolean isEntityParticleSet() {
        if (this.entity != null) {
            return Mapper.particleSet.has(entity);
        }
        return false;
    }

    public boolean isEntityStarSet() {
        if (this.entity != null) {
            return Mapper.starSet.has(entity);
        }
        return false;
    }

    public boolean isMission() {
        return objectType != null && objectType.isMission();
    }

    /**
     * The dataset is a mission if it has a child of archetype 'Satellite'.
     *
     * @return Whether the dataset represented by this entity.
     */
    private boolean isEntitySatellite() {
        if (this.entity != null) {
            return EntityUtils.getFirstChildWithComponent(entity, Mapper.parentOrientation) != null;
        }
        return false;
    }


    /**
     * Gets the component type of the model object linked to this catalog.
     *
     * @return The component type.
     */
    public ComponentTypes getCt() {
        if (entity != null) {
            return Mapper.base.get(entity).ct;
        }
        return null;
    }

    public enum DatasetSourceType {
        INTERNAL,
        LOD,
        SAMP,
        SCRIPT,
        UI
    }

    public enum DatasetContentType {
        STAR_SET,
        PARTICLE_SET,
        MISSION,
        OTHER;

        boolean isSet() {
            return this == STAR_SET || this == PARTICLE_SET;
        }

        boolean isMission() {
            return this == MISSION;
        }
    }

}
