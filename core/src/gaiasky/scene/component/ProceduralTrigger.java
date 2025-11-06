/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

/**
 * Component that holds information about the procedural generation in certain objects.
 */
public class ProceduralTrigger implements Component {

    /** Procedural generation flag. If true, a procedural galaxy will be generated for this object. **/
    public boolean proceduralGeneration = false;
    /** The procedurally generated billboard group entity. **/
    public Entity billboardGroup;
}
