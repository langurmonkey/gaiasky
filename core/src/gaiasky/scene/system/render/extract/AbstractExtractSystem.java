/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.GaiaSky;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Base;
import gaiasky.scene.view.LabelView;

import java.util.List;

public abstract class AbstractExtractSystem extends IteratingSystem {

    protected final ICamera camera;
    protected ISceneRenderer renderer;
    protected LabelView view;
    protected List<List<IRenderable>> renderLists;

    public AbstractExtractSystem(Family family, int priority) {
        super(family, priority);
        this.camera = GaiaSky.instance.cameraManager;
        this.view = new LabelView();
    }

    public void extract(Entity entity) {
        processEntity(entity, 0f);
    }

    public void setRenderer(ISceneRenderer renderer) {
        this.renderer = renderer;
        this.renderLists = renderer.getRenderLists();
    }

    /**
     * Computes whether the entity with the given base component must be rendered.
     * Entities must be rendered when their opacity is non-zero, they are visible,
     * they are not a copy, and all of their component types are active.
     *
     * @param base The base component of the entity.
     *
     * @return Whether the entity must be rendered.
     */
    protected boolean mustRender(Base base) {
        return base.opacity > 0 && !base.copy && renderer.allOn(base.ct) && base.isVisible();
    }

    /**
     * Adds the given renderable to the given render group list.
     *
     * @param renderable The renderable to add.
     * @param rg         The render group that identifies the renderable list.
     *
     * @return True if added, false otherwise.
     */
    protected boolean addToRender(IRenderable renderable, RenderGroup rg) {
        try {
            return renderLists.get(rg.ordinal()).add(renderable);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes the given renderable from the given render group list.
     *
     * @param renderable The renderable to remove.
     * @param rg         The render group to remove from.
     *
     * @return True if removed, false otherwise.
     */
    protected boolean removeFromRender(IRenderable renderable, RenderGroup rg) {
        return renderLists.get(rg.ordinal()).remove(renderable);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup rg) {
        return renderLists.get(rg.ordinal()).contains(renderable);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup... rgs) {
        boolean is = false;
        for (RenderGroup rg : rgs)
            is = is || renderLists.get(rg.ordinal()).contains(renderable);
        return is;
    }
}
