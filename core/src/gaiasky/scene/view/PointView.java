/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.api.IPointRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Verts;
import gaiasky.scene.entity.EntityUtils;

public class PointView extends RenderView implements IPointRenderable {

    /** The verts component . **/
    private Verts verts;

    /** Creates an empty line view. **/
    public PointView() {
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.verts, Verts.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.verts = Mapper.verts.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.verts = null;
    }

    @Override
    public void blend() {
        EntityUtils.blend(verts);
    }

    @Override
    public void depth() {
        EntityUtils.depth(verts);
    }
}
