/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class Flags implements Component {

    /**
     * Flag indicating whether the object has been computed in this step.
     */
    public boolean computed = true;
    /**
     * Is this just a copy?
     */
    public boolean copy = false;
    /**
     * Has this been updated at least once?
     */
    public boolean initialUpdate = false;
    /**
     * Is this node visible?
     */
    protected boolean visible = true;
    /**
     * Force to render the label of this entity,
     * bypassing the solid angle check
     */
    protected boolean forceLabel = false;

}