/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.FadeNode;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.octreewrapper.OctreeWrapper;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.filter.Filter;
import gaiasky.util.filter.attrib.IAttribute;

import java.time.Instant;

public class CatalogInfo {
    private static final Log logger = Logger.getLogger(CatalogInfo.class);
    private static int colorIndexSequence = 0;


    public enum CatalogInfoType {
        INTERNAL, LOD, SAMP, SCRIPT, UI
    }

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
    public CatalogInfoType type;

    // Reference to scene graph object
    public FadeNode object;

    public CatalogInfo(String name, String description, String source, CatalogInfoType type, float hlSizeFactor, FadeNode object) {
        super();
        this.name = name;
        this.description = description;
        this.source = source;
        this.type = type;
        this.object = object;
        this.loadDateUTC = Instant.now();
        this.plainColor = true;
        this.hlColor = new float[4];
        this.hlSizeFactor = hlSizeFactor;
        this.hlAllVisible = true;
        System.arraycopy(ColorUtils.getColorFromIndex(colorIndexSequence++), 0, this.hlColor, 0, 4);

        this.object.setCatalogInfo(this);
    }

    public void setVisibility(boolean visibility) {
        if (this.object != null) {
            this.object.setVisibleGroup(visibility);
        }
    }

    public boolean isVisible() {
        return isVisible(false);
    }

    public boolean isVisible(boolean attributeValue) {
        if (this.object != null) {
            return this.object.isVisibleGroup(attributeValue);
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

    public void setHlColor(float[] hlColor) {
        this.plainColor = true;
        setColor(hlColor[0], hlColor[1], hlColor[2], hlColor[3]);
    }

    public float[] getHlColor() {
        return hlColor;
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
        if (this.object != null) {
            if(!isRegular()) {
                EventManager.instance.post(Events.SCENE_GRAPH_REMOVE_OBJECT_CMD, this.object, true);
            }
            this.object.dispose();
            logger.info(I18n.txt("gui.dataset.remove.info",  name));
            EventManager.instance.post(Events.POST_POPUP_NOTIFICATION, I18n.txt("gui.dataset.remove.info",  name));
        }
    }

    /**
     * Highlight the dataset using the dataset's own color index
     *
     * @param hl Whether to highlight or not
     */
    public void highlight(boolean hl) {
        this.highlighted = hl;
        if (plainColor) {
            object.highlight(hl, hlColor, hlAllVisible);
        } else {
            object.highlight(hl, hlCmapIndex, hlCmapAttribute, hlCmapMin, hlCmapMax, hlAllVisible);
        }
    }

    /**
     * @return True if this is a catalog of stars or particles, false otherwise (star clusters)
     */
    public boolean isRegular() {
        return this.object instanceof ParticleGroup || this.object instanceof OctreeWrapper;
    }

}
