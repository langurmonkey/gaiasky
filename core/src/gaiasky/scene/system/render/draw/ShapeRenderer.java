/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.shape.ShapeEntityRenderSystem;

import java.util.List;

public class ShapeRenderer extends AbstractRenderSystem {

    private final ShapeEntityRenderSystem renderSystem;
    /**
     * The shape renderer
     */
    private final com.badlogic.gdx.graphics.glutils.ShapeRenderer shapeRenderer;

    public ShapeRenderer(SceneRenderer sceneRenderer, final RenderGroup rg, final float[] alphas, final ShaderProgram spriteShader) {
        super(sceneRenderer, rg, alphas, null);
        this.shapeRenderer = new com.badlogic.gdx.graphics.glutils.ShapeRenderer(5000, spriteShader);
        this.renderSystem = new ShapeEntityRenderSystem();

    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        shapeRenderer.begin(ShapeType.Line);
        renderables.forEach(r -> {
            Render render = (Render) r;
            renderSystem.setEntity(render.entity);
            renderSystem.render(shapeRenderer, rc, getAlpha(r), camera);
        });
        shapeRenderer.end();
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        updateBatchSize(w, h);
    }

    @Override
    public void updateBatchSize(int w, int h) {
        shapeRenderer.setProjectionMatrix(shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, w, h));
    }

}
