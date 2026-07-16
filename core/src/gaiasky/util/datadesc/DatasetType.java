/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the dataset type as a string, and contains a list of datasets for this type. This does
 * not act as an enum, as there may be multiple instances of the same dataset type and different
 * datasets.
 */
public class DatasetType {
    public String typeStr;
    public List<Dataset> datasets;

    /** Type-to-icon-key map. Icon keys are drawable identifiers in the {@link com.badlogic.gdx.scenes.scene2d.ui.Skin}. **/
    private static final Map<String, String> iconMap;

    static {
        iconMap = new HashMap<>();
        iconMap.put("other", "icon-database");
        iconMap.put("data-pack", "icon-elem-others");
        iconMap.put("catalog-lod", "icon-elem-stars");
        iconMap.put("catalog-gaia", "icon-elem-stars");
        iconMap.put("catalog-star", "icon-elem-stars");
        iconMap.put("catalog-gal", "icon-elem-galaxies");
        iconMap.put("catalog-cluster", "icon-elem-clusters");
        iconMap.put("catalog-sso", "icon-elem-asteroids");
        iconMap.put("catalog-other", "icon-elem-others");
        iconMap.put("mesh", "icon-elem-meshes");
        iconMap.put("spacecraft", "icon-elem-satellites");
        iconMap.put("system", "iconic-target");
        iconMap.put("texture-pack", "icon-texture");
        iconMap.put("virtualtex-pack", "iconic-image");
        iconMap.put("volume", "icon-elem-nebulae");
    }

    /** Gets a weight for each type string. The weight is used for sorting the types. **/
    public static int getTypeWeight(String type) {
        return switch (type) {
            case "data-pack" -> -2;
            case "texture-pack" -> -1;
            case "catalog-lod" -> 0;
            case "catalog-gaia" -> 1;
            case "catalog-star" -> 2;
            case "catalog-gal" -> 3;
            case "catalog-cluster" -> 4;
            case "catalog-sso" -> 5;
            case "catalog-other" -> 6;
            case "system" -> 7;
            case "spacecraft" -> 8;
            case "mesh" -> 9;
            case "volume" -> 10;
            case "virtualtex-pack" -> 11;
            case "other" -> 12;
            default -> 13;
        };
    }


    /**
     * @param typeString The dataset type as a string.
     *
     * @return The icon identifier for this type.
     */
    public static String getTypeIcon(String typeString) {
        if (typeString != null && iconMap.containsKey(typeString))
            return iconMap.get(typeString);
        return "icon-elem-others";
    }

    /**
     * Creates a type with an empty datasets list from a string.
     *
     * @param typeStr The type string.
     */
    public DatasetType(String typeStr) {
        this.typeStr = typeStr;
        this.datasets = new ArrayList<>();
    }

    /**
     * Adds a dataset to this type.
     *
     * @param dd The dataset metadata object.
     */
    public void addDataset(Dataset dd) {
        this.datasets.add(dd);
    }

    /**
     * Returns the icon identifier for this type.
     *
     * @return The icon identifier.
     */
    public String getIcon() {
        return getTypeIcon(typeStr);
    }
}
