/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class LocationMark implements Component {
    // Locations with a solid angle greater than this are shown.
    public static final float LOWER_LIMIT = 5e-4f;
    // Locations with a solid angle lower than this are shown.
    public static final float UPPER_LIMIT = 4.5e-3f;

    /**
     * The display name.
     **/
    public String displayName;

    /**
     * Description, which shows up in a tooltip when the mouse cursor hovers over the location marker.
     */
    public String tooltipText;

    /**
     * A link (URL) to an external resource, displayed in the tooltip if present.
     */
    public String link;

    /**
     * Additional categorization of locations. This is used only in the UI so that all locations in the same category can be turned on and off at
     * the same time with a single click.
     */
    public String locationType = null;

    /**
     * Location marker texture. Set to 'none' to disable maker.
     * Possible values are 'none', 'default', 'flag', 'city', 'town', 'landmark', or a path to a PNG image.
     */
    public String locationMarkerTexture = "default";

    /**
     * Longitude and latitude
     **/
    public Vector2 location;
    /**
     * Location in the reference system of the parent.
     */
    public Vector3 location3d;
    /**
     * This controls the distance from the center in case of non-spherical
     * objects.
     **/
    public float distFactor = 1f;

    // Size in Km
    public float sizeKm;

    /**
     * Ignore the {@link LocationMark#UPPER_LIMIT} when determining the visibility of this location's label.
     * Effectively, setting this to true causes the location to not disappear regardless of the camera's distance.
     */
    public boolean ignoreSolidAngleLimit = false;

    public void setLocation(double[] pos) {
        this.location = new Vector2((float) pos[0], (float) pos[1]);
    }

    public void setDistFactor(Double distFactor) {
        this.distFactor = distFactor.floatValue();
    }

    public void setLocationType(String type) {
        this.locationType = type.strip();
    }

    public void setLocationMarkerTexture(String tex) {
        this.locationMarkerTexture = tex;
    }

    public void setMarkerTexture(String tex) {
        this.setLocationMarkerTexture(tex);
    }

    public void setIgnoreSolidAngleLimit(boolean ignoreSolidAngleLimit) {
        this.ignoreSolidAngleLimit = ignoreSolidAngleLimit;
    }

    public void setTooltipText(String tooltip) {
        this.tooltipText = tooltip;
    }
}
