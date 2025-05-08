/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Consumers.Consumer6;
import gaiasky.util.Consumers.Consumer9;
import gaiasky.util.Functions.Function3;
import gaiasky.util.math.Vector3D;

public class Focus implements Component {

    /**
     * Whether this entity is focusable or not. If it is not focusable, the entity won't appear in the search results and
     * can't be selected with the mouse.
     **/
    public boolean focusable = true;

    /** Consumer that returns whether the focus is active or not. **/
    public Function3<FocusActive, Entity, Base, Boolean> activeFunction;

    /** Consumer that computes whether the focus is hit by a ray. **/
    public Consumer6<FocusHit, FocusView, Vector3D, Vector3D, NaturalCamera, Array<Entity>> hitRayConsumer;

    /** Consumer that computes whether the focus is hit by the given screen coordinates. **/
    public Consumer9<FocusHit, FocusView, Integer, Integer, Integer, Integer, Integer, NaturalCamera, Array<Entity>> hitCoordinatesConsumer;

    public void setFocusable(Boolean focusable) {
        this.focusable = focusable;
    }
}
