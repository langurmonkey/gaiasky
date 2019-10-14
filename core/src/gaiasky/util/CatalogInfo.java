/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.scenegraph.FadeNode;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColourUtils;

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
    public float[] hlColor;
    public float hlSizeFactor;
    public boolean hlAllVisible;

    public CatalogInfoType type;

    public FadeNode object;
    public boolean highlighted;

    public CatalogInfo(String name, String description, String source, CatalogInfoType type, float hlSizeFactor, FadeNode object) {
        super();
        this.name = name;
        this.description = description;
        this.source = source;
        this.type = type;
        this.object = object;
        this.loadDateUTC = Instant.now();
        this.hlColor = new float[4];
        this.hlSizeFactor = hlSizeFactor;
        this.hlAllVisible = true;
        System.arraycopy(ColourUtils.getColorFromIndex(colorIndexSequence++), 0, this.hlColor, 0, 4);

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
        this.hlColor[0] = r;
        this.hlColor[1] = g;
        this.hlColor[2] = b;
        this.hlColor[3] = a;
        highlight(highlighted);
    }

    public void setHlColor(float[] hlColor){
        setColor(hlColor[0], hlColor[1], hlColor[2], hlColor[3]);
    }

    public float[] getHlColor(){
        return hlColor;
    }

    public void setHlSizeFactor(float hlSizeFactor){
        this.hlSizeFactor = hlSizeFactor;
        highlight(highlighted);
    }

    public void setHlAllVisible(boolean allVisible){
        this.hlAllVisible = allVisible;
        highlight(highlighted);
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
        object.highlight(hl, hlColor);
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
