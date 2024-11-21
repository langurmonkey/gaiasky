/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.particles.values.PointSpawnShapeValue;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.g3d.particles.IntBillboardParticleBatch;
import gaiasky.util.gdx.g3d.particles.IntParticleController;
import gaiasky.util.gdx.g3d.particles.ParticleIntShader;
import gaiasky.util.gdx.g3d.particles.emitters.RegularIntEmitter;
import gaiasky.util.gdx.g3d.particles.influencers.*;
import gaiasky.util.gdx.g3d.particles.renderers.IntBillboardRenderer;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;

import java.util.List;

/**
 * Renders particle systems using a billboard particle batch.
 */
public class ParticleSystemRenderer extends AbstractRenderSystem {
    private final IntBillboardParticleBatch particleBatch;
    private final IntModelBatch batch;
    private final Environment environment;

    private final Array<IntParticleController> emitters;

    public ParticleSystemRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas) {
        super(sceneRenderer, rg, alphas, null);
        batch = new IntModelBatch();
        emitters = new Array<>();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0f, 0f, 0.1f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, 0, -0.5f, -1));

        var texture = new Texture("data/pre_particle.png");
        particleBatch = new IntBillboardParticleBatch(ParticleIntShader.AlignMode.Screen, true, 100);
        particleBatch.setTexture(texture);

        var tmpVector = new Vector3();

        float d = 1;
        // X
        addEmitter(new float[]{1, 0.12156863f, 0.047058824f}, texture, tmpVector.set(d, d, 0), Vector3.X, 360);

        // Y
        addEmitter(new float[]{0.12156863f, 1, 0.047058824f}, texture, tmpVector.set(0, d, d), Vector3.Y, -360);

        // Z
        addEmitter(new float[]{0.12156863f, 0.047058824f, 1}, texture, tmpVector.set(0, d, d), Vector3.Z, -360);

    }


    private void addEmitter(float[] colors, Texture particleTexture, Vector3 translation, Vector3 actionAxis,
                            float actionRotation) {
        IntParticleController controller = createBillboardController(colors, particleTexture);
        controller.init();
        controller.start();
        emitters.add(controller);
        controller.translate(translation);
    }


    private IntParticleController createBillboardController(float[] colors, Texture particleTexture) {
        // Emission
        RegularIntEmitter emitter = new RegularIntEmitter();
        emitter.getDuration().setLow(3000);
        emitter.getEmission().setHigh(2900);
        emitter.getLife().setHigh(1000);
        emitter.setMaxParticleCount(3000);

        // Spawn
        PointSpawnShapeValue pointSpawnShapeValue = new PointSpawnShapeValue();
        pointSpawnShapeValue.xOffsetValue.setLow(0, 1f);
        pointSpawnShapeValue.xOffsetValue.setActive(true);
        pointSpawnShapeValue.yOffsetValue.setLow(0, 1f);
        pointSpawnShapeValue.yOffsetValue.setActive(true);
        pointSpawnShapeValue.zOffsetValue.setLow(0, 1f);
        pointSpawnShapeValue.zOffsetValue.setActive(true);
        SpawnInfluencer spawnSource = new SpawnInfluencer(pointSpawnShapeValue);

        // Scale
        ScaleInfluencer scaleInfluencer = new ScaleInfluencer();
        scaleInfluencer.value.setTimeline(new float[]{0, 1});
        scaleInfluencer.value.setScaling(new float[]{1, 0});
        scaleInfluencer.value.setLow(0);
        scaleInfluencer.value.setHigh(1);

        // Color
        ColorInfluencer.Single colorInfluencer = new ColorInfluencer.Single();
        colorInfluencer.colorValue.setColors(new float[]{colors[0], colors[1], colors[2], 0, 0, 0});
        colorInfluencer.colorValue.setTimeline(new float[]{0, 1});
        colorInfluencer.alphaValue.setHigh(1);
        colorInfluencer.alphaValue.setTimeline(new float[]{0, 0.5f, 0.8f, 1});
        colorInfluencer.alphaValue.setScaling(new float[]{0, 0.15f, 0.5f, 0});

        // Dynamics
        DynamicsInfluencer dynamicsInfluencer = new DynamicsInfluencer();
        DynamicsModifier.BrownianAcceleration modifier = new DynamicsModifier.BrownianAcceleration();
        modifier.strengthValue.setTimeline(new float[]{0, 1});
        modifier.strengthValue.setScaling(new float[]{0, 1});
        modifier.strengthValue.setHigh(80);
        modifier.strengthValue.setLow(1, 5);
        dynamicsInfluencer.velocities.add(modifier);

        return new IntParticleController("Billboard Controller", emitter, new IntBillboardRenderer(particleBatch),
                new RegionInfluencer.Single(particleTexture), spawnSource, scaleInfluencer, colorInfluencer, dynamicsInfluencer);
    }

    public void render(List<IRenderable> renderables, ICamera camera, double t, RenderingContext rc) {
        renderStud(null, camera, t);
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        var pos = camera.getInversePos().put(new Vector3());

        particleBatch.setCamera(camera.getCamera());

        if (emitters.size > 0) {
            particleBatch.begin();
            for (IntParticleController controller : emitters) {
                controller.setTranslation(pos);
                controller.update();
                controller.draw();
            }
            particleBatch.end();
            batch.render(particleBatch, environment);
        }
    }
}
