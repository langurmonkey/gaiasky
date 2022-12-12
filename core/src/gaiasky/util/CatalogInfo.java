/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.filter.Filter;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.i18n.I18n;

import java.time.Instant;

public class CatalogInfo {
    private static final Log logger = Logger.getLogger(CatalogInfo.class);
    private static int colorIndexSequence = 0;
    private final FocusView view;
    // Base properties
    public String name;
    public String description;
    public String source;
    public long nParticles;
    public long sizeBytes;
    public Instant loadDateUTC;

    // Highlight
    public boolean highlighted;
    public boolean plainColor;
    public float[] hlColor;
    public float hlSizeFactor;
    public boolean hlAllVisible;
    public int hlCmapIndex = 0;
    public IAttribute hlCmapAttribute;
    public double hlCmapMin = 0, hlCmapMax = 0;

    // The filtering object. May be null
    public Filter filter;

    // Catalog type
    public CatalogInfoSource type;

    // Reference to the entity
    public Entity entity;
    public CatalogInfo(String name, String description, String source, CatalogInfoSource type, float hlSizeFactor, Entity entity) {
        this(name, description, source, type, hlSizeFactor);
        setEntity(entity);
    }

    public CatalogInfo(String name, String description, String source, CatalogInfoSource type, float hlSizeFactor) {
        super();
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
    }

    public void setEntity(Entity entity) {
        if (entity != null) {
            this.entity = entity;
            this.view.setEntity(this.entity);
            if (Mapper.datasetDescription.has(entity)) {
                Mapper.datasetDescription.get(entity).setCatalogInfo(this);
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

    public void setColor(float r, float g, float b, float a) {
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

    public void setHlColormap(int cmapIndex, IAttribute cmapAttribute, double cmapMin, double cmapMax) {
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
                    view.highlight(hl, hlCmapIndex, hlCmapAttribute, hlCmapMin, hlCmapMax, hlAllVisible);
                }
            }

        }
    }

    /**
     * @return True if this is a highlightable catalog, false otherwise.
     */
    public boolean isHighlightable() {
        if (this.entity != null) {
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

    public enum CatalogInfoSource {
        INTERNAL,
        LOD,
        SAMP,
        SCRIPT,
        UI
    }

}
