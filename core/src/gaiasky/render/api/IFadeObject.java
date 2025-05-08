/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import gaiasky.util.math.Vector2D;

public interface IFadeObject {
    /**
     * Gets the fade in distances.
     *
     * @return The fade in distances in internal units.
     */
    public Vector2D getFadeIn();

    /**
     * Sets the near and far fade in distances.
     *
     * @param nearPc Near fade in distance in parsecs.
     * @param farPc  Far fade in distance in parsecs.
     */
    public void setFadeIn(double nearPc, double farPc);

    /**
     * Gets the fade out distances.
     *
     * @return The fade out distances in internal units.
     */
    public Vector2D getFadeOut();

    /**
     * Sets the near and far fade out distances.
     *
     * @param nearPc Near fade out distance in parsecs.
     * @param farPc  Far fade out distance in parsecs.
     */
    public void setFadeOut(double nearPc, double farPc);
}
