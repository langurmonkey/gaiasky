/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.sprite.SpriteEntityRenderSystem;

import java.util.List;

public class SpriteRenderer extends AbstractRenderSystem {

    private final SpriteEntityRenderSystem renderer;
    private final SpriteBatch batch;

    public SpriteRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ShaderProgram program) {
        super(sceneRenderer, rg, alphas, null);

        batch = new SpriteBatch(50, program);
        renderer = new SpriteEntityRenderSystem();
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        batch.begin();

        for (var r : renderables) {
            renderer.render(((Render) r).getEntity(), batch, camera);
        }

        batch.end();
    }


}