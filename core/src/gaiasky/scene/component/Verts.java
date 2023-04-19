/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderGroup;

public class Verts implements Component {
    public boolean blend = true, depth = true, additive = true;

    public int glPrimitive;

    /** The render group **/
    public RenderGroup renderGroup;

    /** Whether to close the polyline (connect end point to start point) or not **/
    public boolean closedLoop = true;

    // Line width or point size.
    public float primitiveSize = 1f;

    public PointCloudData pointCloudData;

    public void blend() {
        if (blend) {
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            if (additive) {
                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
            } else {
                Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        } else {
            Gdx.gl20.glDisable(GL20.GL_BLEND);
        }
    }

    public void markForUpdate(Render render) {
        EventManager.publish(Event.GPU_DISPOSE_VERTS_OBJECT, render, renderGroup);
    }

}
