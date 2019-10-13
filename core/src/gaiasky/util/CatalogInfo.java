/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util;

import gaia.cu9.ari.gaiaorbit.scenegraph.FadeNode;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.color.ColourUtils;

import java.time.Instant;

public class CatalogInfo {
    private static final Log logger =Logger.getLogger(CatalogInfo.class);
    private static int colorIndexSequence = 0;


    public enum CatalogInfoType {
        INTERNAL, LOD, SAMP, SCRIPT, UI
    }

    public String name;
    public String description;
    public String source;
    public Instant loadDateUTC;
    public float[] color;

    public CatalogInfoType type;

    public FadeNode object;
    public boolean highlighted;

    public CatalogInfo(String name, String description, String source, CatalogInfoType type, FadeNode object) {
        super();
        this.name = name;
        this.description = description;
        this.source = source;
        this.type = type;
        this.object = object;
        this.loadDateUTC = Instant.now();
        this.color = new float[4];
        System.arraycopy(ColourUtils.getColorFromIndex(colorIndexSequence++), 0, this.color, 0, 4);

        this.object.setCatalogInfo(this);
    }

    public void setVisibility(boolean visibility) {
        if (this.object != null) {
            this.object.setVisible(visibility);
        }
    }

    public boolean isVisible(){
        if(this.object != null){
            return this.object.isVisible();
        }
        return true;
    }

    public void setColor(float r, float g, float b, float a){
        this.color[0] = r;
        this.color[1] = g;
        this.color[2] = b;
        this.color[3] = a;
        highlight(highlighted);
    }

    public void setColor(float[] color){
        setColor(color[0], color[1], color[2], color[3]);
    }

    public float[] getColor(){
        return color;
    }

    /**
     * Unloads and removes the catalog described by this catalog info
     */
    public void removeCatalog() {
        if (this.object != null) {
            logger.info("Removing dataset " + name);
            this.object.dispose();
        }
    }

    /**
     * Highlight the dataset using the dataset's own color index
     * @param hl Whether to highlight or not
     */
    public void highlight(boolean hl){
        this.highlighted = hl;
        object.highlight(hl, color);
    }

    /**
     * Highlight the dataset using a specific color index
     * @param hl Whether to highlight or not
     * @param color The color
     */
    public void highlight(boolean hl, float[] color){
        this.highlighted = hl;
        object.highlight(hl, color);
    }

}
