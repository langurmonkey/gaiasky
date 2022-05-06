/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import gaiasky.render.ComponentTypes;

/**
 * A top-level renderable interface that all renderable objects must extend
 */
public interface IRenderable {

    /**
     * Gets the component types of this entity
     *
     * @return The component types
     */
    ComponentTypes getComponentType();

    /**
     * Gets the last distance to the camera calculated for this entity
     *
     * @return The distance
     */
    double getDistToCamera();

    /**
     * Returns the opacity of this renderable
     *
     * @return The opacity
     */
    float getOpacity();

}
