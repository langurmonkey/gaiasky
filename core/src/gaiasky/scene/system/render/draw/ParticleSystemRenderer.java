/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;

import java.util.List;

public class ParticleSystemRenderSystem extends AbstractRenderSystem {
    private final ParticleSystem particleSystem;
    private final BillboardParticleBatch batch;
    private final ParticleEffect explosion;
    private final Matrix4 trf;
    private ParticleEffect explosionInstance;

    public ParticleSystemRenderSystem(SceneRenderer sceneRenderer) {
        super(sceneRenderer, null, null, null);
        particleSystem = new ParticleSystem();
        // Since our particle effects are PointSprites, we create a PointSpriteParticleBatch
        batch = new BillboardParticleBatch();
        particleSystem.add(batch);

        ParticleEffectLoader pel = new ParticleEffectLoader(new InternalFileHandleResolver());
        ParticleEffectLoader.ParticleEffectLoadParameter loadParam = new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches());
        String explosionStr = "assets/effects/explosion.pfx";
        pel.getDependencies(explosionStr, Gdx.files.internal(explosionStr), loadParam);
        explosion = pel.loadSync(GaiaSky.instance.assetManager, explosionStr, Gdx.files.internal(explosionStr), loadParam);
        trf = new Matrix4();
    }

    public void render(List<IRenderable> renderables, ICamera camera, double t, RenderingContext rc) {
        renderStud(null, camera, t);
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        batch.setCamera(camera.getCamera());

        if (explosionInstance == null) {
            explosionInstance = explosion.copy();
            explosionInstance.init();
            explosionInstance.start();  // optional: particle will begin playing immediately
            particleSystem.add(explosionInstance);
        }
        Vector3 pos = new Vector3((float) Constants.AU_TO_U, (float) 0, (float) Constants.AU_TO_U);
        pos.sub(camera.getPos().put(new Vector3()));
        trf.idt().translate(pos);
        batch.setCamera(camera.getCamera());
        batch.begin();
        explosionInstance.setTransform(trf);
        particleSystem.update(); // technically not necessary for rendering
        particleSystem.begin();
        particleSystem.draw();
        particleSystem.end();
        batch.end();
    }
}
