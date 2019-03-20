package gaia.cu9.ari.gaiaorbit.util;

import gaia.cu9.ari.gaiaorbit.scenegraph.FadeNode;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

public class CatalogInfo {
    private static final Log logger =Logger.getLogger(CatalogInfo.class);

    public enum CatalogInfoType {
        INTERNAL, LOD, SAMP, SCRIPT, UI
    }

    public String name;
    public String description;
    public String source;
    public CatalogInfoType type;

    public FadeNode object;

    public CatalogInfo(String name, String description, String source, CatalogInfoType type, FadeNode object) {
        super();

        this.name = name;
        this.description = description;
        this.source = source;
        this.type = type;
        this.object = object;
        this.object.setCatalogInfo(this);
    }

    public void setVisibility(boolean visibility) {
        if (this.object != null) {
            this.object.setVisible(visibility);
        }
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
}
